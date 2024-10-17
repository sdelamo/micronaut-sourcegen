plugins {
    id("io.micronaut.build.internal.sourcegen-module")
}

dependencies {
    compileOnly(mn.micronaut.core.processor)
}
