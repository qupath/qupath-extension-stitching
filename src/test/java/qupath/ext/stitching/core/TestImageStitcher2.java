package qupath.ext.stitching.core;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class TestImageStitcher2 {

    @Test
    void check() throws IOException, InterruptedException {
//        String path = "/Users/lleplat/t.ome.zarr";
//
//        deleteDirectoryRecursively(new File(path));
//
//        new ImageStitcher.Builder(List.of(
//                "/Users/lleplat/QuPath/Images/Stitching Vectra Polaris Datasets/1757621_Scan1/1757621_Scan1_[10017,57934]_component_data.tif",
//                "/Users/lleplat/QuPath/Images/Stitching Vectra Polaris Datasets/1757621_Scan1/1757621_Scan1_[10017,58277]_component_data.tif",
//                "/Users/lleplat/QuPath/Images/Stitching Vectra Polaris Datasets/1757621_Scan1/1757621_Scan1_[10017,58620]_component_data.tif",
//                "/Users/lleplat/QuPath/Images/Stitching Vectra Polaris Datasets/1757621_Scan1/1757621_Scan1_[10017,58962]_component_data.tif",
//                "/Users/lleplat/QuPath/Images/Stitching Vectra Polaris Datasets/1757621_Scan1/1757621_Scan1_[10017,59305]_component_data.tif",
//                "/Users/lleplat/QuPath/Images/Stitching Vectra Polaris Datasets/1757621_Scan1/1757621_Scan1_[10017,59647]_component_data.tif",
//                "/Users/lleplat/QuPath/Images/Stitching Vectra Polaris Datasets/1757621_Scan1/1757621_Scan1_[10017,59990]_component_data.tif",
//                "/Users/lleplat/QuPath/Images/Stitching Vectra Polaris Datasets/1757621_Scan1/1757621_Scan1_[10017,60333]_component_data.tif",
//                "/Users/lleplat/QuPath/Images/Stitching Vectra Polaris Datasets/1757621_Scan1/1757621_Scan1_[10017,60675]_component_data.tif",
//                "/Users/lleplat/QuPath/Images/Stitching Vectra Polaris Datasets/1757621_Scan1/1757621_Scan1_[10017,61018]_component_data.tif"
//        )).build().writeToZarrFile(path, null);
    }

    private static void deleteDirectoryRecursively(File directoryToBeDeleted) throws IOException {
        File[] childFiles = directoryToBeDeleted.listFiles();
        if (childFiles != null) {
            for (File file : childFiles) {
                deleteDirectoryRecursively(file);
            }
        }

        Files.deleteIfExists(directoryToBeDeleted.toPath());
    }
}
