package qupath.ext.stitching.core.positionfinders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qupath.lib.images.servers.ImageServer;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.plugins.tiff.BaselineTIFFTagSet;
import javax.imageio.plugins.tiff.TIFFDirectory;
import javax.imageio.plugins.tiff.TIFFField;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

/**
 * A position finder that looks at the "XPosition", "YPosition", "XResolution", and "YResolution" TIFF tags of the
 * input image.
 * <p>
 * The X position is given by "XPosition" * "XResolution" (likewise for the Y position).
 * <p>
 * The input image must be a TIFF file containing the above tags. Its path must be contained in the first URI returned
 * by {@link ImageServer#getURIs()} of the provided server.
 */
public class TiffTagPositionFinder implements PositionFinder {

    private static final Logger logger = LoggerFactory.getLogger(TiffTagPositionFinder.class);

    @Override
    public int[] findPosition(ImageServer<?> server) throws IOException {
        String path;
        if (server.getURIs().isEmpty()) {
            throw new IllegalArgumentException(String.format("The provided server %s doesn't have any URI", server));
        } else {
            if (server.getURIs().size() > 1) {
                logger.debug("Multiple URIs found for {}. Only considering the first one to find position", server);
            }
            path = server.getURIs().iterator().next().getPath();
        }

        Iterator<ImageReader> readers = ImageIO.getImageReadersByFormatName("TIFF");
        if (!readers.hasNext()) {
            throw new IllegalArgumentException("No ImageIO TIFF reader found");
        }
        ImageReader reader = readers.next();

        try (ImageInputStream inputStream = ImageIO.createImageInputStream(new File(path))) {
            reader.setInput(inputStream);

            IIOMetadata metadata = reader.getImageMetadata(reader.getMinIndex());
            if (metadata == null) {
                throw new IllegalArgumentException(String.format("No metadata found in %s", path));
            }
            TIFFDirectory tiffDirectory = TIFFDirectory.createFromMetadata(metadata);

            return new int[] {
                    (int) Math.round(getTag(path, tiffDirectory, BaselineTIFFTagSet.TAG_X_RESOLUTION).getAsDouble(0) *
                            getTag(path, tiffDirectory, BaselineTIFFTagSet.TAG_X_POSITION).getAsDouble(0)
                    ),
                    (int) Math.round(getTag(path, tiffDirectory, BaselineTIFFTagSet.TAG_Y_RESOLUTION).getAsDouble(0) *
                            getTag(path, tiffDirectory, BaselineTIFFTagSet.TAG_Y_POSITION).getAsDouble(0)
                    )
            };
        }
    }

    private static TIFFField getTag(String path, TIFFDirectory tiffDirectory, int tagNumber) {
        TIFFField tag = tiffDirectory.getTIFFField(tagNumber);

        if (tag == null) {
            throw new IllegalArgumentException(String.format("The provided file %s does not contain the %d tag", path, tagNumber));
        } else {
            return tag;
        }
    }
}
