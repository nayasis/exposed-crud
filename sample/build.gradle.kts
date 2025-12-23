plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.ksp)
    alias(libs.plugins.serial)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    implementation(libs.exposed.dao)
    implementation(libs.exposed.kotlin.datetime)
    implementation(libs.exposed.java.time)
    implementation(libs.exposed.json)
    implementation(libs.serial)
    implementation(project(":annotations"))
    ksp(project(":processor"))
    testImplementation(kotlin("test"))
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testRuntimeOnly("org.xerial:sqlite-jdbc:3.44.1.0")
    testRuntimeOnly("com.h2database:h2:2.2.224")
}