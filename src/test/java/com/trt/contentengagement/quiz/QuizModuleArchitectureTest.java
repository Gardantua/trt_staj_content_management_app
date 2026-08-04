package com.trt.contentengagement.quiz;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.trt.contentengagement",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class QuizModuleArchitectureTest {

    @ArchTest
    static final ArchRule QUIZ_DOMAIN_STAYS_FRAMEWORK_INDEPENDENT = noClasses()
            .that().resideInAPackage("..quiz.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..",
                    "jakarta.persistence..",
                    "jakarta.servlet.."
            );

    @ArchTest
    static final ArchRule QUIZ_DOES_NOT_USE_OTHER_MODULE_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage("..quiz..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..content.infrastructure..",
                    "..identity.infrastructure..",
                    "..admin.infrastructure.."
            );
}
