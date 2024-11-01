plugins {
    id("io.micronaut.build.internal.sourcegen-module")
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    implementation(projects.sourcegenGenerator)
    implementation(libs.managed.kotlinpoet)
    implementation(libs.managed.kotlinpoet.javapoet)

    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
}

tasks.withType(Test::class).configureEach {
    useJUnit()
    predictiveSelection {
        enabled = false
    }
}
