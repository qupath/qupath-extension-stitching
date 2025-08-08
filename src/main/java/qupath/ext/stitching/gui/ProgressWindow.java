package qupath.ext.stitching.gui;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ProgressIndicator;
import javafx.stage.Modality;
import javafx.stage.Stage;
import qupath.ext.stitching.Utils;

import java.io.IOException;
import java.util.ResourceBundle;

/**
 * A modal window that displays a progress bar or a loading indicator, with a status text, while an operation is
 * happening. The operation can optionally be cancelled.
 */
class ProgressWindow extends Stage {

    private static final ResourceBundle resources = Utils.getResources();
    private final Runnable onCancelClicked;
    @FXML
    private Label label;
    @FXML
    private ProgressIndicator progressIndicator;
    @FXML
    private ProgressBar progressBar;

    /**
     * Create the window. This hides by default the loading indicator.
     *
     * @param label a text describing the operation
     * @param onCancelClicked a function that will be called when the user cancel the operation. This window is
     *                        already automatically closed when this happens
     * @throws IOException when an error occurs while creating the window
     */
    public ProgressWindow(String label, Runnable onCancelClicked) throws IOException {
        this.onCancelClicked = onCancelClicked;

        FXMLLoader loader = new FXMLLoader(ProgressWindow.class.getResource("progress_window.fxml"), resources);
        loader.setRoot(this);
        loader.setController(this);
        loader.load();

        progressIndicator.setVisible(false);

        progressIndicator.managedProperty().bind(progressIndicator.visibleProperty());
        progressBar.managedProperty().bind(progressBar.visibleProperty());

        initModality(Modality.WINDOW_MODAL);

        setTitle(label);
    }

    @FXML
    private void onCancelClicked(ActionEvent ignored) {
        close();
        onCancelClicked.run();
    }

    /**
     * Set the progress displayed by the window. This also hide the progress indicator and
     * show the progress bar.
     *
     * @param progress a number between 0 and 1, where 0 means the beginning and 1 the end of
     *                 the operation
     */
    public void setProgress(float progress) {
        progressIndicator.setVisible(false);

        progressBar.setVisible(true);
        progressBar.setProgress(progress);
    }

    /**
     * Show the progress indicator and hide the progress bar.
     */
    public void setUndefinedProgress() {
        progressIndicator.setVisible(true);

        progressBar.setVisible(false);
    }

    /**
     * Set a text describing the current step.
     *
     * @param status the current step of the operation
     */
    public void setStatus(String status) {
        this.label.setText(status);
    }
}
