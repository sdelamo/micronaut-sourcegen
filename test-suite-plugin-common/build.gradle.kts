plugins {
    id("io.micronaut.build.internal.sourcegen-testsuite")
}

dependencies {
    annotationProcessor(mn.micronaut.inject)
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(projects.sourcegenGeneratorJava)
    annotationProcessor(projects.sourcegenPluginGenerator)

    implementation(projects.sourcegenPluginAnnotations)
}
