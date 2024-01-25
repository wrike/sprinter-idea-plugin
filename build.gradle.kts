plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "1.9.0"
    id("org.jetbrains.intellij") version "1.14.1"
}

group = "com.wrike"
version = "1.3.2"

repositories {
    mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
    version.set("2023.3")
    type.set("IU") // Target IDE Platform

    plugins.set(listOf(
        "com.intellij.java",
        "TestNG-J",
        "JUnit",
        "org.jetbrains.kotlin",
    ))
}

tasks {
    patchPluginXml {
        sinceBuild.set("233")
        version.set("${project.version}")
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }

    runIde {
        this.maxHeapSize = "1024m"
    }
}
