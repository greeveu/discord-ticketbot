import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "eu.greev"
version = "1.3.1"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    implementation("net.dv8tion", "JDA", "5.6.1") {
        exclude("club.minnced", "opus-java")
        exclude("com.google.crypto.tink", "tink")
    }
    implementation("org.jdbi", "jdbi3-oracle12", "3.44.0")
    implementation("org.slf4j", "slf4j-log4j12", "2.0.17")
    implementation("org.xerial", "sqlite-jdbc", "3.50.3.0")
    implementation("org.apache.logging.log4j", "log4j-api", "2.25.1")
    implementation("org.apache.logging.log4j", "log4j-core", "2.25.1")
    implementation("me.carleslc.Simple-YAML", "Simple-Yaml", "1.8.3")
    implementation("com.fasterxml.jackson.core", "jackson-databind", "2.16.1")

    compileOnly("org.projectlombok", "lombok", "1.18.40")
    annotationProcessor("org.projectlombok", "lombok", "1.18.40")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = "eu.greev.dcbot.Main"
    }
    archiveFileName.set("discord-ticketbot.jar")
}