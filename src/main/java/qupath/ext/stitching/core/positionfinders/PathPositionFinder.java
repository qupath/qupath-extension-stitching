package qupath.ext.stitching.core.positionfinders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.PixelCalibration;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A position that looks at the first URI of the input image to find a position.
 * <p>
 * More precisely, a [xPosition; yPosition] string is expected from the first URI, where positions are expressed in
 * micrometers.
 */
public class PathPositionFinder implements PositionFinder {

    private static final Logger logger = LoggerFactory.getLogger(PathPositionFinder.class);
    private static final Pattern POSITION_IN_NAME_PATTERN = Pattern.compile("\\[([\\d.]+),([\\d.]+)]");

    @Override
    public int[] findPosition(ImageServer<?> server) {
        String path;
        if (server.getURIs().isEmpty()) {
            throw new IllegalArgumentException(String.format("The provided server %s doesn't have any URI", server));
        } else {
            if (server.getURIs().size() > 1) {
                logger.debug("Multiple URIs found for {}. Only considering the first one to find position", server);
            }
            path = server.getURIs().iterator().next().getPath();
        }
        Matcher matcher = POSITION_IN_NAME_PATTERN.matcher(path);

        if (matcher.find() && matcher.groupCount() > 1) {
            float x = Float.parseFloat(matcher.group(1));
            float y = Float.parseFloat(matcher.group(2));
            PixelCalibration pixelCalibration = server.getMetadata().getPixelCalibration();

            return new int[] {
                    (int) Math.round(Double.isNaN(pixelCalibration.getPixelWidthMicrons()) ? x : x / pixelCalibration.getPixelWidthMicrons()),
                    (int) Math.round(Double.isNaN(pixelCalibration.getPixelHeightMicrons()) ? y : y / pixelCalibration.getPixelHeightMicrons())
            };
        } else {
            throw new IllegalArgumentException(String.format("No X or Y position found in %s", path));
        }
    }
}
