import qupath.ext.stitching.core.ImageStitcher

/*
 * This script will stitch input images and write the result to an output OME-Zarr image.
 */

var inputImages = [
        "/path/to/the/input/image1.tiff",
        "/path/to/the/input/image2.tiff",
        // other images...
]
var outputImage = "/path/to/the/output/image.ome.zarr"      // the path must ends with ".ome.zarr" and must not already exist

new ImageStitcher.Builder(inputImages)
    .build()
    .writeToZarrFile(outputImage, null)

println "Done"