package com.trt.contentengagement.leaderboard;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(
        packages = "com.trt.contentengagement",
        importOptions = ImportOption.DoNotIncludeTests.class
)
class LeaderboardModuleArchitectureTest {

    @ArchTest
    static final ArchRule LEADERBOARD_DOMAIN_STAYS_FRAMEWORK_INDEPENDENT = noClasses()
            .that().resideInAPackage("..leaderboard.domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "org.springframework..", "jakarta.persistence..", "jakarta.servlet.."
            );

    @ArchTest
    static final ArchRule LEADERBOARD_DOES_NOT_USE_OTHER_MODULE_INFRASTRUCTURE = noClasses()
            .that().resideInAPackage("..leaderboard..")
            .should().dependOnClassesThat().resideInAnyPackage(
                    "..content.infrastructure..", "..identity.infrastructure..",
                    "..gamification.infrastructure..", "..gameplay.infrastructure..",
                    "..quiz.infrastructure..", "..messaging.infrastructure.."
            );

    @ArchTest
    static final ArchRule REDIS_STAYS_OUT_OF_APPLICATION_AND_DOMAIN = noClasses()
            .that().resideInAnyPackage(
                    "..leaderboard.application..", "..leaderboard.domain.."
            )
            .should().dependOnClassesThat().resideInAPackage(
                    "org.springframework.data.redis.."
            );
}
