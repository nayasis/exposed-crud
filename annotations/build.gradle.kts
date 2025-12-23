import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = "com.dshatz.exposed-crud"
version = when {
    project.hasProperty("version") && project.property("version").let { it != "" && it != "unspecified" } -> {
        project.property("version") as String
    }
    else -> "0.1.0-SNAPSHOT"
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.exposed.core)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    if(listOf("publishToMavenLocal","publishMavenPublicationToMavenLocal").none{gradle.startParameter.taskNames.contains(it)}) {
        signAllPublications()
    }
    coordinates(project.group.toString(), "lib")
    pom {
        name = "Exposed-CRUD"
        description = "Exposed CRUD repository generator."
        inceptionYear = "2025"
        url = "https://github.com/dshatz/exposed-crud/"
        licenses {
            license {
                name = "GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007"
                url = "https://github.com/dshatz/openapi2ktor/blob/main/LICENSE"
            }
        }
        developers {
            developer {
                id = "dshatz"
                name = "Daniels Šatcs"
                email = "dev@dshatz.com"
            }
        }
        scm {
            url = "https://github.com/dshatz/exposed-crud"
            connection = "git@github.com:dshatz/exposed-crud.git"
        }
    }
}
