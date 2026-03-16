plugins {
    // QuPath Gradle extension convention plugin
    id("qupath-conventions")
    // Benchmark plugin
    id("me.champeau.jmh") version "0.7.3"
}

qupathExtension {
    name = "qupath-extension-stitching"
    group = "io.github.qupath"
    version = "0.2.1"
    description = "An extension to combine multiple TIFF images into a single image"
    automaticModule = "io.github.qupath.extension.stitching"
}

dependencies {
    implementation(libs.bundles.qupath)
    implementation(libs.bundles.logging)
    implementation(libs.qupath.ext.bioformats)
    implementation(libs.qupath.fxtras)

    testImplementation(libs.junit)
    testImplementation(libs.junit.platform)
}

repositories {
    maven {
        name = "ome.maven"
        url = uri("https://artifacts.openmicroscopy.org/artifactory/maven")
    }
}
