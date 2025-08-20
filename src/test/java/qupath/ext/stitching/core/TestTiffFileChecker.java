package qupath.ext.stitching.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.stitching.Utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class TestTiffFileChecker {

    @Test
    void Check_Null_Path() {
        Assertions.assertThrows(
                NullPointerException.class,
                () -> TiffFileChecker.checkTiffFile(null)
        );
    }

    @Test
    void Check_File_Does_Not_Exit() throws IOException {
        String path = Files.createTempDirectory(null).resolve("no_file").toString();

        Assertions.assertThrows(IOException.class, () -> TiffFileChecker.checkTiffFile(path));

        Utils.deleteFileOrDirectoryRecursively(Path.of(path).getParent().toFile());
    }

    @Test
    void Check_Empty_Tiff_File() throws IOException {
        String path = Files.createTempFile(null, ".tiff").toString();

        Assertions.assertThrows(IllegalArgumentException.class, () -> TiffFileChecker.checkTiffFile(path));

        Files.delete(Path.of(path));
    }

    @Test
    void Check_Non_Tiff_File() throws IOException {
        Path tempFilePath = Files.createTempFile(null, ".tiff");
        Files.writeString(tempFilePath, "some content");
        String path = tempFilePath.toString();

        Assertions.assertThrows(IllegalArgumentException.class, () -> TiffFileChecker.checkTiffFile(path));

        Files.delete(tempFilePath);
    }

    @Test
    void Check_Tiff_File_With_First_Sequence() throws IOException {
        Path tempFilePath = Files.createTempFile(null, ".tiff");
        Files.write(tempFilePath, new byte[] {0x49, 0x49, 0x2A, 0x00}, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.writeString(tempFilePath, "some content", StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        String path = tempFilePath.toString();

        Assertions.assertDoesNotThrow(() -> TiffFileChecker.checkTiffFile(path));

        Files.delete(tempFilePath);
    }

    @Test
    void Check_Tiff_File_With_Second_Sequence() throws IOException {
        Path tempFilePath = Files.createTempFile(null, ".tiff");
        Files.write(tempFilePath, new byte[] {0x4D, 0x4D, 0x00, 0x2A}, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        Files.writeString(tempFilePath, "some content", StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        String path = tempFilePath.toString();

        Assertions.assertDoesNotThrow(() -> TiffFileChecker.checkTiffFile(path));

        Files.delete(tempFilePath);
    }
}
