plugins {
    id("java")
}

group = "com.hnp-arda.castlerush"
version = "2.0.0"

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
    compileOnly("io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.13.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.register<Copy>("copy") {
    dependsOn("build")
    from(layout.buildDirectory.dir("libs"))
    into("C:/Users/Administrator/Desktop/Server/plugins")
    include("*.jar").doNotTrackState("a")
}

tasks.register<Exec>("start") {
    workingDir = file("C:/Users/Administrator/Desktop/Server")
    commandLine("C:/Users/Administrator/Desktop/Server/start.bat")
    dependsOn("copy")
}


tasks.register<Copy>("copy2") {
    dependsOn("build")
    from(layout.buildDirectory.dir("libs"))
    into("C:/Users/ArdaM/Desktop/pluginserver/plugins")
    include("*.jar").doNotTrackState("a")
}

tasks.register<Exec>("start2") {
    workingDir = file("C:/Users/ArdaM/Desktop/pluginserver")
    commandLine("C:/Users/ArdaM/Desktop/pluginserver/start.bat")
    dependsOn("copy2")
}
