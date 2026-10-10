plugins {
    kotlin("jvm") version "2.4.20"
    id("com.gradleup.shadow") version "9.6.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    jacoco
}

group = "dev.coughlin"
version = providers.gradleProperty("version").getOrElse("1.3.0-alpha.1")

repositories {
    mavenCentral()
    maven("https://repo.extendedclip.com/releases/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots/")
}

// Isolate detekt's Kotlin 2.4.10 parser from the project's Kotlin 2.4.20 compiler.
val detektCli =
    configurations.create("detektCli") {
        isCanBeConsumed = false
        isTransitive = false
    }
val detektJavaToolchains = extensions.getByType<org.gradle.jvm.toolchain.JavaToolchainService>()

dependencies {
    add(detektCli.name, "dev.detekt:detekt-cli:2.0.0-alpha.6:all")
    compileOnly("org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    testImplementation("me.clip:placeholderapi:2.11.6")
    implementation("org.bstats:bstats-bukkit:3.2.1")

    testImplementation("org.spigotmc:spigot-api:26.2-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("io.mockk:mockk:1.14.11")
    testImplementation("com.tngtech.archunit:archunit-junit5:1.5.1")
}

tasks {
    val detekt =
        register<JavaExec>("detekt") {
            group = "verification"
            description = "Run isolated detekt syntax analysis (no compiler/type resolution)"
            classpath = detektCli
            mainClass.set("dev.detekt.cli.Main")
            javaLauncher.set(detektJavaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(21)) })
            val source = providers.gradleProperty("detektInput").getOrElse("src")
            val reportDir =
                layout.buildDirectory
                    .dir("reports/detekt")
                    .get()
                    .asFile
            args(
                "--analysis-mode",
                "light",
                "--language-version",
                "2.4",
                "--input",
                file(source).absolutePath,
                "--config",
                file("detekt-config.yml").absolutePath,
                "--build-upon-default-config",
                "--base-path",
                rootDir.absolutePath,
                "--report",
                "html:${reportDir.resolve("detekt.html")}",
                "--report",
                "sarif:${reportDir.resolve("detekt.sarif")}",
                "--fail-on-severity",
                "Warning",
            )
        }

    check { dependsOn(detekt) }

    jar {
        enabled = false
    }

    shadowJar {
        archiveBaseName.set("DeathBanPro")
        archiveClassifier.set("")
        relocate("org.bstats", "dev.coughlin.deathban.metrics.bstats")

        exclude("DebugProbesKt.bin")
        exclude("META-INF/*.kotlin_module")
        exclude("META-INF/maven/**")

        minimize {
            // Keep bStats classes as they're loaded via reflection
            exclude(dependency("org.bstats:.*"))
            // Kotlin runtime helpers and external themes can reference classes
            // that static minimization cannot safely discover.
            exclude(dependency("org.jetbrains.kotlin:kotlin-stdlib.*"))
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
        dependsOn(shadowJar)
        systemProperty(
            "deathban.shadowJar",
            shadowJar
                .get()
                .archiveFile
                .get()
                .asFile.absolutePath,
        )
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
        version = "9.6.1"
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
