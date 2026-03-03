plugins {
    kotlin("jvm") version "2.3.10"
    id("com.gradleup.shadow") version "9.3.2"
}

group = "dev.coughlin"
version = "1.0.0-beta.1"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.2.1")

    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.14.9")
}

tasks {
    shadowJar {
        archiveClassifier.set("")
        relocate("org.bstats", "dev.coughlin.deathban.metrics.bstats")

        // Exclude unused Kotlin packages to reduce JAR size
        // Note: Keep kotlin/enums (needed for enum support)
        exclude("kotlin/coroutines/**")
        exclude("kotlin/streams/**")
        exclude("kotlin/js/**")
        exclude("kotlin/time/**")
        exclude("kotlin/random/**")
        exclude("kotlin/concurrent/**")
        exclude("kotlin/contracts/**")
        exclude("kotlin/experimental/**")
        exclude("kotlin/properties/**")
        exclude("kotlin/system/**")
        exclude("kotlin/math/**")
        exclude("kotlin/sequences/**")
        exclude("kotlin/io/**")
        exclude("kotlin/reflect/**")
        exclude("kotlin/jdk7/**")
        exclude("DebugProbesKt.bin")
        exclude("META-INF/versions/**")
        exclude("META-INF/*.kotlin_module")
        exclude("META-INF/maven/**")

        minimize {
            // Keep bStats classes as they're loaded via reflection
            exclude(dependency("org.bstats:.*"))
        }
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to version)
        }
    }

    test {
        useJUnitPlatform()
    }
}

kotlin {
    jvmToolchain(25)
}
