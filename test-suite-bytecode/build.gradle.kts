plugins {
    id("io.micronaut.build.internal.sourcegen-testsuite")
}

dependencies {
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(projects.sourcegenGeneratorBytecode)
    annotationProcessor(projects.testSuiteCustomGenerators)

    implementation(projects.sourcegenAnnotations)
    implementation(projects.testSuiteCustomAnnotations)

    testAnnotationProcessor(mn.micronaut.inject.java)

    testImplementation(mnTest.micronaut.test.junit5)

    testRuntimeOnly(mnTest.junit.jupiter.engine)
}
//
//tasks {
//    compileJava {
//        options.isFork = true
//        options.forkOptions.jvmArgs = listOf("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
//    }
//}
