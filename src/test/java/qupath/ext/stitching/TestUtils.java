package qupath.ext.stitching;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestUtils {

    @Test
    void Check_File_Deleted_Or_Moved_To_Trash() throws IOException {
        Path file = Files.createTempFile(null, null);

        Utils.moveFileOrDirectoryToTrashOrDeleteRecursively(file.toFile());

        Assertions.assertFalse(Files.exists(file));
    }

    @Test
    void Check_Empty_Directory_Deleted_Or_Moved_To_Trash() throws IOException {
        Path directory = Files.createTempDirectory(null);

        Utils.moveFileOrDirectoryToTrashOrDeleteRecursively(directory.toFile());

        Assertions.assertFalse(Files.exists(directory));
    }

    @Test
    void Check_Non_Empty_Directory_Deleted_Or_Moved_To_Trash() throws IOException {
        Path directory = Files.createTempDirectory(null);
        Files.createFile(directory.resolve("file"));

        Utils.moveFileOrDirectoryToTrashOrDeleteRecursively(directory.toFile());

        Assertions.assertFalse(Files.exists(directory));
    }

    @Test
    void Check_File_Deleted() throws IOException {
        Path file = Files.createTempFile(null, null);

        Utils.deleteFileOrDirectoryRecursively(file.toFile());

        Assertions.assertFalse(Files.exists(file));
    }

    @Test
    void Check_Empty_Directory_Deleted() throws IOException {
        Path directory = Files.createTempDirectory(null);

        Utils.deleteFileOrDirectoryRecursively(directory.toFile());

        Assertions.assertFalse(Files.exists(directory));
    }

    @Test
    void Check_Non_Empty_Directory_Deleted() throws IOException {
        Path directory = Files.createTempDirectory(null);
        Files.createFile(directory.resolve("file"));

        Utils.deleteFileOrDirectoryRecursively(directory.toFile());

        Assertions.assertFalse(Files.exists(directory));
    }
}
