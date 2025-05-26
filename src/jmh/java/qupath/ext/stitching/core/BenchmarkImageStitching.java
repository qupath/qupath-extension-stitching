package qupath.ext.stitching.core;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import qupath.ext.stitching.Utils;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.IntStream;

/**
 * A benchmark to determine the optimal number of threads for stitching images. It will create sample TIFF
 * images, and then stitch them with different number of threads.
 * <p>
 * It can be run with "./gradlew jmh". Results will be printed on the console and saved to build/results/jmh/results.txt.
 */
@BenchmarkMode(Mode.AverageTime)
@State(Scope.Benchmark)
@Warmup(iterations = 1)
@Fork(2)
public class BenchmarkImageStitching {

    private static final int NUMBER_OF_IMAGES = 50;
    private static final int WIDTH_OF_IMAGES = 1800;    // the width and height correspond to the size of images coming from a dataset
    private static final int HEIGHT_OF_IMAGES = 1400;   // used during the development of this extension
    private Path imagesDirectory;
    private List<String> imagePaths;
    @Param({"0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9", "1.", "1.33", "1.66", "2.", "2.33", "2.66", "3."})
    public float fractionOfAvailableCores;

    @Setup(Level.Trial)
    public void Create_Input_Images() throws IOException {
        imagesDirectory = Files.createTempDirectory(null);
        imagePaths = IntStream.range(0, NUMBER_OF_IMAGES)
                .mapToObj(i -> {
                    try {
                        String path = imagesDirectory.resolve(String.format("%d.tiff", i)).toString();
                        ImageUtils.writeTiff(
                                path,
                                ImageUtils.createSampleImage(WIDTH_OF_IMAGES, HEIGHT_OF_IMAGES, Color.WHITE),
                                1,
                                1,
                                0,
                                i*HEIGHT_OF_IMAGES
                        );
                        return path;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .toList();
    }

    @Benchmark
    public void Benchmark_Number_Of_Threads_Of_Image_Stitcher_Creation() throws IOException, InterruptedException {
        String outputPath = Files.createTempDirectory(imagesDirectory, null).resolve("image.ome.zarr").toString();

        new ImageStitcher.Builder(imagePaths)
                .setNumberOfThreads(Math.max(
                        (int) (Runtime.getRuntime().availableProcessors() * fractionOfAvailableCores),
                        1
                ))
                .build()
                .writeToZarrFile(outputPath, null);
    }

    @TearDown(Level.Trial)
    public void Delete_Images() throws IOException {
        Utils.deleteDirectoryRecursively(imagesDirectory.toFile());
    }
}
