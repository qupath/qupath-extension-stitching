package qupath.ext.stitching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
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
     * @param directoryToDelete the file or directory to delete
     * @throws IOException if an I/O error occurs
     * @throws SecurityException if the user doesn't have sufficient rights to move or
     * delete some files
     * @throws NullPointerException if the provided file or directory is null
     */
    public static void moveDirectoryToTrashOrDeleteRecursively(File directoryToDelete) throws IOException {
        if (!directoryToDelete.exists()) {
            logger.debug("Can't delete {}: the path does not exist", directoryToDelete);
            return;
        }

        Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
        if (desktop != null && desktop.isSupported(Desktop.Action.MOVE_TO_TRASH)) {
            logger.debug("Moving {} to trash", directoryToDelete);
            desktop.moveToTrash(directoryToDelete);
        } else {
            logger.debug("Moving to trash not supported. Deleting {}", directoryToDelete);
            deleteDirectoryRecursively(directoryToDelete);
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
    public static void deleteDirectoryRecursively(File directoryToBeDeleted) throws IOException {
        logger.debug("Deleting children of {}", directoryToBeDeleted);
        File[] childFiles = directoryToBeDeleted.listFiles();
        if (childFiles != null) {
            for (File file : childFiles) {
                deleteDirectoryRecursively(file);
            }
        }

        logger.debug("Deleting {}", directoryToBeDeleted);
        Files.deleteIfExists(directoryToBeDeleted.toPath());
    }
}
