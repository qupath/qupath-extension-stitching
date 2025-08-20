# QuPath stitching extension

Welcome to the stitching extension for [QuPath](http://qupath.github.io)!

This extension supports stitching individual fields of view into a larger image.
This includes Vectra images where the position information is encoded in the file name or TIFF tags.

The extension is intended for QuPath v0.6 and later.
It is not compatible with earlier QuPath versions.

## Installing

To install the stitching extension, you can:
* Open the QuPath [extension manager](https://qupath.readthedocs.io/en/latest/docs/intro/extensions.html#managing-extensions-with-the-extension-manager) and install the extension from there (recommended).
* Or download the latest `qupath-extension-stitching-[version].jar` file from [releases](https://github.com/qupath/qupath-extension-stitching/releases) and drag it onto the main QuPath window.

If you haven't installed any extensions before, you'll be prompted to select a QuPath user directory.
The extension will then be copied to a location inside that directory.

You might then need to restart QuPath (but not your computer).

You can then stitch images in QuPath by clicking on `Extensions` -> `Stitch images`.
Additionally, you can use the extension in a script (see the `sample-scripts` folder of this repository).

## Building

You can build the extension using OpenJDK 21 or later with

```bash
./gradlew clean build
```

The output will be under `build/libs`.
You can drag the jar file on top of QuPath to install the extension.

## Running tests

You can run the tests with

```bash
./gradlew test
```

## Running benchmarks

You can run the benchmarks with

```bash
./gradlew jmh
```