plugins {
    kotlin("jvm") version "2.4.10"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "dev.mznu.maintenance"
version = providers.gradleProperty("buildVersion").getOrElse("dev")
description = "A maintenance mode plugin for Paper."

repositories {
    mavenCentral()

    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    val paperApi = "io.papermc.paper:paper-api:26.2.build.119-stable"

    compileOnly(paperApi)

    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testImplementation(paperApi)
}

kotlin {
    jvmToolchain(25)
}

tasks {
    processResources {
        val properties = mapOf(
            "version" to project.version,
            "description" to project.description,
        )

        inputs.properties(properties)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(properties)
        }
    }

    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }

    test {
        useJUnitPlatform()
    }
}
