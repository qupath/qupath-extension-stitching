package qupath.ext.stitching.core.positionfinders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.ext.stitching.core.ImageUtils;
import qupath.lib.images.servers.AbstractTileableImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.TileRequest;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class TestTiffTagPositionFinder {

    @Test
    void Check_Null_Server() {
        PositionFinder positionFinder = new TiffTagPositionFinder();

        Assertions.assertThrows(NullPointerException.class, () -> positionFinder.findPosition(null));
    }

    @Test
    void Check_Server_With_No_Uri() throws Exception {
        SampleImageServer server = new SampleImageServer(List.of());
        PositionFinder positionFinder = new TiffTagPositionFinder();

        Assertions.assertThrows(RuntimeException.class, () -> positionFinder.findPosition(server));

        server.close();
    }

    @Test
    void Check_No_File_On_Path() throws Exception {
        SampleImageServer server = new SampleImageServer(List.of(Path.of("/some/file").toUri()));
        PositionFinder positionFinder = new TiffTagPositionFinder();

        Assertions.assertThrows(RuntimeException.class, () -> positionFinder.findPosition(server));

        server.close();
    }

    @Test
    void Check_No_Tiff_Tags() throws Exception {
        Path path = Files.createTempFile(null, ".tiff");
        ImageIO.write(ImageUtils.createSampleImage(2, 3, Color.WHITE), "tiff", new File(path.toString()));
        SampleImageServer server = new SampleImageServer(List.of(path.toUri()));
        PositionFinder positionFinder = new TiffTagPositionFinder();

        Assertions.assertThrows(RuntimeException.class, () -> positionFinder.findPosition(server));

        server.close();
        Files.delete(path);
    }

    @Test
    void Check_Position() throws Exception {
        Path path = Files.createTempFile(null, ".tiff");
        ImageUtils.writeTiff(path.toString(), ImageUtils.createSampleImage(2, 3, Color.WHITE), 7, 3, 7, 4);
        SampleImageServer server = new SampleImageServer(List.of(path.toUri()));
        PositionFinder positionFinder = new TiffTagPositionFinder();
        int[] expectedPosition = new int[] { 49, 12 };

        int[] position = positionFinder.findPosition(server);

        Assertions.assertArrayEquals(expectedPosition, position);

        server.close();
        Files.delete(path);

    }

    private static class SampleImageServer extends AbstractTileableImageServer {

        private final List<URI> uris;
        private final ImageServerMetadata metadata;

        public SampleImageServer(List<URI> uris) {
            this.uris = uris;

            this.metadata = new ImageServerMetadata.Builder()
                    .width(1)
                    .height(1)
                    .build();
        }

        @Override
        protected BufferedImage readTile(TileRequest tileRequest) {
            return null;
        }

        @Override
        protected ImageServerBuilder.ServerBuilder<BufferedImage> createServerBuilder() {
            return null;
        }

        @Override
        protected String createID() {
            return "";
        }

        @Override
        public Collection<URI> getURIs() {
            return uris;
        }

        @Override
        public String getServerType() {
            return "";
        }

        @Override
        public ImageServerMetadata getOriginalMetadata() {
            return metadata;
        }
    }
}
