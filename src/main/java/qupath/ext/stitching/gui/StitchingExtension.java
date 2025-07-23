package qupath.ext.stitching.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.ext.stitching.Utils;
import qupath.lib.common.Version;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.actions.ActionTools;
import qupath.lib.gui.extensions.QuPathExtension;
import qupath.lib.gui.tools.MenuTools;

import java.util.ResourceBundle;

/**
 * Install the stitching extension to a QuPath GUI.
 */
public class StitchingExtension implements QuPathExtension {

    private static final Logger logger = LoggerFactory.getLogger(StitchingExtension.class);
    private static final ResourceBundle resources = Utils.getResources();
    private static final String EXTENSION_NAME = resources.getString("Extension.name");
    private static final String EXTENSION_DESCRIPTION = resources.getString("Extension.description");
    private static final Version EXTENSION_QUPATH_VERSION = Version.parse("v0.6.0");
    private boolean isInstalled = false;

    @Override
    public void installExtension(QuPathGUI qupath) {
        if (isInstalled) {
            logger.debug("Stitching extension already installed, skipping installation");
            return;
        }
        logger.debug("Adding stitching extension to {}", qupath);

        MenuTools.addMenuItems(
                qupath.getMenu("Extensions", false),
                ActionTools.createAction(
                        new StitchingAction(qupath),
                        StitchingAction.getTitle()
                )
        );
        isInstalled = true;
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    @Override
    public String getDescription() {
        return EXTENSION_DESCRIPTION;
    }

    @Override
    public Version getQuPathVersion() {
        return EXTENSION_QUPATH_VERSION;
    }
}
