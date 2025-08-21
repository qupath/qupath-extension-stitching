package qupath.ext.stitching.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.stitching.core.positionfinders.FilenamePatternPositionFinder;
import qupath.ext.stitching.core.positionfinders.PositionFinder;
import qupath.ext.stitching.core.positionfinders.TiffTagPositionFinder;
import qupath.lib.common.ThreadTools;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerProvider;
import qupath.lib.images.servers.ImageServers;
import qupath.lib.images.servers.SparseImageServer;
import qupath.lib.images.writers.ome.OMEPyramidWriter;
import qupath.lib.images.writers.ome.zarr.OMEZarrWriter;
import qupath.lib.regions.ImageRegion;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

/**
 * A class to stitch TIFF images.
 * <p>
 * Use a {@link Builder} to create an instance of this class.
 */
public class ImageStitcher {

    private static final Logger logger = LoggerFactory.getLogger(ImageStitcher.class);
    private final int numberOfThreads;
    private final ImageServer<BufferedImage> server;
    private final AtomicBoolean someInputImagesNotUsed = new AtomicBoolean(false);

    private ImageStitcher(Builder builder) throws InterruptedException, IOException {
        logger.debug("Creating image stitcher for {}", builder.imagePaths);

        this.numberOfThreads = builder.numberOfThreads;

        SparseImageServer.Builder sparserServerBuilder = new SparseImageServer.Builder();

        ExecutorService executorService = Executors.newFixedThreadPool(
                numberOfThreads,
                ThreadTools.createThreadFactory("stitcher-", false)
        );
        AtomicBoolean atLeastOneImageAdded = new AtomicBoolean(false);

        AtomicInteger counter = new AtomicInteger(0);
        for (String imagePath: builder.imagePaths) {
            executorService.execute(() -> {
                try {
                    logger.debug("Checking if {} is a TIFF file", imagePath);
                    TiffFileChecker.checkTiffFile(imagePath);

                    logger.debug("Parsing {}...", imagePath);
                    ImageServerBuilder.UriImageSupport<BufferedImage> imageSupport = ImageServerProvider.getPreferredUriImageSupport(BufferedImage.class, imagePath);
                    if (imageSupport == null || imageSupport.getBuilders().isEmpty()) {
                        logger.warn("Cannot read image located at {}", imagePath);
                        return;
                    }
                    ImageServerBuilder.ServerBuilder<BufferedImage> serverBuilder = imageSupport.getBuilders().getFirst();
                    logger.debug("Got server builder {} for {}", serverBuilder, imagePath);

                    ImageServer<BufferedImage> server = serverBuilder.build();
                    logger.debug("Got server {} for {}", server, imagePath);

                    int[] position = null;
                    for (int i=0; i<builder.positionFinders.size(); i++) {
                        try {
                            position = builder.positionFinders.get(i).findPosition(server);
                        } catch (IOException | RuntimeException e) {
                            if (i < builder.positionFinders.size() - 1) {
                                logger.debug("Cannot use {} to retrieve position. Trying following one", builder.positionFinders.get(i), e);
                            } else {
                                throw e;
                            }
                        }
                    }
                    if (position == null) {
                        throw new IllegalStateException("No position finder was able to work");
                    }
                    int[] finalPosition = position;

                    List<ImageRegion> regions = IntStream.range(0, server.getMetadata().getSizeZ())
                            .boxed()
                            .flatMap(z -> IntStream.range(0, server.getMetadata().getSizeT())
                                    .mapToObj(t -> ImageRegion.createInstance(
                                            finalPosition[0],
                                            finalPosition[1],
                                            server.getMetadata().getWidth(),
                                            server.getMetadata().getHeight(),
                                            z,
                                            t
                                    ))
                            ).toList();
                    logger.debug("Got regions {} for {}", regions, imagePath);

                    synchronized (this) {
                        for (ImageRegion region: regions) {
                            sparserServerBuilder.serverRegion(
                                    region,
                                    1.0,
                                    server
                            );
                        }
                    }
                    atLeastOneImageAdded.set(true);
                } catch (Exception e) {
                    logger.warn("Cannot read TIFF image located at {}", imagePath, e);
                    someInputImagesNotUsed.set(true);
                }

                if (builder.onProgress != null) {
                    builder.onProgress.accept((float) counter.incrementAndGet() / builder.imagePaths.size());
                }
            });
        }
        executorService.close();

        try {
            executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            logger.debug("Waiting interrupted. Stopping tasks", e);
            executorService.shutdownNow();
            throw e;
        }

        if (!atLeastOneImageAdded.get()) {
            throw new IllegalArgumentException(String.format("No images from %s were successfully parsed", builder.imagePaths));
        }

        if (builder.pyramidalize) {
            this.server = ImageServers.pyramidalize(sparserServerBuilder.build());
        } else {
            this.server = sparserServerBuilder.build();
        }
        logger.debug("Created {} for {}", server, builder.imagePaths);
    }

