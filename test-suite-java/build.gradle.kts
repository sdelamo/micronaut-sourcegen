plugins {
    id("io.micronaut.build.internal.sourcegen-testsuite")
}

dependencies {
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(projects.sourcegenGeneratorJava)
    annotationProcessor(projects.testSuiteCustomGenerators)

    implementation(projects.sourcegenAnnotations)
    implementation(projects.testSuiteCustomAnnotations)
    implementation(mnValidation.validation)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
    testAnnotationProcessor(mn.micronaut.inject.java)

    testAnnotationProcessor(mnValidation.micronaut.validation.processor)
    testImplementation(mnValidation.micronaut.validation)
}
//
//tasks {
//    compileJava {
//        options.isFork = true
//        options.forkOptions.jvmArgs = listOf("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
//    }
//}
