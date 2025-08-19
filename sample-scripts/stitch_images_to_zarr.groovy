import qupath.ext.stitching.core.ImageStitcher

/*
 * This script will stitch input images and write the result to an output OME-Zarr image.
 */

var inputImages = [
        "/path/to/the/input/image1.tiff",
        "/path/to/the/input/image2.tiff",
        // other images...
]
var outputImage = "/path/to/the/output/image.ome.zarr"          // the path must ends with ".ome.zarr" and must not already exist
var imageSource = ImageStitcher.ImageSource.VECTRA_3                   // the type of the input images. Can also be ImageStitcher.ImageSource.VECTRA_2
var numberOfThreads = Runtime.getRuntime().availableProcessors()  // the number of threads to use when reading and writing files
var pyramidalize = true                                       // whether to create a pyramidal image

new ImageStitcher.Builder(inputImages)
    .imageSource(imageSource)
    .numberOfThreads(numberOfThreads)
    .pyramidalize(pyramidalize)
    .build()
    .writeToZarrFile(outputImage, null)

println "Done"