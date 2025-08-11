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
    private enum ImageFormat {
        OME_ZARR("OME-Zarr"),
        OME_TIFF("OME-TIFF");

        private final String name;

        ImageFormat(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

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

        ImageFormat imageFormat = (ImageFormat) parameters.getChoiceParameterValue("imageFormat");

        File outputFile = FileChoosers.promptToSaveFile(
                resources.getString("StitchingAction.chooseOutputPath"),
                new File(""),
                switch (imageFormat) {
                    case OME_ZARR -> FileChoosers.createExtensionFilter("OME-Zarr", ".ome.zarr");
                    case OME_TIFF -> FileChoosers.createExtensionFilter("OME-TIFF", ".ome.tiff");
                }
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
                Utils.moveFileOrDirectoryToTrashOrDeleteRecursively(outputFile);
            } catch (IOException e) {
                logger.error("Cannot delete {} file", outputFile, e);

                Dialogs.showErrorMessage(
                        resources.getString("StitchingAction.stitchingFailed"),
                        resources.getString("StitchingAction.fileAlreadyExistsAndCannotBeDeleted")
                );
            }
        }

        stitchImages(inputFiles.stream().map(File::getPath).toList(), outputFile.getPath(), parameters, imageFormat);
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
                )
                .addChoiceParameter(
                        "imageFormat",
                        resources.getString("StitchingAction.imageFormat"),
                        ImageFormat.OME_ZARR,
                        List.of(ImageFormat.values()),
                        resources.getString("StitchingAction.imageFormatDescription")
                );
    }

    private void stitchImages(List<String> inputImages, String outputImage, ParameterList parameters, ImageFormat imageFormat) {
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
                        .setOnProgress(progress -> Platform.runLater(() -> progressWindow.setProgress(switch (imageFormat) {
                            case OME_ZARR -> progress / 2;
                            case OME_TIFF -> progress;
                        })))
                        .build();

                switch (imageFormat) {
                    case OME_ZARR -> {
                        Platform.runLater(() -> progressWindow.setStatus(resources.getString("StitchingAction.writingOutputZarrImage")));
                        imageStitcher.writeToZarrFile(
                                outputImage,
                                progress -> Platform.runLater(() -> progressWindow.setProgress(0.5f + progress/2)));
                    }
                    case OME_TIFF -> {
                        Platform.runLater(() -> {
                            progressWindow.setStatus(resources.getString("StitchingAction.writingOutputTiffImage"));
                            progressWindow.setUndefinedProgress();
                        });
                        imageStitcher.writeToTiffFile(outputImage);
                    }
                }

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
