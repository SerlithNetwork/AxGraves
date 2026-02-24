plugins {
    java
    id("com.gradleup.shadow") version "9.2.2"
}

group = "com.artillexstudios"
version = "1.26.2"

repositories {
    mavenCentral()
    mavenLocal()
    maven("https://repo.artillex-studios.com/releases/") {
        name = "Artillex-Studios"
    }
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("org.slf4j:slf4j-api:2.0.17")

    implementation("com.artillexstudios.axapi:axapi:1.4.840")
    implementation("org.bstats:bstats-bukkit:3.1.0")
}

val targetJavaVersion = 21
java {
    val javaVersion = JavaVersion.toVersion(targetJavaVersion)
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    if (JavaVersion.current() < javaVersion) {
        toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    }
}

tasks.compileJava {
    options.encoding = "UTF-8"

    if (targetJavaVersion >= 17 || JavaVersion.current().isJava10Compatible) {
        options.release.set(targetJavaVersion)
    }
}

tasks.build {
    dependsOn("shadowJar")
}

tasks.shadowJar {
    archiveClassifier.set("")

    mapOf(
        "com.artillexstudios.axapi" to "axapi",
        "org.bstats" to "bstats",
        "revxrsal.commands" to "lamp",
    ).forEach { (key, value) ->
        relocate(key, "com.artillexstudios.axgraves.libs.$value")
    }

}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
