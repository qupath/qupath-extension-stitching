package qupath.ext.stitching.gui;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.stitching.Utils;
import qupath.ext.stitching.core.ImageStitcher;
import qupath.fx.dialogs.Dialogs;
import qupath.fx.dialogs.FileChoosers;
import qupath.lib.common.ThreadTools;
import qupath.lib.gui.QuPathGUI;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.MessageFormat;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * An action to stitch images and save the resulting image with the Zarr format.
 */
class StitchingAction implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(StitchingAction.class);
    private static final ResourceBundle resources = Utils.getResources();
    private final QuPathGUI quPath;

    /**
     * Create the action.
     *
     * @param quPath the QuPathGUI owning this action
     */
    public StitchingAction(QuPathGUI quPath) {
        this.quPath = quPath;
    }

    @Override
    public void run() {
        logger.debug("Stitching action called for {}", quPath);

        List<File> inputFiles = FileChoosers.promptForMultipleFiles(
                resources.getString("stitcherAction.chooseInputImages"),
                FileChoosers.createExtensionFilter("TIFF files", ".tif", ".tiff")
        );
        if (inputFiles == null || inputFiles.isEmpty()) {
            return;
        }
        logger.debug("Got files {} to stitch", inputFiles);

        File outputFile = FileChoosers.promptToSaveFile(
                resources.getString("stitcherAction.chooseOutputPath"),
                new File(""),
                FileChoosers.createExtensionFilter("OME-Zarr", ".ome.zarr")
        );
        if (outputFile == null) {
            return;
        }
        logger.debug("Got file {} to write stitched image", outputFile);

        if (Files.exists(outputFile.toPath())) {
            logger.debug("A file already exists at {}. Asking user if possible to delete it", outputFile);

            if (!Dialogs.showConfirmDialog(
                    resources.getString("stitcherAction.warning"),
                    resources.getString("stitcherAction.fileAlreadyExists")
            )) {
                return;
            }
            try {
                logger.debug("Deletion of {} accepted. Moving it to trash (or deleting it if moving to trash not supported)", outputFile);
                Utils.moveDirectoryToTrashOrDeleteRecursively(outputFile);
            } catch (IOException | SecurityException e) {
                logger.error("Cannot delete {} file", outputFile, e);

                Dialogs.showErrorMessage(
                        resources.getString("stitcherAction.stitchingFailed"),
                        resources.getString("stitcherAction.fileAlreadyExistsAndCannotBeDeleted")
                );
            }
        }

        stitchImages(inputFiles.stream().map(File::getPath).toList(), outputFile.getPath());
    }

    /**
     * @return a localized string describing this action
     */
    public static String getTitle() {
        return resources.getString("stitcherAction.title");
    }

    private void stitchImages(List<String> inputImages, String outputImage) {
        ExecutorService executor = Executors.newSingleThreadExecutor(ThreadTools.createThreadFactory("stitching-action-", false));
        ProgressWindow progressWindow;
        try {
            progressWindow = new ProgressWindow(
                    resources.getString("stitcherAction.stitchingImages"),
                    executor::shutdownNow
            );
            progressWindow.initOwner(quPath.getStage());
        } catch (IOException e) {
            logger.error("Error while creating progress window", e);
            executor.shutdown();
            return;
        }
        progressWindow.show();

        executor.execute(() -> {
            try {
                Platform.runLater(() -> progressWindow.setStatus(resources.getString("stitcherAction.parsingInputImages")));
                ImageStitcher imageStitcher = new ImageStitcher.Builder(inputImages)
                        .setOnProgress(progress -> Platform.runLater(() -> progressWindow.setProgress(progress / 2)))
                        .build();

                Platform.runLater(() -> progressWindow.setStatus(resources.getString("stitcherAction.writingOutputImage")));
                imageStitcher.writeToZarrFile(
                        outputImage,
                        progress -> Platform.runLater(() -> progressWindow.setProgress(0.5f + progress/2))
                );

                Platform.runLater(() -> {
                    progressWindow.close();

                    Dialogs.showInfoNotification(
                            resources.getString("stitcherAction.stitchingCompleted"),
                            resources.getString(imageStitcher.areSomeInputImagesNotUsed() ?
                                    "stitcherAction.someImagesCombined" :
                                    "stitcherAction.providedImagesCombined"
                            )
                    );
                });
            } catch (Exception e) {
                Platform.runLater(progressWindow::close);

                if (e instanceof InterruptedException) {
                    logger.debug("Stitching {} to {} interrupted", inputImages, outputImage, e);
                } else {
                    logger.error("Error when stitching {} to {}", inputImages, outputImage, e);

                    Platform.runLater(() -> Dialogs.showErrorMessage(
                            resources.getString("stitcherAction.stitchingFailed"),
                            MessageFormat.format(
                                    resources.getString("stitcherAction.errorWhenStitching"),
                                    e.getLocalizedMessage()
                            )
                    ));
                }
            }
        });
        executor.shutdown();
    }
}
