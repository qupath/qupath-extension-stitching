package qupath.ext.stitching.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.stitching.Utils;
import qupath.lib.regions.RegionRequest;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TestImageStitcher {

    @Test
    void Check_No_Image_Given() {
        List<String> imagePaths = List.of();

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ImageStitcher.Builder(imagePaths).build()
        );
    }

    @Test
    void Check_No_Tiff_Image_Given() throws IOException {
        Path imagePath = Files.createTempFile(null, ".tiff");
        Files.writeString(imagePath, "some content");
        List<String> imagePaths = List.of(imagePath.toString());

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ImageStitcher.Builder(imagePaths).build()
        );

        Files.delete(imagePath);
    }

    @Test
    void Check_No_Tiff_Image_With_Required_Tags_Given() throws IOException {
        String imagePath = Files.createTempFile(null, ".tiff").toString();
        ImageIO.write(ImageUtils.createSampleImage(1, 1, Color.WHITE), "tiff", new File(imagePath));
        List<String> imagePaths = List.of(imagePath);

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> new ImageStitcher.Builder(imagePaths).build()
        );

        Files.delete(Path.of(imagePath));
    }

    @Test
    void Check_Width_Of_Assembled_Image() throws IOException, InterruptedException {
        String imagePath1 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath1, ImageUtils.createSampleImage(2, 3, Color.WHITE), 1, 1, 0, 0);
        String imagePath2 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath2, ImageUtils.createSampleImage(2, 3, Color.WHITE), 1, 1, 2, 4);
        List<String> imagePaths = List.of(imagePath1, imagePath2);
        int expectedWidth = 4;

        ImageStitcher imageStitcher = new ImageStitcher.Builder(imagePaths).build();

        Assertions.assertEquals(expectedWidth, imageStitcher.getServer().getWidth());

        Files.delete(Path.of(imagePath1));
        Files.delete(Path.of(imagePath2));
    }

    @Test
    void Check_Height_Of_Assembled_Image() throws IOException, InterruptedException {
        String imagePath1 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath1, ImageUtils.createSampleImage(2, 3, Color.WHITE), 1, 1, 0, 0);
        String imagePath2 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath2, ImageUtils.createSampleImage(2, 3, Color.WHITE), 1, 1, 2, 4);
        List<String> imagePaths = List.of(imagePath1, imagePath2);
        int expectedHeight = 7;

        ImageStitcher imageStitcher = new ImageStitcher.Builder(imagePaths).build();

        Assertions.assertEquals(expectedHeight, imageStitcher.getServer().getHeight());

        Files.delete(Path.of(imagePath1));
        Files.delete(Path.of(imagePath2));
    }

    @Test
    void Check_Pixels_Of_Assembled_Image() throws IOException, InterruptedException {
        String imagePath1 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath1, ImageUtils.createSampleImage(2, 3, Color.RED), 1, 1, 0, 0);
        String imagePath2 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath2, ImageUtils.createSampleImage(2, 3, Color.BLUE), 1, 1, 2, 4);
        List<String> imagePaths = List.of(imagePath1, imagePath2);
        BufferedImage expectedImage = new BufferedImage(4, 7, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = expectedImage.createGraphics();
        graphics.setColor(Color.RED);
        graphics.fillRect(0, 0, 2, 3);
        graphics.setColor(Color.BLUE);
        graphics.fillRect(2, 4, 2, 3);
        graphics.dispose();

        ImageStitcher imageStitcher = new ImageStitcher.Builder(imagePaths).build();

        ImageUtils.assertRgbBufferedImagesEqual(
                expectedImage,
                imageStitcher.getServer().readRegion(RegionRequest.createInstance(imageStitcher.getServer()))
        );

        Files.delete(Path.of(imagePath1));
        Files.delete(Path.of(imagePath2));
    }

    @Test
    void Check_Zarr_Not_Written_With_Invalid_Path() throws IOException {
        String imagePath1 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath1, ImageUtils.createSampleImage(2, 3, Color.WHITE), 1, 1, 0, 0);
        String imagePath2 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath2, ImageUtils.createSampleImage(2, 3, Color.WHITE), 1, 1, 2, 4);
        List<String> imagePaths = List.of(imagePath1, imagePath2);
        String outputPath = Files.createTempDirectory(null).resolve("invalid.path").toString();

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ImageStitcher.Builder(imagePaths).build().writeToZarrFile(outputPath, null));

        Files.delete(Path.of(imagePath1));
        Files.delete(Path.of(imagePath2));
        Utils.deleteDirectoryRecursively(Paths.get(outputPath).getParent().toFile());
    }

    @Test
    void Check_Zarr_Not_Written_When_File_Already_Exists() throws IOException {
        String imagePath1 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath1, ImageUtils.createSampleImage(2, 3, Color.WHITE), 1, 1, 0, 0);
        String imagePath2 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath2, ImageUtils.createSampleImage(2, 3, Color.WHITE), 1, 1, 2, 4);
        List<String> imagePaths = List.of(imagePath1, imagePath2);
        String outputPath = Files.createTempFile(null,"ome.zarr").toString();

        Assertions.assertThrows(IllegalArgumentException.class, () -> new ImageStitcher.Builder(imagePaths).build().writeToZarrFile(outputPath, null));

        Files.delete(Path.of(imagePath1));
        Files.delete(Path.of(imagePath2));
        Files.delete(Path.of(outputPath));
    }

    @Test
    void Check_Zarr_File_Written() throws IOException, InterruptedException {
        String imagePath1 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath1, ImageUtils.createSampleImage(2, 3, Color.WHITE), 1, 1, 0, 0);
        String imagePath2 = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(imagePath2, ImageUtils.createSampleImage(2, 3, Color.WHITE), 1, 1, 2, 4);
        List<String> imagePaths = List.of(imagePath1, imagePath2);
        Path outputPath = Path.of(Files.createTempDirectory(null).resolve("image.ome.zarr").toString());

        new ImageStitcher.Builder(imagePaths).build().writeToZarrFile(outputPath.toString(), null);

        Assertions.assertTrue(Files.exists(outputPath));

        Files.delete(Path.of(imagePath1));
        Files.delete(Path.of(imagePath2));
        Utils.deleteDirectoryRecursively(outputPath.getParent().toFile());
    }
}
