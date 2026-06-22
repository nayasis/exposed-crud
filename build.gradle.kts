plugins {
    alias(libs.plugins.jvm) apply false
}

group = "io.github.nayasis"
version = when {
    project.hasProperty("mavenReleaseVersion") && project.property("mavenReleaseVersion").let { it != "" && it != "unspecified" } -> {
        project.property("mavenReleaseVersion") as String
    }
    else -> "0.1.0-SNAPSHOT"
}
