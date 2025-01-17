plugins {
    id("io.micronaut.build.internal.sourcegen-module")
}

micronautBuild {
    binaryCompatibility {
        enabled.set(false)
    }
}
