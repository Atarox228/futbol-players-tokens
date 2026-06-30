package com.desapp.futbolplayerstokens.architecture;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaConstructor;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static org.assertj.core.api.Assertions.assertThat;

@AnalyzeClasses(packages = ArchitectureTest.ROOT_PACKAGE, importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    static final String ROOT_PACKAGE = "com.desapp.futbolplayerstokens";
    private static final Set<String> DTO_EXACT_NAMES = Set.of("ValuationContext", "ValuationResult");

    private static final List<String> COMMON_EXTERNAL_PACKAGES = List.of(
            "java..",
            "jakarta..",
            "org.springframework..",
            "org.slf4j..",
            "lombok..",
            "com.fasterxml.jackson..",
            "io.swagger.v3.oas.annotations..",
            "org.springdoc.."
    );

    private static final List<String> CONTROLLER_ALLOWED_PACKAGES = List.of(
            ROOT_PACKAGE + ".controller..",
            ROOT_PACKAGE + ".service..",
            ROOT_PACKAGE + ".controller.dto..",
            ROOT_PACKAGE + ".exception..",
            ROOT_PACKAGE + ".modelo..",
            ROOT_PACKAGE + ".repository..",
            ROOT_PACKAGE + ".security..",
            ROOT_PACKAGE + ".scheduler.."
    );

    private static final List<String> SERVICE_INTERFACE_ALLOWED_PACKAGES = List.of(
            ROOT_PACKAGE + ".service..",
            ROOT_PACKAGE + ".repository..",
            ROOT_PACKAGE + ".modelo..",
            ROOT_PACKAGE + ".controller.dto..",
            ROOT_PACKAGE + ".exception.."
    );

    private static final List<String> SERVICE_IMPL_ALLOWED_PACKAGES = List.of(
            ROOT_PACKAGE + ".service..",
            ROOT_PACKAGE + ".service.impl..",
            ROOT_PACKAGE + ".repository..",
            ROOT_PACKAGE + ".modelo..",
            ROOT_PACKAGE + ".controller.dto..",
            ROOT_PACKAGE + ".exception..",
            ROOT_PACKAGE + ".config.."
    );

    private static final List<String> REPOSITORY_ALLOWED_PACKAGES = List.of(
            ROOT_PACKAGE + ".modelo.."
    );

    private static final List<String> MODEL_ALLOWED_PACKAGES = List.of(
            ROOT_PACKAGE + ".modelo..",
            ROOT_PACKAGE + ".exception.."
    );

    @ArchTest
        static final ArchRule controllers_should_only_depend_on_allowed_packages =
            classes().that().resideInAPackage(ROOT_PACKAGE + ".controller..").should(
                onlyDependOnInternalPackages("controller dependency boundaries", CONTROLLER_ALLOWED_PACKAGES, false, Set.of(ROOT_PACKAGE + ".controller.dto")));

    @ArchTest
        static final ArchRule service_interfaces_should_only_depend_on_allowed_packages =
            classes().that().resideInAPackage(ROOT_PACKAGE + ".service..").should(
                onlyDependOnInternalPackages("service interface dependency boundaries", SERVICE_INTERFACE_ALLOWED_PACKAGES, true, Set.of()));

    @ArchTest
        static final ArchRule service_impls_should_only_depend_on_allowed_packages =
            classes().that().resideInAPackage(ROOT_PACKAGE + ".service.impl..").should(
                onlyDependOnInternalPackages("service implementation dependency boundaries", SERVICE_IMPL_ALLOWED_PACKAGES, false, Set.of()));

    @ArchTest
        static final ArchRule repositories_should_only_depend_on_model_packages =
            classes().that().resideInAPackage(ROOT_PACKAGE + ".repository..").should(
                onlyDependOnInternalPackages("repository dependency boundaries", REPOSITORY_ALLOWED_PACKAGES, true, Set.of()));

    @ArchTest
        static final ArchRule model_should_not_depend_on_upper_layers =
            classes().that().resideInAPackage(ROOT_PACKAGE + ".modelo..").should(
                onlyDependOnInternalPackages("model dependency boundaries", MODEL_ALLOWED_PACKAGES, false, Set.of()));

        @Test
        void core_layers_should_be_free_of_cycles() {
        JavaClass[] importedClasses = new ClassFileImporter()
            .withImportOption(new ImportOption.DoNotIncludeTests())
            .importPackages(ROOT_PACKAGE)
            .stream()
            .filter(thisClass -> !thisClass.getName().contains("$"))
            .toArray(JavaClass[]::new);

        Map<String, Set<String>> graph = new HashMap<>();
        for (JavaClass javaClass : importedClasses) {
            String sourceLayer = mapCoreLayer(javaClass.getPackageName());
            if (sourceLayer == null) {
            continue;
            }

            graph.computeIfAbsent(sourceLayer, key -> new LinkedHashSet<>());
            for (Dependency dependency : javaClass.getDirectDependenciesFromSelf()) {
            String targetLayer = mapCoreLayer(dependency.getTargetClass().getPackageName());
            if (targetLayer == null || targetLayer.equals(sourceLayer)) {
                continue;
            }
            graph.get(sourceLayer).add(targetLayer);
            }
        }

        Set<String> visited = new HashSet<>();
        Set<String> recursionStack = new HashSet<>();
        List<String> cycles = new ArrayList<>();

        for (String node : graph.keySet()) {
            if (detectCycle(node, graph, visited, recursionStack)) {
            cycles.add(node);
            }
        }

        assertThat(cycles).as("core layer cycle detection").isEmpty();
        }

    @ArchTest
    static final ArchRule controllers_should_follow_naming_convention =
            classes().that().resideInAPackage(ROOT_PACKAGE + ".controller..").should(haveAllowedNames(
                            "Controller naming convention",
                            Set.of("Controller", "ControllerREST"),
                    Set.of(), false,
                    Set.of(ROOT_PACKAGE + ".controller.dto")));

    @ArchTest
    static final ArchRule service_interfaces_should_follow_naming_convention =
            classes().that().resideInAPackage(ROOT_PACKAGE + ".service..").should(haveAllowedNames(
                            "Service interface naming convention",
                            Set.of("Service", "Strategy", "Router"),
                    Set.of(), true,
                    Set.of()));

    @ArchTest
    static final ArchRule service_impls_should_follow_naming_convention =
            classes().that().resideInAPackage(ROOT_PACKAGE + ".service.impl..").should(haveAllowedNames(
                            "Service implementation naming convention",
                            Set.of("Impl", "Strategy"),
                    Set.of(), false,
                    Set.of()));

    @ArchTest
    static final ArchRule repositories_should_follow_naming_convention =
            classes().that().resideInAPackage(ROOT_PACKAGE + ".repository..").should(haveAllowedNames(
                            "Repository naming convention",
                            Set.of("Repository"),
                    Set.of(), true,
                    Set.of()));

    @ArchTest
    static final ArchRule dto_classes_should_follow_naming_convention =
            classes().that().resideInAPackage(ROOT_PACKAGE + ".controller.dto..").should(haveAllowedNames(
                            "DTO naming convention",
                            Set.of("DTO", "Request", "Response"),
                    DTO_EXACT_NAMES, false,
                    Set.of()));

    @ArchTest
    static final ArchRule exceptions_should_follow_naming_convention =
            classes().that().resideInAPackage(ROOT_PACKAGE + ".exception..").should(haveAllowedNames(
                            "Exception naming convention",
                            Set.of("Exception"),
                    Set.of(), false,
                    Set.of()));

    @ArchTest
    static final ArchRule no_autowired_injection =
            classes().that().resideInAPackage(ROOT_PACKAGE + "..").should(notUseAutowiredInjection());

    @Test
    void main_sources_should_not_use_system_out_or_err_println() throws IOException {
        List<String> violations = new ArrayList<>();
        Path sourceRoot = Path.of("src/main/java");

        if (Files.exists(sourceRoot)) {
            try (var paths = Files.walk(sourceRoot)) {
                paths.filter(path -> Files.isRegularFile(path) && path.toString().endsWith(".java"))
                        .forEach(path -> {
                            try {
                                String content = Files.readString(path, StandardCharsets.UTF_8);
                                if (content.contains("System.out.println")) {
                                    violations.add(path + " contains System.out.println");
                                }
                                if (content.contains("System.err.println")) {
                                    violations.add(path + " contains System.err.println");
                                }
                            } catch (IOException ex) {
                                throw new IllegalStateException("Unable to read " + path, ex);
                            }
                        });
            }
        }

        assertThat(violations).isEmpty();
    }

    private static ArchCondition<JavaClass> haveAllowedNames(String description, Set<String> suffixes, Set<String> exactNames, boolean interfacesOnly, Set<String> excludedPackagePrefixes) {
        Set<String> suffixCopy = new HashSet<>(suffixes);
        Set<String> exactCopy = new HashSet<>(exactNames);
        Set<String> excludedPrefixes = new HashSet<>(excludedPackagePrefixes);

        return new ArchCondition<>(description) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.getName().contains("$")) {
                    return;
                }

                if (interfacesOnly && !item.isInterface()) {
                    return;
                }

                for (String excludedPrefix : excludedPrefixes) {
                    if (item.getPackageName().startsWith(excludedPrefix)) {
                        return;
                    }
                }

                String simpleName = item.getSimpleName();
                boolean matches = exactCopy.contains(simpleName);

                for (String suffix : suffixCopy) {
                    if (simpleName.endsWith(suffix)) {
                        matches = true;
                        break;
                    }
                }

                if (!matches) {
                    events.add(SimpleConditionEvent.violated(item, item.getName() + " does not follow " + description));
                }
            }
        };
    }

    private static ArchCondition<JavaClass> onlyDependOnInternalPackages(String description, List<String> allowedPackages, boolean interfacesOnly, Set<String> excludedPackagePrefixes) {
        List<String> allowedPrefixes = new ArrayList<>(allowedPackages);
        Set<String> excludedPrefixes = new HashSet<>(excludedPackagePrefixes);

        return new ArchCondition<>(description) {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                if (item.getName().contains("$")) {
                    return;
                }

                if (interfacesOnly && !item.isInterface()) {
                    return;
                }

                for (String excludedPrefix : excludedPrefixes) {
                    if (item.getPackageName().startsWith(excludedPrefix)) {
                        return;
                    }
                }

                for (Dependency dependency : item.getDirectDependenciesFromSelf()) {
                    String targetPackage = dependency.getTargetClass().getPackageName();
                    if (!targetPackage.startsWith(ROOT_PACKAGE)) {
                        continue;
                    }

                    boolean allowed = allowedPrefixes.stream().anyMatch(pattern -> {
                        String prefix = pattern.endsWith("..") ? pattern.substring(0, pattern.length() - 2) : pattern;
                        return targetPackage.equals(prefix) || targetPackage.startsWith(prefix + ".");
                    });

                    if (!allowed) {
                        events.add(SimpleConditionEvent.violated(dependency,
                                item.getName() + " depends on " + dependency.getTargetClass().getName()
                                        + " which is outside the allowed internal packages for " + description));
                    }
                }
            }
        };
    }

    private static String mapCoreLayer(String packageName) {
        if (packageName.startsWith(ROOT_PACKAGE + ".controller.dto")) {
            return "controller.dto";
        }
        if (packageName.startsWith(ROOT_PACKAGE + ".controller")) {
            return "controller";
        }
        if (packageName.startsWith(ROOT_PACKAGE + ".service.impl")) {
            return "service";
        }
        if (packageName.startsWith(ROOT_PACKAGE + ".service")) {
            return "service";
        }
        if (packageName.startsWith(ROOT_PACKAGE + ".repository")) {
            return "repository";
        }
        if (packageName.startsWith(ROOT_PACKAGE + ".modelo")) {
            return "modelo";
        }
        if (packageName.startsWith(ROOT_PACKAGE + ".security")) {
            return "security";
        }
        return null;
    }

    private static boolean detectCycle(String node, Map<String, Set<String>> graph, Set<String> visited, Set<String> recursionStack) {
        if (recursionStack.contains(node)) {
            return true;
        }
        if (visited.contains(node)) {
            return false;
        }

        visited.add(node);
        recursionStack.add(node);

        for (String neighbor : graph.getOrDefault(node, Set.of())) {
            if (detectCycle(neighbor, graph, visited, recursionStack)) {
                return true;
            }
        }

        recursionStack.remove(node);
        return false;
    }

    private static ArchCondition<JavaClass> notUseAutowiredInjection() {
        return new ArchCondition<>("not use @Autowired") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                for (JavaField field : item.getAllFields()) {
                    if (field.isAnnotatedWith(Autowired.class)) {
                        events.add(SimpleConditionEvent.violated(field, item.getName() + " field " + field.getName() + " uses @Autowired"));
                    }
                }

                for (JavaMethod method : item.getAllMethods()) {
                    if (method.isAnnotatedWith(Autowired.class)) {
                        events.add(SimpleConditionEvent.violated(method, item.getName() + " method " + method.getName() + " uses @Autowired"));
                    }
                }

                for (JavaConstructor constructor : item.getConstructors()) {
                    if (constructor.isAnnotatedWith(Autowired.class)) {
                        events.add(SimpleConditionEvent.violated(constructor, item.getName() + " constructor uses @Autowired"));
                    }
                }
            }
        };
    }
}