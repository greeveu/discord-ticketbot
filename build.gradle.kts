import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "de.jjjannik"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.dv8tion:JDA:5.0.0-alpha.17")
    implementation("com.zaxxer:HikariCP:5.0.1")
    implementation("org.slf4j:slf4j-log4j12:2.0.0-alpha7")
    implementation("org.xerial:sqlite-jdbc:3.39.2.0")
}

tasks.jar {
    manifest.attributes["Main-Class"] = "eu.greev.dcbot.Main"
}

tasks.withType()

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<ShadowJar>() {
    manifest {
        attributes["Main-Class"] = "eu.greev.dcbot.Main"
    }
    //   ./gradlew shadowJar
}