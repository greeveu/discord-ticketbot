import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "de.jjjannik"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.0-alpha.19")
    implementation("org.jdbi:jdbi3-oracle12:3.32.0")
    implementation("org.slf4j:slf4j-log4j12:2.0.0")
    implementation("org.xerial:sqlite-jdbc:3.39.3.0")
    implementation("org.apache.logging.log4j:log4j-api:2.18.0")
    implementation("org.apache.logging.log4j:log4j-core:2.18.0")
    implementation("me.carleslc.Simple-YAML:Simple-Yaml:1.8")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<ShadowJar> {
    manifest {
        attributes["Main-Class"] = "eu.greev.dcbot.Main"
    }
}