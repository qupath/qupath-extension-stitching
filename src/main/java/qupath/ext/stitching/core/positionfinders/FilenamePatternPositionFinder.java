package qupath.ext.stitching.core.positionfinders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.ImageServer;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.PixelCalibration;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A position that uses a regex on the first URI of the input image to find a position.
 * <p>
 * More precisely, the provided pattern is used to find some coordinates x,y in the image path. Those coordinates
 * are then converted to pixel units using the provided unit.
 * <p>
 * If several coordinates are found in the image path, only the last is used.
 * <p>
 * Float coordinates are rounded to the nearest integer.
 * <p>
 * Some pre-defined standards patterns are provided (see {@link Unit} and {@link #FilenamePatternPositionFinder(StandardPattern)}).
 */
public class FilenamePatternPositionFinder implements PositionFinder {

    private static final Logger logger = LoggerFactory.getLogger(FilenamePatternPositionFinder.class);
    private static final Pattern VECTRA_PATTERN = Pattern.compile("\\[([\\d.]+),([\\d.]+)]");
    private final Pattern pattern;
    private final Unit unit;
    /**
     * The unit the coordinate will have. They will be converted to pixel unit.
     */
    public enum Unit {
        /**
         * Pixel units.
         */
        PIXEL,
        /**
         * The unit defined in the image file (e.g. micrometers).
         */
        CALIBRATED,
        /**
         * 0-based indices indicating where the tile appears on the resulting image (with (0, 0) indicating the top left tile).
         */
        GRID
    }
    /**
     * Pre-defined patterns that are provided for convenience.
     */
    public enum StandardPattern {
        /**
         * Vectra pattern. It expects "[x,y]" in calibrated units.
         */
        VECTRA
    }

    /**
     * Create a filename pattern from a standard pattern.
     *
     * @param standardPattern the standard pattern to use
     */
    public FilenamePatternPositionFinder(StandardPattern standardPattern) {
        this(
                switch (standardPattern) {
                    case VECTRA -> VECTRA_PATTERN;
                },
                switch (standardPattern) {
                    case VECTRA -> Unit.CALIBRATED;
                }
        );
    }

    /**
     * Create a custom filename pattern.
     * @param pattern the pattern to use when finding coordinates in the file name. It must contain two capturing groups
     *                (the first one for the x position, and the second one for the y position)
     * @param unit the unit of the coordinates present in the file name. They will be converted to pixel coordinates
     * @throws NullPointerException if one of the provided parameter is null
     */
    public FilenamePatternPositionFinder(Pattern pattern, Unit unit) {
        this.pattern = Objects.requireNonNull(pattern);
        this.unit = Objects.requireNonNull(unit);
    }

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
        Matcher matcher = pattern.matcher(path);

        String x = null;
        String y = null;
        while (matcher.find()) {
            if (matcher.groupCount() > 1) {
                x = matcher.group(1);
                y = matcher.group(2);
            }
        }
        if (x == null || y == null) {
            throw new IllegalArgumentException(String.format("No X or Y position found in %s", path));
        }

        return getPixelCoordinates(Float.parseFloat(x), Float.parseFloat(y), server.getMetadata());
    }

    private int[] getPixelCoordinates(float x, float y, ImageServerMetadata metadata) {
        return switch (unit) {
            case PIXEL -> new int[] { Math.round(x), Math.round(y) };
            case CALIBRATED -> {
                PixelCalibration pixelCalibration = metadata.getPixelCalibration();

                yield new int[] {
                        Math.round(pixelCalibration.getPixelWidth() == null ?
                                x :
                                x / pixelCalibration.getPixelWidth().floatValue()
                        ),
                        Math.round(pixelCalibration.getPixelHeight() == null ?
                                y :
                                y / pixelCalibration.getPixelHeight().floatValue()
                        ),
                };
            }
            case GRID -> new int[] {
                    Math.round(x * metadata.getWidth()),
                    Math.round(y * metadata.getHeight())
            };
        };
    }
}
