package qupath.ext.stitching.core.positionfinders;

import qupath.lib.images.servers.ImageServer;

import java.io.IOException;

/**
 * A class that can find a tile position within an image server.
 */
@FunctionalInterface
public interface PositionFinder {

    /**
     * Find the [x,y] position (in pixel coordinates) of the tile represented by the provided image server.
     *
     * @param server a server representing a tile whose position should be determined
     * @return an integer array of size two, the first element being the x-coordinate and the second element being
     * the y-coordinate of the tile represented by the provided server
     * @throws IOException if an error occurred while determining the position
     * @throws NullPointerException if the provided server is null
     * @throws RuntimeException if the provided server doesn't contain a position as described by this class
     */
    int[] findPosition(ImageServer<?> server) throws IOException;
}
