package qupath.ext.stitching;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.common.GeneralTools;

import javax.swing.SwingUtilities;
import java.awt.Desktop;
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

        if (!moveToTrash(fileOrDirectoryToDelete)) {
            deleteFileOrDirectoryRecursively(fileOrDirectoryToDelete);
        }
    }

    //TODO: remove those two functions and use GeneralTools.moveToTrash() instead when switching to QuPath 0.7
    private static boolean moveToTrash(File fileOrDirectory) {
        if (Desktop.isDesktopSupported()) {
            try {
                var desktop = Desktop.getDesktop();
                return desktop.isSupported(Desktop.Action.MOVE_TO_TRASH) && moveToTrash(desktop, fileOrDirectory);
            } catch (UnsupportedOperationException e) {
                logger.warn("Cannot move {} to trash", fileOrDirectory, e);
                return false;
            }
        } else {
            logger.warn("Desktop class not supported. Cannot move {} to trash", fileOrDirectory);
            return false;
        }
    }
    private static boolean moveToTrash(Desktop desktop, File fileOrDirectory) {
        if (SwingUtilities.isEventDispatchThread() || !GeneralTools.isWindows()) {
            // It seems safe to call move to trash from any thread on macOS and Linux
            // We can't use the EDT on macOS because of https://bugs.openjdk.org/browse/JDK-8087465
            return desktop.moveToTrash(fileOrDirectory);
        } else {
            // EXCEPTION_ACCESS_VIOLATION associated with moveToTrash reported on Windows 11.
            // https://github.com/qupath/qupath/issues/1738
            // Could not be replicated (but we didn't have Windows 11...); taking a guess that this might help.
            try {
                SwingUtilities.invokeAndWait(() -> moveToTrash(desktop, fileOrDirectory));
            } catch (Exception e) {
                logger.error("Exception moving file to trash: {}", e.getMessage(), e);
                return false;
            }
            return !fileOrDirectory.exists();
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
