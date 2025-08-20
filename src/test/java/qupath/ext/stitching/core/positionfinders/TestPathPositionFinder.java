package qupath.ext.stitching.core.positionfinders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import qupath.lib.images.servers.AbstractTileableImageServer;
import qupath.lib.images.servers.ImageServerBuilder;
import qupath.lib.images.servers.ImageServerMetadata;
import qupath.lib.images.servers.TileRequest;

import java.awt.image.BufferedImage;
import java.net.URI;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;

public class TestPathPositionFinder {

    @Test
    void Check_Null_Server() {
        PositionFinder positionFinder = new PathPositionFinder();

        Assertions.assertThrows(NullPointerException.class, () -> positionFinder.findPosition(null));
    }

    @Test
    void Check_Server_With_No_Uri() throws Exception {
        SampleImageServer server = new SampleImageServer(List.of());
        PositionFinder positionFinder = new PathPositionFinder();

        Assertions.assertThrows(RuntimeException.class, () -> positionFinder.findPosition(server));

        server.close();
    }

    @Test
    void Check_Server_With_No_Position_In_Uri() throws Exception {
        SampleImageServer server = new SampleImageServer(List.of(Path.of("/some/file.tiff").toUri()));
        PositionFinder positionFinder = new PathPositionFinder();

        Assertions.assertThrows(RuntimeException.class, () -> positionFinder.findPosition(server));

        server.close();
    }

    @Test
    void Check_Position_With_No_Pixel_Size() throws Exception {
        SampleImageServer server = new SampleImageServer(List.of(Path.of("/some/file[234.2344,587].tiff").toUri()));
        int[] expectedPosition = new int[] {234, 587};
        PositionFinder positionFinder = new PathPositionFinder();

        int[] position = positionFinder.findPosition(server);

        Assertions.assertArrayEquals(expectedPosition, position);

        server.close();
    }

    @Test
    void Check_Position_With_Pixel_Size() throws Exception {
        SampleImageServer server = new SampleImageServer(
                List.of(Path.of("/some/file[234.2344,587].tiff").toUri()),
                2.5,
                10
        );
        int[] expectedPosition = new int[] {Math.round(234.2344f / 2.5f), Math.round(587f / 10f)};
        PositionFinder positionFinder = new PathPositionFinder();

        int[] position = positionFinder.findPosition(server);

        Assertions.assertArrayEquals(expectedPosition, position);

        server.close();
    }

    @Test
    void Check_Position_With_Two_Uris() throws Exception {
        SampleImageServer server = new SampleImageServer(List.of(
                Path.of("/some/file[234.2344,587].tiff").toUri(),
                Path.of("/some/file[42,0.87].tiff").toUri()
        ));
        int[] expectedPosition = new int[] {234, 587};
        PositionFinder positionFinder = new PathPositionFinder();

        int[] position = positionFinder.findPosition(server);

        Assertions.assertArrayEquals(expectedPosition, position);

        server.close();
    }

    private static class SampleImageServer extends AbstractTileableImageServer {

        private final List<URI> uris;
        private final ImageServerMetadata metadata;

        public SampleImageServer(List<URI> uris) {
            this(uris, null, null);
        }

        public SampleImageServer(List<URI> uris, Number pixelWidthMicrons, Number pixelHeightMicrons) {
            this.uris = uris;

            ImageServerMetadata.Builder metadataBuilder = new ImageServerMetadata.Builder();
            if (pixelWidthMicrons != null && pixelHeightMicrons != null) {
                metadataBuilder.pixelSizeMicrons(pixelWidthMicrons, pixelHeightMicrons);
            }
            this.metadata = metadataBuilder
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
