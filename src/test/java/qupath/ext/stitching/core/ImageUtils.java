package qupath.ext.stitching.core;

import org.junit.jupiter.api.Assertions;
import org.opentest4j.AssertionFailedError;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.plugins.tiff.TIFFTag;
import javax.imageio.stream.ImageOutputStream;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Collection;

/**
 * A collection of utility methods for creating (TIFF) images.
 */
public class ImageUtils {

    private ImageUtils() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * Create a RGB image.
     *
     * @param width the width the image should have
     * @param height the height the image should have
     * @param color the color the image should have
     * @return an image with the specified size and the specified color
     */
    public static BufferedImage createSampleImage(int width, int height, Color color) {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics2D graphics = image.createGraphics();
        graphics.setColor(color);
        graphics.fillRect(0, 0, width, height);

        graphics.dispose();

        return image;
    }

    /**
     * Write a TIFF file to the provided path that contain the "XResolution", "YResolution", "XPosition", and "YPosition" tags
     * with the provided values.
     *
     * @param path the path the image should have
     * @param image the image to write
     * @param xResolution the value of the "XResolution" tag
     * @param yResolution the value of the "YResolution" tag
     * @param xPosition the value of the "XPosition" tag
     * @param yPosition the value of the "YPosition" tag
     */
    public static void writeTiff(String path, BufferedImage image, int xResolution, int yResolution, int xPosition, int yPosition) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("tiff").next();
        try (ImageOutputStream outputStream = ImageIO.createImageOutputStream(new File(path))) {
            writer.setOutput(outputStream);

            ImageWriteParam params = writer.getDefaultWriteParam();

            TIFFDirectory tiffDirectory = TIFFDirectory.createFromMetadata(writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), params));
            tiffDirectory.addTIFFField(new TIFFField(
                    BaselineTIFFTagSet.getInstance().getTag(BaselineTIFFTagSet.TAG_X_RESOLUTION),
                    TIFFTag.TIFF_RATIONAL,
                    1,
                    new long[][]{{xResolution, 1}}
            ));
            tiffDirectory.addTIFFField(new TIFFField(
                    BaselineTIFFTagSet.getInstance().getTag(BaselineTIFFTagSet.TAG_Y_RESOLUTION),
                    TIFFTag.TIFF_RATIONAL,
                    1,
                    new long[][]{{yResolution, 1}}
            ));
            tiffDirectory.addTIFFField(new TIFFField(
                    BaselineTIFFTagSet.getInstance().getTag(BaselineTIFFTagSet.TAG_X_POSITION),
                    TIFFTag.TIFF_RATIONAL,
                    1,
                    new long[][]{{xPosition, 1}}
            ));
            tiffDirectory.addTIFFField(new TIFFField(
                    BaselineTIFFTagSet.getInstance().getTag(BaselineTIFFTagSet.TAG_Y_POSITION),
                    TIFFTag.TIFF_RATIONAL,
                    1,
                    new long[][]{{yPosition, 1}}
            ));

            writer.write(null, new IIOImage(image, null, tiffDirectory.getAsMetadata()), params);
        }
        writer.dispose();
    }

    /**
     * Assert that two RGB buffered images are equal.
     *
     * @param expectedImage the expected image
     * @param actualImage the actual image
     */
    public static void assertRgbBufferedImagesEqual(BufferedImage expectedImage, BufferedImage actualImage) {
        Assertions.assertEquals(expectedImage.getWidth(), actualImage.getWidth());
        Assertions.assertEquals(expectedImage.getHeight(), actualImage.getHeight());

        int[] expectedPixels = expectedImage.getRGB(
                0,
                0,
                expectedImage.getWidth(),
                expectedImage.getHeight(),
                null,
                0,
                expectedImage.getWidth()
        );
        int[] actualPixels = actualImage.getRGB(
                0,
                0,
                actualImage.getWidth(),
                actualImage.getHeight(),
                null,
                0,
                actualImage.getWidth()
        );

        Assertions.assertArrayEquals(expectedPixels, actualPixels);
    }

    /**
     * Assert that two lists are equal without taking the order
     * of elements into account.
     * This function doesn't work if some duplicates are present in one
     * of the list.
     *
     * @param expectedCollection the expected values
     * @param actualCollection the actual values
     * @param <T> the type of the elements of the collection
     */
    public static <T> void assertCollectionsEqualsWithoutOrder(Collection<? extends T> expectedCollection, Collection<? extends T> actualCollection) {
        if (expectedCollection.size() != actualCollection.size()) {
            throw new AssertionFailedError(String.format(
                    "Expected collection size: %d but was: %d",
                    expectedCollection.size(),
                    actualCollection.size())
            );
        }

        if (!expectedCollection.containsAll(actualCollection) || !actualCollection.containsAll(expectedCollection)) {
            throw new AssertionFailedError(String.format(
                    "Expected collection: %s but was: %s",
                    expectedCollection,
                    actualCollection
            ));
        }
    }
}
