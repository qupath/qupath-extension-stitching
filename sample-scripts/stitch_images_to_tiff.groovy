import qupath.ext.stitching.core.ImageStitcher
import qupath.ext.stitching.core.positionfinders.TiffTagPositionFinder
import qupath.ext.stitching.core.positionfinders.FilenamePatternPositionFinder

/*
 * This script will stitch input images and write the result to an output OME-TIFF image.
 */

var inputImages = [
        "/path/to/the/input/image1.tiff",
        "/path/to/the/input/image2.tiff",
        // other images...
]
var outputImage = "/path/to/the/output/image.ome.tiff"      // the path must ends with ".ome.tiff" and must not already exist
var positionFinders = [
        new FilenamePatternPositionFinder(FilenamePatternPositionFinder.StandardPattern.VECTRA),
        new TiffTagPositionFinder()
]     // a list of strategies for where to find each tile position. FilenamePatternPositionFinder looks at the image name, while TiffTagPositionFinder looks at the TIFF tags of the image
var numberOfThreads = Runtime.getRuntime().availableProcessors()    // the number of threads to use when reading and writing files
var pyramidalize = true                                         // whether to create a pyramidal image

new ImageStitcher.Builder(inputImages)
    .positionFinders(positionFinders)
    .numberOfThreads(numberOfThreads)
    .pyramidalize(pyramidalize)
    .build()
    .writeToTiffFile(outputImage)

println "Done"