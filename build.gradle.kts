import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.4.21"
    id("com.gradleup.shadow") version "9.6.1"
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0"
    jacoco
}

group = "dev.coughlin"
version = providers.gradleProperty("version").getOrElse("2.0.0-alpha.1")

// Paper API 26.3 build 237 is the newest stable build that includes builds 228, 229, and 233.
// This plugin has no NMS, so it does not depend on the paperweight dev-bundle.
val paperApi = "26.3.build.237-stable"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

// Isolate detekt's Kotlin 2.4.10 parser from the project's Kotlin 2.4.20 compiler.
val detektCli =
    configurations.create("detektCli") {
        isCanBeConsumed = false
        isTransitive = false
    }
val detektJavaToolchains = extensions.getByType<org.gradle.jvm.toolchain.JavaToolchainService>()

// Java 21 guard compiled into the JAR root. Java 25 loads the real classes from META-INF/versions/25.
val jvm21Stub =
    sourceSets.create("jvm21Stub") {
        java.setSrcDirs(listOf("src/jvm21Stub/java"))
    }

// Resolved as Java 25 so the guard can be compiled with --release 21 against Paper's Java 25 API jar.
val paperForStub =
    configurations.create("paperForStub") {
        isCanBeConsumed = false
        isCanBeResolved = true
    }

dependencies {
    add(detektCli.name, "dev.detekt:detekt-cli:2.0.0-alpha.6:all")
    add(paperForStub.name, "io.papermc.paper:paper-api:$paperApi")
    compileOnly("io.papermc.paper:paper-api:$paperApi")
    compileOnly("me.clip:placeholderapi:2.12.3")
    testImplementation("me.clip:placeholderapi:2.12.3")
    implementation("org.bstats:bstats-bukkit:3.2.1")

    testImplementation("io.papermc.paper:paper-api:$paperApi")
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
            javaLauncher.set(detektJavaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
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

    named<JavaCompile>("compileJvm21StubJava") {
        classpath = paperForStub
        options.release.set(21)
    }

    shadowJar {
        archiveBaseName.set("DeathBanPro")
        archiveClassifier.set("")
        relocate("org.bstats", "dev.coughlin.deathban.metrics.bstats")
        dependsOn(jvm21Stub.classesTaskName)
        manifest {
            attributes("Multi-Release" to "true")
        }
        from(jvm21Stub.output) {
            include("dev/coughlin/deathban/DeathBanPlugin.class")
        }
        eachFile {
            val sourcePath = file.invariantSeparatorsPath
            if (
                path.startsWith("dev/coughlin/deathban/") &&
                !path.startsWith("dev/coughlin/deathban/metrics/") &&
                !sourcePath.contains("/jvm21Stub/")
            ) {
                path = "META-INF/versions/25/$path"
            }
        }

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

    // Gradle 9.8.1 runs on Java 25. Keep this aligned with gradle-wrapper.properties.
    wrapper {
        version = "9.8.1"
        distributionType = Wrapper.DistributionType.BIN
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

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_25)
    }
}
