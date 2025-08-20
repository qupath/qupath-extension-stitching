package qupath.ext.stitching.core;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;

/**
 * A class that checks whether a file represents a TIFF file. This is done by looking at the first four bytes of the
 * provided file, which must match: [0x49, 0x49, 0x2A, 0x00] or [0x4D, 0x4D, 0x00, 0x2A].
 */
public class TiffFileChecker {

    private TiffFileChecker() {
        throw new AssertionError("This class is not instantiable");
    }

    /**
     * Check if the provided path points to a TIFF file. Take a look at the class documentation for more information.
     *
     * @param path the path pointing to the file to test
     * @throws IOException if the path doesn't point to an existing file or if the file cannot be read
     * @throws IllegalArgumentException if the provided file is not a TIFF file
     * @throws NullPointerException if the provided path is null
     */
    public static void checkTiffFile(String path) throws IOException {
        byte[] bytes;
        try (FileInputStream inputStream = new FileInputStream(Objects.requireNonNull(path))) {
            bytes = inputStream.readNBytes(4);
        }

        if (bytes.length < 4) {
            throw new IllegalArgumentException(String.format("%s contains less than 4 bytes, so it cannot be a TIFF file", path));
        }

        if (!((bytes[0] == 0x49 && bytes[1] == 0x49 && bytes[2] == 0x2A && bytes[3] == 0x00) ||
                (bytes[0] == 0x4D && bytes[1] == 0x4D && bytes[2] == 0x00 && bytes[3] == 0x2A))) {
            throw new IllegalArgumentException(String.format(
                    "The first four bytes of %s (%s) don't match the TIFF specifications",
                    path,
                    Arrays.toString(bytes)
            ));
        }
    }
}
