package qupath.ext.stitching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.common.GeneralTools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ResourceBundle;

/**
 * Utility methods for the whole extension.
 */
public class Utils {

    private static final Logger logger = LoggerFactory.getLogger(Utils.class);
    private static final ResourceBundle resources = ResourceBundle.getBundle("qupath.ext.stitching.strings");

    private Utils() {
        throw new AssertionError("This class is not instantiable.");
    }

    /**
     * @return the resources containing the localized strings of the extension
     */
    public static ResourceBundle getResources() {
        return resources;
    }

    /**
     * Attempt to move the provided file to trash, or delete it (and all its children
     * recursively if it's a directory) if the current platform does not support moving
     * files to trash.
     * This won't do anything if the provided file doesn't exist.
     *
     * @param fileOrDirectoryToDelete the file or directory to delete
     * @throws IOException if an I/O error occurs
     * @throws NullPointerException if the provided file or directory is null
     */
    public static void moveFileOrDirectoryToTrashOrDeleteRecursively(File fileOrDirectoryToDelete) throws IOException {
        if (!fileOrDirectoryToDelete.exists()) {
            logger.debug("Can't delete {}: the path does not exist", fileOrDirectoryToDelete);
            return;
        }

        if (!GeneralTools.moveToTrash(fileOrDirectoryToDelete)) {
            deleteFileOrDirectoryRecursively(fileOrDirectoryToDelete);
        }
    }

    /**
     * Delete the file and all its children recursively if it's a directory.
     * This won't do anything if the provided file doesn't exist.
     *
     * @param directoryToBeDeleted the file or directory to delete
     * @throws IOException if an I/O error occurs
     * @throws SecurityException if the user doesn't have sufficient rights to delete
     * some files
     * @throws NullPointerException if the provided file or directory is null
     */
    public static void deleteFileOrDirectoryRecursively(File directoryToBeDeleted) throws IOException {
        logger.debug("Deleting children of {}", directoryToBeDeleted);
        File[] childFiles = directoryToBeDeleted.listFiles();
        if (childFiles != null) {
            for (File file : childFiles) {
                deleteFileOrDirectoryRecursively(file);
            }
        }

        logger.debug("Deleting {}", directoryToBeDeleted);
        Files.deleteIfExists(directoryToBeDeleted.toPath());
    }
}
