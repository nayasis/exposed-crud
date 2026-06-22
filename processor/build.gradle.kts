import com.vanniktech.maven.publish.SonatypeHost

plugins {
    alias(libs.plugins.jvm)
    alias(libs.plugins.vanniktech.mavenPublish)
}

group = rootProject.group
version = rootProject.version

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.interop)
    implementation(libs.exposed.core)
    implementation(libs.exposed.dao)
    implementation(project(":annotations"))
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    if(listOf("publishToMavenLocal","publishMavenPublicationToMavenLocal").none{gradle.startParameter.taskNames.contains(it)}) {
        signAllPublications()
    }
    coordinates(project.group.toString(), "exposed-crud-processor")
    pom {
        name = "Exposed-CRUD"
        description = "Exposed CRUD repository generator."
        inceptionYear = "2025"
        url = "https://github.com/nayasis/exposed-crud/"
        licenses {
            license {
                name = "GNU GENERAL PUBLIC LICENSE Version 3, 29 June 2007"
                url = "https://github.com/nayasis/exposed-crud/blob/main/LICENSE"
            }
        }
        developers {
            developer {
                id = "nayasis"
                name = "nayasis"
                email = "nayasis@gmail.com"
            }
        }
        scm {
            url = "https://github.com/nayasis/exposed-crud"
            connection = "scm:git:github.com/nayasis/exposed-crud.git"
            developerConnection = "scm:git:ssh://github.com/nayasis/exposed-crud.git"
        }
    }
}
