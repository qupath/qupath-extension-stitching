package qupath.ext.stitching.core;

import qupath.lib.images.servers.PixelCalibration;
import qupath.lib.regions.ImageRegion;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

/**
 * A class that can create an {@link ImageRegion} by looking at specific tags of a TIFF file.
 */
class TiffRegionParser {

    private static final Pattern POSITION_IN_NAME_PATTERN = Pattern.compile("\\[([\\d.]+),([\\d.]+)]");
    private TiffRegionParser() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Create a list of regions based on the "ImageWidth" and "ImageLength" tags of the provided TIFF file and:
     * <ul>
     *     <li>
     *         The name of the image if the provided image source is {@link ImageStitcher.ImageSource#VECTRA_2}
     *         (a [xPosition; yPosition] string is expected from the file name, where positions are expressed in
     *         micrometer).
     *     </li>
     *     <li>
     *         The "XPosition", "YPosition", "XResolution", and "YResolution" tags if the provided image source is
     *         {@link ImageStitcher.ImageSource#VECTRA_3}.
     *     </li>
     * </ul>
     * One region is created for each z-stack and timepoint.
     *
     * @param path a path to a TIFF file containing the tags used to create the region
     * @param imageSource indicate how input images were generated.
     * @param sizeZ the number of z-stacks of the TIFF image
     * @param sizeT the number of timepoints of the TIFF image
     * @param pixelCalibration the pixel calibration of the TIFF image
     * @return the region created from the above tags
     * @throws NullPointerException if the provided path is null
     * @throws IOException if an I/O error occurs while reading the provided file
     * @throws SecurityException if the user doesn't have sufficient rights to read the provided file
     * @throws IllegalArgumentException if the provided file is not a TIFF file, doesn't contain metadata,
     * or doesn't contain enough information to determine the region
     * @throws NumberFormatException if the position in the name of the image cannot be converted to a float
     * @throws IllegalStateException if no ImageIO TIFF reader was found
     * @throws IIOInvalidTreeException if the image metadata cannot be parsed
     */
    public static List<ImageRegion> parseRegion(String path, ImageStitcher.ImageSource imageSource, int sizeZ, int sizeT, PixelCalibration pixelCalibration) throws IOException {
        checkTiffFile(path);

        return parseRegionFromTIFF(path, imageSource, sizeZ, sizeT, pixelCalibration);
    }

    private static void checkTiffFile(String path) throws IOException {
        byte[] bytes;
        try (FileInputStream inputStream = new FileInputStream(path)) {
            bytes = inputStream.readNBytes(4);
        }

        if (bytes.length < 4) {
            throw new IllegalArgumentException(String.format("%s contains less than 4 bytes, so it cannot be a TIFF file", path));
        }

        if (!((bytes[0] == 0x49 && bytes[1] == 0x49 && bytes[2] == 0x2A && bytes[3] == 0x00) ||
                (bytes[0] == 0x4D && bytes[1] == 0x4D && bytes[2] == 0x00 && bytes[3] == 0x2A))) {
            throw new IllegalArgumentException(String.format(
                    "The first four bytes of %s (%s) don't match the TIFF specifications",
                    path,
                    Arrays.toString(bytes)
            ));
        }
    }

    private static List<ImageRegion> parseRegionFromTIFF(String path, ImageStitcher.ImageSource imageSource, int sizeZ, int sizeT, PixelCalibration pixelCalibration) throws IOException {
        try (ImageInputStream inputStream = ImageIO.createImageInputStream(new File(path))) {
            Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("TIFF");

            if (!readers.hasNext()) {
                throw new IllegalStateException("No ImageIO TIFF reader found");
            }
            ImageReader reader = readers.next();
            reader.setInput(inputStream);

            IIOMetadata metadata = reader.getImageMetadata(reader.getMinIndex());
            if (metadata == null) {
                throw new IllegalArgumentException(String.format("No metadata found in %s", path));
            }
            TIFFDirectory tiffDirectory = TIFFDirectory.createFromMetadata(metadata);

            int[] xy = switch (imageSource) {
                case VECTRA_2 -> {
                    Matcher matcher = POSITION_IN_NAME_PATTERN.matcher(path);

                    if (matcher.find() && matcher.groupCount() > 1) {
                        float x = Float.parseFloat(matcher.group(1));
                        float y = Float.parseFloat(matcher.group(2));

                        yield new int[] {
                                (int) Math.round(Double.isNaN(pixelCalibration.getPixelWidthMicrons()) ? x : x / pixelCalibration.getPixelWidthMicrons()),
                                (int) Math.round(Double.isNaN(pixelCalibration.getPixelHeightMicrons()) ? y : y / pixelCalibration.getPixelHeightMicrons())
                        };
                    } else {
                        throw new IllegalArgumentException(String.format("No X or Y position found in %s", path));
                    }
                }
                case VECTRA_3 -> new int[] {
                        (int) Math.round(getTag(path, tiffDirectory, BaselineTIFFTagSet.TAG_X_RESOLUTION).getAsDouble(0) *
                                getTag(path, tiffDirectory, BaselineTIFFTagSet.TAG_X_POSITION).getAsDouble(0)
                        ),
                        (int) Math.round(getTag(path, tiffDirectory, BaselineTIFFTagSet.TAG_Y_RESOLUTION).getAsDouble(0) *
                                getTag(path, tiffDirectory, BaselineTIFFTagSet.TAG_Y_POSITION).getAsDouble(0)
                        )
                };
            };

            int width = getTag(path, tiffDirectory, BaselineTIFFTagSet.TAG_IMAGE_WIDTH).getAsInt(0);
            int height = getTag(path, tiffDirectory, BaselineTIFFTagSet.TAG_IMAGE_LENGTH).getAsInt(0);

            return IntStream.range(0, sizeZ)
                    .boxed()
                    .flatMap(z -> IntStream.range(0, sizeT)
                            .mapToObj(t -> ImageRegion.createInstance(xy[0], xy[1], width, height, z, t))
                    ).toList();
        }
    }

    private static TIFFField getTag(String path, TIFFDirectory tiffDirectory, int tagNumber) {
        TIFFField tag = tiffDirectory.getTIFFField(tagNumber);

        if (tag == null) {
            throw new IllegalArgumentException(String.format("The provided file %s does not contain the %d tag", path, tagNumber));
        } else {
            return tag;
        }
    }
}
