package ru.adiaphora.platform;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Verifies the modular-monolith boundaries with Spring Modulith. {@code verify()} fails the build if
 * any module reaches into another module's non-{@code api} internals, or if there is an illegal
 * dependency cycle. Also (re)generates the module documentation under {@code target/}.
 *
 * <p>The {@code demo} package is excluded from the model: its seeder deliberately orchestrates
 * several modules' use cases to build local demo data — an exception no business module gets.
 */
class ModularityTest {

    static final ApplicationModules modules = ApplicationModules.of(
            AdiaphoraPlatformApplication.class,
            DescribedPredicate.describe("demo seeding shim",
                    JavaClass.Predicates.resideInAPackage("ru.adiaphora.platform.demo..")));

    @Test
    void verifiesModuleStructure() {
        modules.verify();
    }

    @Test
    void writesModuleDocumentation() {
        new Documenter(modules).writeDocumentation();
    }
}
