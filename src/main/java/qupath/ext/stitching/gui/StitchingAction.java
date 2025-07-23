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
import qupath.lib.gui.tools.GuiTools;
import qupath.lib.plugins.parameters.ParameterList;

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

        ParameterList parameters = createParameterList();
        if (!GuiTools.showParameterDialog(resources.getString("StitchingAction.title"), parameters)) {
            return;
        }
        logger.debug("Got parameters {} for stitching", parameters);

        List<File> inputFiles = FileChoosers.promptForMultipleFiles(
                resources.getString("StitchingAction.chooseInputImages"),
                FileChoosers.createExtensionFilter("TIFF files", ".tif", ".tiff")
        );
        if (inputFiles == null || inputFiles.isEmpty()) {
            return;
        }
        logger.debug("Got files {} to stitch", inputFiles);

        File outputFile = FileChoosers.promptToSaveFile(
                resources.getString("StitchingAction.chooseOutputPath"),
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
                    resources.getString("StitchingAction.warning"),
                    resources.getString("StitchingAction.fileAlreadyExists")
            )) {
                return;
            }
            try {
                logger.debug("Deletion of {} accepted. Moving it to trash (or deleting it if moving to trash not supported)", outputFile);
                Utils.moveDirectoryToTrashOrDeleteRecursively(outputFile);
            } catch (IOException | SecurityException e) {
                logger.error("Cannot delete {} file", outputFile, e);

                Dialogs.showErrorMessage(
                        resources.getString("StitchingAction.stitchingFailed"),
                        resources.getString("StitchingAction.fileAlreadyExistsAndCannotBeDeleted")
                );
            }
        }

        stitchImages(inputFiles.stream().map(File::getPath).toList(), outputFile.getPath(), parameters);
    }

    /**
     * @return a localized string describing this action
     */
    public static String getTitle() {
        return resources.getString("StitchingAction.title");
    }

    private static ParameterList createParameterList() {
        return new ParameterList()
                .addIntParameter(
                        "numberOfThreads",
                        resources.getString("StitchingAction.numberOfThreads"),
                        Runtime.getRuntime().availableProcessors(),
                        "",
                        1,
                        Runtime.getRuntime().availableProcessors(),
                        resources.getString("StitchingAction.numberOfThreadsDescription")
                )
                .addBooleanParameter(
                        "pyramidalize",
                        resources.getString("StitchingAction.pyramidalize"),
                        true,
                        resources.getString("StitchingAction.pyramidalizeDescription")
                );
    }

    private void stitchImages(List<String> inputImages, String outputImage, ParameterList parameters) {
        ExecutorService executor = Executors.newSingleThreadExecutor(ThreadTools.createThreadFactory("stitching-action-", false));
        ProgressWindow progressWindow;
        try {
            progressWindow = new ProgressWindow(
                    resources.getString("StitchingAction.stitchingImages"),
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
                Platform.runLater(() -> progressWindow.setStatus(resources.getString("StitchingAction.parsingInputImages")));
                ImageStitcher imageStitcher = new ImageStitcher.Builder(inputImages)
                        .setNumberOfThreads(parameters.getIntParameterValue("numberOfThreads"))
                        .pyramidalize(parameters.getBooleanParameterValue("pyramidalize"))
                        .setOnProgress(progress -> Platform.runLater(() -> progressWindow.setProgress(progress / 2)))
                        .build();

                Platform.runLater(() -> progressWindow.setStatus(resources.getString("StitchingAction.writingOutputImage")));
                imageStitcher.writeToZarrFile(
                        outputImage,
                        progress -> Platform.runLater(() -> progressWindow.setProgress(0.5f + progress/2))
                );

                Platform.runLater(() -> {
                    progressWindow.close();

                    Dialogs.showInfoNotification(
                            resources.getString("StitchingAction.stitchingCompleted"),
                            resources.getString(imageStitcher.areSomeInputImagesNotUsed() ?
                                    "StitchingAction.someImagesCombined" :
                                    "StitchingAction.providedImagesCombined"
                            )
                    );
                });
            } catch (Exception e) {
                Platform.runLater(progressWindow::close);

                if (e instanceof InterruptedException || (e.getCause() != null && e.getCause() instanceof InterruptedException)) {
                    logger.debug("Stitching {} to {} interrupted", inputImages, outputImage, e);
                } else {
                    logger.error("Error when stitching {} to {}", inputImages, outputImage, e);

                    Platform.runLater(() -> Dialogs.showErrorMessage(
                            resources.getString("StitchingAction.stitchingFailed"),
                            MessageFormat.format(
                                    resources.getString("StitchingAction.errorWhenStitching"),
                                    e.getLocalizedMessage()
                            )
                    ));
                }
            }
        });
        executor.shutdown();
    }
}
