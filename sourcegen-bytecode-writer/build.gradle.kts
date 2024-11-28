plugins {
    id("io.micronaut.build.internal.sourcegen-module")
}

repositories {
    maven(
        url = uri("https://www.jetbrains.com/intellij-repository/releases/")
    )
}

dependencies {
    api(projects.sourcegenModel)
    compileOnly(mn.micronaut.core.processor)
    api(libs.asm)
    api(libs.asm.commons)
    api(libs.asm.util)
    testImplementation(mn.micronaut.core.processor)
    testImplementation(libs.junit.jupiter.engine)
    testImplementation(libs.intellij.java.decompiler)
    testImplementation(projects.testSuiteCustomGenerators)
}

tasks.withType<Test> {
    useJUnitPlatform()
}
