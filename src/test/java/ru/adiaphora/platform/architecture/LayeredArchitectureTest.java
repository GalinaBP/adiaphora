package ru.adiaphora.platform.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Fine-grained architecture rules that complement Spring Modulith's module-boundary checks. These
 * assert the internal layering convention (domain → application → infrastructure) and keep the
 * {@code common} module free of dependencies on business modules.
 *
 * <p>Rules use {@code allowEmptyShould(true)} so they remain green while modules are still being
 * scaffolded, and start biting as soon as the relevant packages exist.
 */
class LayeredArchitectureTest {

    private static final String ROOT = "ru.adiaphora.platform";
    private static JavaClasses classes;

    @BeforeAll
    static void importClasses() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(ROOT);
    }

    @Test
    void domainMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void domainMustNotDependOnWebOrPersistenceFrameworks() {
        // Neutral pagination value types (org.springframework.data.domain: Page/Pageable/Sort) are
        // allowed in domain repository interfaces; JPA and web frameworks are not.
        ArchRule rule = noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        "org.springframework.web..",
                        "jakarta.persistence..",
                        "org.springframework.data.jpa..",
                        "org.springframework.stereotype..")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void controllersMustNotDependOnSpringDataRepositories() {
        ArchRule rule = noClasses()
                .that().haveSimpleNameEndingWith("Controller")
                .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository")
                .allowEmptyShould(true);
        rule.check(classes);
    }

    @Test
    void commonMustNotDependOnBusinessModules() {
        ArchRule rule = noClasses()
                .that().resideInAPackage(ROOT + ".common..")
                .should().dependOnClassesThat().resideInAnyPackage(
                        ROOT + ".auth..",
                        ROOT + ".application..",
                        ROOT + ".questionnaire..",
                        ROOT + ".rules..",
                        ROOT + ".review..",
                        ROOT + ".document..",
                        ROOT + ".audit..")
                .allowEmptyShould(true);
        rule.check(classes);
    }
}
