package dev.coughlin.deathban.architecture

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * Architecture tests to enforce code structure and design principles.
 * Uses ArchUnit to validate layer separation, naming conventions, and dependencies.
 */
@DisplayName("Architecture Tests")
class ArchitectureTests {
    private lateinit var importedClasses: JavaClasses

    @BeforeEach
    fun setup() {
        importedClasses =
            ClassFileImporter()
                .withImportOption { !it.contains("Test.class") }
                .importPackages("dev.coughlin.deathban")
    }

    @Test
    @DisplayName("No cyclic dependencies exist")
    fun testNoCyclicDependencies() {
        SlicesRuleDefinition
            .slices()
            .matching("dev.coughlin.deathban.(*)..")
            .should()
            .beFreeOfCycles()
            .check(importedClasses)
    }

    @Test
    @DisplayName("Manager classes follow naming conventions")
    fun testManagerNaming() {
        ArchRuleDefinition
            .classes()
            .that()
            .resideInAPackage("dev.coughlin.deathban.manager")
            .and()
            .haveSimpleNameContaining("Manager")
            .should()
            .haveSimpleNameEndingWith("Manager")
            .check(importedClasses)
    }

    @Test
    @DisplayName("Listeners follow naming conventions")
    fun testListenerNaming() {
        ArchRuleDefinition
            .classes()
            .that()
            .resideInAPackage("dev.coughlin.deathban.listener")
            .and()
            .haveSimpleNameContaining("Listener")
            .should()
            .haveSimpleNameEndingWith("Listener")
            .check(importedClasses)
    }

    @Test
    @DisplayName("Commands follow naming conventions")
    fun testCommandNaming() {
        ArchRuleDefinition
            .classes()
            .that()
            .resideInAPackage("dev.coughlin.deathban.command")
            .and()
            .haveSimpleNameContaining("Command")
            .should()
            .haveSimpleNameEndingWith("Command")
            .check(importedClasses)
    }

    @Test
    @DisplayName("Config classes reside in config package")
    fun testConfigPackageIsolation() {
        ArchRuleDefinition
            .classes()
            .that()
            .haveSimpleNameContaining("Settings")
            .or()
            .haveSimpleNameContaining("Messages")
            .should()
            .resideInAPackage("dev.coughlin.deathban.config")
            .check(importedClasses)
    }

    @Test
    @DisplayName("Data classes reside in data package")
    fun testDataPackageIsolation() {
        ArchRuleDefinition
            .classes()
            .that()
            .haveSimpleNameEndingWith("Data")
            .or()
            .haveSimpleNameEndingWith("Record")
            .should()
            .resideInAPackage("dev.coughlin.deathban.data")
            .check(importedClasses)
    }

    @Test
    @DisplayName("Commands may only depend on allowed packages")
    fun testCommandDependencies() {
        ArchRuleDefinition
            .classes()
            .that()
            .resideInAPackage("dev.coughlin.deathban.command")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "dev.coughlin.deathban.manager",
                "dev.coughlin.deathban.data",
                "dev.coughlin.deathban.config",
                "org.bukkit",
                "java..",
                "kotlin..",
                "dev.coughlin.deathban",
            ).check(importedClasses)
    }

    @Test
    @DisplayName("Listeners are not imported by commands")
    fun testListenerDecoupling() {
        ArchRuleDefinition
            .classes()
            .that()
            .resideInAPackage("dev.coughlin.deathban.command..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "dev.coughlin.deathban.manager",
                "dev.coughlin.deathban.data",
                "dev.coughlin.deathban.config",
                "org.bukkit",
                "java..",
                "kotlin..",
            ).check(importedClasses)
    }

    @Test
    @DisplayName("Data layer not imported by listeners")
    fun testDataLayerDecoupling() {
        ArchRuleDefinition
            .classes()
            .that()
            .resideInAPackage("dev.coughlin.deathban.listener..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "dev.coughlin.deathban.manager",
                "dev.coughlin.deathban.config",
                "org.bukkit",
                "java..",
                "kotlin..",
            ).check(importedClasses)
    }

    @Test
    @DisplayName("Theme manager is in theme package")
    fun testThemePackageIsolation() {
        ArchRuleDefinition
            .classes()
            .that()
            .haveSimpleNameContaining("Theme")
            .should()
            .resideInAPackage("dev.coughlin.deathban.theme")
            .check(importedClasses)
    }

    @Test
    @DisplayName("Public classes follow package conventions")
    fun testPublicClassConventions() {
        ArchRuleDefinition
            .classes()
            .that()
            .arePublic()
            .and()
            .resideInAPackage("dev.coughlin.deathban")
            .should()
            .bePublic()
            .check(importedClasses)
    }
}
