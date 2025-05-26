package qupath.ext.stitching.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.stitching.Utils;
import qupath.lib.regions.ImageRegion;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;


public class TestTiffRegionParser {

    @Test
    void Check_Null_Path() {
        Assertions.assertThrows(NullPointerException.class, () -> TiffRegionParser.parseRegion(null, 1, 1));
    }

    @Test
    void Check_File_Does_Not_Exit() throws IOException {
        String path = Files.createTempDirectory(null).resolve("no_file").toString();

        Assertions.assertThrows(IOException.class, () -> TiffRegionParser.parseRegion(path, 1, 1));

        Utils.deleteDirectoryRecursively(Path.of(path).getParent().toFile());
    }

    @Test
    void Check_Empty_Tiff_File() throws IOException {
        String path = Files.createTempFile(null, ".tiff").toString();

        Assertions.assertThrows(IllegalArgumentException.class, () -> TiffRegionParser.parseRegion(path, 1, 1));

        Files.delete(Path.of(path));
    }

    @Test
    void Check_Non_Tiff_File() throws IOException {
        Path tempFilePath = Files.createTempFile(null, ".tiff");
        Files.writeString(tempFilePath, "some content");
        String path = tempFilePath.toString();

        Assertions.assertThrows(IllegalArgumentException.class, () -> TiffRegionParser.parseRegion(path, 1, 1));

        Files.delete(tempFilePath);
    }

    @Test
    void Check_Tiff_File_With_Missing_Tags() throws IOException {
        String path = Files.createTempFile(null, ".tiff").toString();
        ImageIO.write(ImageUtils.createSampleImage(2, 3, Color.WHITE), "tiff", new File(path));

        Assertions.assertThrows(IllegalArgumentException.class, () -> TiffRegionParser.parseRegion(path, 1, 1));

        Files.delete(Path.of(path));
    }

    @Test
    void Check_Regions_Of_Tiff_File() throws IOException {
        String path = Files.createTempFile(null, ".tiff").toString();
        ImageUtils.writeTiff(path, ImageUtils.createSampleImage(2, 3, Color.WHITE), 7, 3, 7, 4);
        List<ImageRegion> expectedRegions = List.of(ImageRegion.createInstance(49, 12, 2, 3, 0, 0));   // width and height are set from buffered image size

        List<ImageRegion> regions = TiffRegionParser.parseRegion(path, 1, 1);

        ImageUtils.assertCollectionsEqualsWithoutOrder(expectedRegions, regions);

        Files.delete(Path.of(path));
    }
}
