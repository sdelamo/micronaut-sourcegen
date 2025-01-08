plugins {
    id("io.micronaut.build.internal.sourcegen-testsuite")
    groovy
}

dependencies {
    compileOnly(mn.micronaut.inject.groovy)
    compileOnly(projects.sourcegenGenerator)
    compileOnly(projects.sourcegenGeneratorJava)

//    implementation(mn.micronaut.inject.groovy)
//    implementation(projects.sourcegenGeneratorJava)
//    implementation(projects.sourcegenGenerator)

    implementation(projects.sourcegenAnnotations)

    testImplementation(mnTest.micronaut.test.junit5)

    testRuntimeOnly(mnTest.junit.jupiter.engine)
}
