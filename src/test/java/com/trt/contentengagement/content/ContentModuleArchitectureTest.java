package com.trt.contentengagement.content;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.trt.contentengagement",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class ContentModuleArchitectureTest {

    @ArchTest
    static final ArchRule CONTENT_DOMAIN_STAYS_FRAMEWORK_INDEPENDENT = noClasses()
            .that().resideInAPackage("..content.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.servlet.."
            );

    @ArchTest
    static final ArchRule CONTENT_DOES_NOT_USE_IDENTITY_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage("..content..")
            .should().dependOnClassesThat().resideInAPackage("..identity.infrastructure..");
}
