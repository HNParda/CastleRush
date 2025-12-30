plugins {
    id("java")
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "com.hnp-arda.castlerush"
version = "2.2.0"

repositories {
    mavenCentral()
}


repositories {
    maven {
        name = "papermc"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks {
    runServer {
        minecraftVersion("1.21.11")
    }
}