    /**
     * @return an image server representing the resulting image
     */
    public ImageServer<BufferedImage> getServer() {
        return server;
    }

    /**
     * Write the resulting image to the specified path with the Zarr format. This may take some time depending on
     * the number of input images.
     *
     * @param outputPath the path the output image should have
     * @param onProgress a function that will be called at different steps when the writing occurs. Its parameter will be a float
     *                   between 0 and 1 indicating the progress of the operation (0: beginning, 1: finished). This function may
     *                   be called from any thread. Can be null
     * @throws IOException if the empty image cannot be created. This can happen if the provided
     * path is incorrect or if the user doesn't have enough permissions
     * @throws IllegalArgumentException if the provided path doesn't end with ".ome.zarr" or if a file/directory already exists
     * at this location
     * @throws InterruptedException if the writing is interrupted
     */
    public void writeToZarrFile(String outputPath, Consumer<Float> onProgress) throws IOException, InterruptedException {
        logger.debug("Attempting to write {} to {}", server, outputPath);
        AtomicReference<Float> progress = new AtomicReference<>(0f);
        Map<Integer, Float> levelProgress = new ConcurrentHashMap<>();      // Lower resolution tiles take more time to read, so we assume that a
                                                                            // processed lower resolution tile provides more progress than a processed
                                                                            // higher resolution tile
        try (var writer = new OMEZarrWriter.Builder(server)
                .parallelize(numberOfThreads)
                .onTileWritten(onProgress == null ?
                        null :
                        tileRequest -> onProgress.accept(progress.updateAndGet(p -> p + levelProgress.get(tileRequest.getLevel())))
                )
                .build(outputPath)
        ) {
            for (int level=0; level<writer.getReaderServer().getMetadata().nLevels(); level++) {
                levelProgress.put(
                        level,
                        1f / (writer.getReaderServer().getMetadata().nLevels() * writer.getReaderServer().getTileRequestManager().getTileRequestsForLevel(level).size())
                );
            }
            logger.debug("{} tiles to write to {}", writer.getReaderServer().getTileRequestManager().getAllTileRequests().size(), outputPath);

            writer.writeImage();
        }
    }

    /**
     * Write the resulting image to the specified path with the OME-TIFF format. This may take some time depending on
     * the number of input images.
     *
     * @param outputPath the path the output image should have
     * @throws RuntimeException if an error occurs while writing the image
     */
    public void writeToTiffFile(String outputPath) throws Exception {
        new OMEPyramidWriter.Builder(server)
                .tileSize(512)
                .channelsInterleaved()          // because SparseImageServer returns all channels in a BufferedImage, it's more efficient to write them interleaved
                .parallelize(numberOfThreads)
                .losslessCompression()
                .build()
                .writeSeries(outputPath);
    }

