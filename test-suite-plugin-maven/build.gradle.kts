plugins {
    id("maven-publish")
    id("io.micronaut.build.internal.sourcegen-testsuite")
}

repositories {
    mavenCentral()
}

dependencies {
    api(projects.testSuitePluginCommon)
    annotationProcessor(mn.micronaut.inject)
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(projects.sourcegenGeneratorJava)
    annotationProcessor(projects.sourcegenPluginGenerator)
    implementation(projects.sourcegenPluginAnnotations)

    compileOnly("org.apache.maven.plugin-tools:maven-plugin-annotations:3.9.0")
    implementation("org.apache.maven:maven-plugin-api:3.9.4")
    implementation("org.apache.maven:maven-core:3.9.4")
    testImplementation("org.apache.maven:maven-core:3.9.4")
    testImplementation("org.apache.maven.plugin-testing:maven-plugin-testing-harness:3.3.0")
    testImplementation("org.apache.maven.resolver:maven-resolver-api:1.9.6")
    testImplementation("org.apache.maven.resolver:maven-resolver-impl:1.9.6")
    testImplementation("org.apache.maven.resolver:maven-resolver-spi:1.9.6")
    testImplementation("org.apache.maven.resolver:maven-resolver-transport-wagon:1.9.6")
    testImplementation("org.codehaus.plexus:plexus-classworlds:2.6.0")


    testImplementation(mnTest.micronaut.test.junit5)
    testImplementation(libs.junit.jupiter.engine)
}

tasks.withType<Test> {
    testLogging {
        showStandardStreams = true
    }
}
