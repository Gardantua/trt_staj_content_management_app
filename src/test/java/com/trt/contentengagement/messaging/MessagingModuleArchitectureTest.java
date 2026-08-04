package com.trt.contentengagement.messaging;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.trt.contentengagement",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class MessagingModuleArchitectureTest {
    @ArchTest
    static final ArchRule APPLICATION_DOES_NOT_DEPEND_ON_RABBIT_OR_JDBC = noClasses()
            .that().resideInAPackage("..messaging.application..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework.amqp..",
                    "org.springframework.jdbc..",
                    "..messaging.infrastructure.."
            );

    @ArchTest
    static final ArchRule GAMEPLAY_DOES_NOT_DEPEND_ON_MESSAGING_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage("..gameplay..")
            .should().dependOnClassesThat().resideInAPackage("..messaging.infrastructure..");
}
