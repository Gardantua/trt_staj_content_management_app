package com.trt.contentengagement.gameplay;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.trt.contentengagement", importOptions = ImportOption.DoNotIncludeTests.class)
class GameplayModuleArchitectureTest {
    @ArchTest static final ArchRule GAMEPLAY_DOMAIN_STAYS_FRAMEWORK_INDEPENDENT = noClasses()
            .that().resideInAPackage("..gameplay.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.servlet.."
            );
    @ArchTest static final ArchRule GAMEPLAY_DOES_NOT_USE_OTHER_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage("..gameplay..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..quiz.infrastructure..", "..identity.infrastructure.."
            );
}
