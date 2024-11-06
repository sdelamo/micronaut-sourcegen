plugins {
    id("io.micronaut.build.internal.sourcegen-testsuite")
    id("io.micronaut.minimal.application")
}

micronaut {
    processing {
        // test incremental compile
        incremental(true)
    }
    version.set(libs.versions.micronaut.platform)
}
dependencies {
    annotationProcessor(projects.sourcegenGeneratorJava)
    annotationProcessor(projects.testSuiteCustomGenerators)
    annotationProcessor(mnData.micronaut.data.processor)
    annotationProcessor(mnValidation.micronaut.validation.processor)
    implementation(mnValidation.micronaut.validation)
    implementation(projects.sourcegenAnnotations)    
    implementation(projects.testSuiteCustomAnnotations)
    implementation(mnData.micronaut.data.model)
    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
    testAnnotationProcessor(mn.micronaut.inject.java.test)
}
//
//tasks {
//    compileJava {
//        options.isFork = true
//        options.forkOptions.jvmArgs = listOf("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
//    }
//}

