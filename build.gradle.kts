plugins {
    kotlin("jvm") version "2.4.0"
    id("com.gradleup.shadow") version "9.4.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    jacoco
}

group = "dev.coughlin"
version = "1.0.0-beta.1"

repositories {
    mavenCentral()
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

dependencies {
    compileOnly("org.spigotmc:spigot-api:26.1.2-R0.1-SNAPSHOT")
    implementation("org.bstats:bstats-bukkit:3.2.1")

    testImplementation("org.spigotmc:spigot-api:26.1.2-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.4.2")
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
        finalizedBy(jacocoTestReport)
    }

    jacocoTestReport {
        dependsOn(test)
        reports {
            xml.required.set(true)
            html.required.set(true)
        }
    }

    // Build metadata task
    register("buildMetadata") {
        description = "Capture build metadata"
        doLast {
            val buildDir = layout.buildDirectory.get().asFile
            buildDir.mkdirs()

            val timestamp = System.currentTimeMillis()
            val gitSha =
                try {
                    ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                        .directory(rootProject.projectDir)
                        .redirectError(ProcessBuilder.Redirect.DISCARD)
                        .start()
                        .inputStream
                        .bufferedReader()
                        .readText()
                        .trim()
                } catch (e: Exception) {
                    "unknown"
                }

            val metadata =
                """
                Build-Time: $timestamp
                Git-SHA: $gitSha
                Version: $version
                """.trimIndent()

            file("$buildDir/build-metadata.txt").writeText(metadata)
            println("Build metadata captured:")
            println(metadata)
        }
    }

    // Test summary report task
    register("testSummary") {
        description = "Generate test summary report"
        dependsOn("test")
        doLast {
            val resultsDir =
                layout
                    .buildDirectory
                    .get()
                    .asFile
                    .resolve("test-results/test")
            val reportDir =
                layout
                    .buildDirectory
                    .get()
                    .asFile
                    .resolve("test-reports")
            reportDir.mkdirs()

            if (resultsDir.exists()) {
                val xmlFiles =
                    resultsDir.listFiles { file ->
                        file.isFile && file.extension == "xml"
                    } ?: emptyArray()

                var totalTests = 0
                var totalPassed = 0
                var totalFailed = 0
                var totalSkipped = 0

                xmlFiles.forEach { xmlFile ->
                    val content = xmlFile.readText()
                    val testCount =
                        "tests=\"(\\d+)\""
                            .toRegex()
                            .find(content)
                            ?.groupValues
                            ?.get(1)
                            ?.toIntOrNull() ?: 0
                    val failureCount =
                        "failures=\"(\\d+)\""
                            .toRegex()
                            .find(content)
                            ?.groupValues
                            ?.get(1)
                            ?.toIntOrNull() ?: 0
                    val skippedCount =
                        "skipped=\"(\\d+)\""
                            .toRegex()
                            .find(content)
                            ?.groupValues
                            ?.get(1)
                            ?.toIntOrNull() ?: 0

                    totalTests += testCount
                    totalFailed += failureCount
                    totalSkipped += skippedCount
                    totalPassed += (testCount - failureCount - skippedCount)
                }

                val summary =
                    """
                    ================== TEST SUMMARY ==================
                    Total Tests:  $totalTests
                    Passed:       $totalPassed
                    Failed:       $totalFailed
                    Skipped:      $totalSkipped
                    ==================================================
                    """.trimIndent()

                println(summary)
                reportDir.resolve("test-summary.txt").writeText(summary)
            }
        }
    }

    // Gradle wrapper verification
    wrapper {
        version = "9.5.0"
        distributionType = Wrapper.DistributionType.ALL
        validateDistributionUrl = true
    }

    // Add metadata to shadowJar manifest
    register<Jar>("jarWithMetadata") {
        description = "Create JAR with build metadata"
        dependsOn("buildMetadata")
        from(
            layout
                .buildDirectory
                .get()
                .asFile
                .resolve("build-metadata.txt"),
        )
    }
}

kotlin {
    jvmToolchain(21)
}
