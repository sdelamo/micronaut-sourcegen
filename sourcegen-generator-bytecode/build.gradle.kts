plugins {
    id("io.micronaut.build.internal.sourcegen-module")
}

dependencies {
    api(projects.sourcegenGenerator)
    api(projects.sourcegenBytecodeWriter)
    testImplementation(libs.junit.jupiter.engine)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