    /**
     * @return whether it was not possible to use at least one image from the image paths given
     * to {@link Builder#Builder(List)}
     */
    public boolean areSomeInputImagesNotUsed() {
        return someInputImagesNotUsed.get();
    }

    /**
     * A builder to create a {@link ImageStitcher}.
     */
    public static class Builder {

        private final List<String> imagePaths;
        private List<PositionFinder> positionFinders = List.of(
                new FilenamePatternPositionFinder(FilenamePatternPositionFinder.StandardPattern.VECTRA),
                new TiffTagPositionFinder()
        );
        private int numberOfThreads = Runtime.getRuntime().availableProcessors();   // this was determined by running the BenchmarkImageStitching
                                                                                    // benchmark on several machines and taking a good score that
                                                                                    // doesn't require a lot of RAM
        private boolean pyramidalize = true;
        private Consumer<Float> onProgress = null;

        /**
         * Create the builder.
         *
         * @param imagePaths paths of the TIFF files that should be combined
         * @throws NullPointerException if the provided parameter is null
         */
        public Builder(List<String> imagePaths) {
            this.imagePaths = Objects.requireNonNull(imagePaths);
        }

        /**
         * Set the strategies to retrieve tile positions. For each tile, the first position finder that doesn't throw an exception
         * is used (following the order of the provided list).
         * <p>
         * Take a look at the {@link qupath.ext.stitching.core.positionfinders} package for existing implementations.
         * {@link FilenamePatternPositionFinder} with {@link FilenamePatternPositionFinder.StandardPattern#VECTRA} and
         * {@link TiffTagPositionFinder} by default.
         *
         * @param positionFinders a list of strategies to retrieve tile positions
         * @return this builder
         * @throws NullPointerException if the provided parameter is null
         * @throws IllegalArgumentException if the provided list is empty
         */
        public Builder positionFinders(List<PositionFinder> positionFinders) {
            if (positionFinders.isEmpty()) {
                throw new IllegalArgumentException("The provided list of position finders is empty");
            }

            this.positionFinders = Objects.requireNonNull(positionFinders);
            return this;
        }

        /**
         * Set the number of threads to use when parsing the input images or writing the output image.
         *
         * @param numberOfThreads the number of threads to use. By default, this is equal to {@link Runtime#availableProcessors()}
         * @return this builder
         */
        public Builder numberOfThreads(int numberOfThreads) {
            this.numberOfThreads = numberOfThreads;
            return this;
        }

        /**
         * Whether the downsamples of the output image should be 1, 4, 8, 16, and so on. If no, the output image
         * will only have a single resolution. True by default.
         *
         * @param pyramidalize whether to pyramidalize the output image
         * @return this builder
         */
        public Builder pyramidalize(boolean pyramidalize) {
            this.pyramidalize = pyramidalize;
            return this;
        }

        /**
         * Set a function that will be called at different steps when {@link #build()} is called.
         * <p>
         * Its parameter will be a float between 0 and 1 indicating the progress of the operation (0: beginning,
         * 1: finished).
         * <p>
         * This function may be called from any thread.
         *
         * @param onProgress a function that will be called at different steps when {@link #build()} is called. Can be null
         * @return this builder
         */
        public Builder onProgress(Consumer<Float> onProgress) {
            this.onProgress = onProgress;
            return this;
        }

        /**
         * Create a {@link ImageStitcher}.
         * <p>
         * This will parse every file given to {@link #Builder(List)}, so it might take some time depending on
         * the number of elements. If a file is incorrect (e.g. it's not a TIFF file), it will be skipped and a
         * warning message will be logged.
         *
         * @return this builder
         * @throws IOException if an issue occurs while creating the output image
         * @throws InterruptedException if this operation in interrupted
         * @throws IllegalArgumentException if no image was given to {@link #Builder(List)}, or if it wasn't possible to
         * retrieve any position from the list
         */
        public ImageStitcher build() throws IOException, InterruptedException {
            return new ImageStitcher(this);
        }
    }
}
