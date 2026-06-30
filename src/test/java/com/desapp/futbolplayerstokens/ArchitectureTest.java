package com.desapp.futbolplayerstokens;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

@AnalyzeClasses(packages = "com.desapp.futbolplayerstokens",
        importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule entities_should_only_depend_on_java_and_lombok =
            classes().that().resideInAPackage("..modelo..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "..modelo..",
                            "java..",
                            "jakarta..",
                            "org.slf4j..",
                            "com.fasterxml.jackson..",
                            "lombok..",
                            "..exception.."
                    );

    @ArchTest
    static final ArchRule repositories_should_only_depend_on_entities_and_spring =
            classes().that().resideInAPackage("..repository..")
                    .should().onlyDependOnClassesThat()
                    .resideInAnyPackage(
                            "..repository..",
                            "..modelo..",
                            "org.springframework..",
                            "java..",
                            "jakarta.."
                    );

    @ArchTest
    static final ArchRule core_layers_should_be_free_of_cycles =
            slices().matching("com.desapp.futbolplayerstokens.(service|repository|modelo|security)..")
                    .should().beFreeOfCycles();
}
