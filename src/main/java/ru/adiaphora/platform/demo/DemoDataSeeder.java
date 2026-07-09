package ru.adiaphora.platform.demo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import ru.adiaphora.platform.application.application.ApplicationQueries;
import ru.adiaphora.platform.application.application.CreateApplicationUseCase;
import ru.adiaphora.platform.application.application.SubmitApplicationUseCase;
import ru.adiaphora.platform.auth.api.UserDirectory;
import ru.adiaphora.platform.common.security.AuthenticatedUser;
import ru.adiaphora.platform.common.security.Authorities;
import ru.adiaphora.platform.questionnaire.application.SaveAnswerUseCase;
import ru.adiaphora.platform.rules.application.EvaluateApplicationUseCase;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Seeds demo scenarios for the synthetic {@code user@example.test} account by driving the real use
 * cases under an impersonated principal — so the demo cases carry genuine evaluations, review tasks
 * and audit records, and a demo needs no manual database editing. Runs after the account, questionnaire
 * and single-draft seeds; idempotent (skips when the demo user already has more than one case).
 *
 * <p>The {@code demo} package is deliberately excluded from the Spring Modulith verification (see
 * {@code ModularityTest}): this seeder orchestrates several modules' use cases, which no business
 * module is allowed to do. It is a local-only shim, never active outside the seeded profile.
 *
 * <p>All data is obviously synthetic: {@code example.test} accounts and the AI-012 boundary amounts.
 * Scenario amounts mirror the approved boundary cases: 24,999 / 25,000 / 1,000,000 / 1,000,001 RUB.
 */
@Component
@ConditionalOnProperty(prefix = "adiaphora.seed", name = "enabled", havingValue = "true")
@Order(3)
class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);
    private static final String DEMO_OWNER_EMAIL = "user@example.test";

    private final UserDirectory userDirectory;
    private final ApplicationQueries applicationQueries;
    private final CreateApplicationUseCase createApplication;
    private final SubmitApplicationUseCase submitApplication;
    private final SaveAnswerUseCase saveAnswer;
    private final EvaluateApplicationUseCase evaluateApplication;

    DemoDataSeeder(UserDirectory userDirectory, ApplicationQueries applicationQueries,
                   CreateApplicationUseCase createApplication, SubmitApplicationUseCase submitApplication,
                   SaveAnswerUseCase saveAnswer, EvaluateApplicationUseCase evaluateApplication) {
        this.userDirectory = userDirectory;
        this.applicationQueries = applicationQueries;
        this.createApplication = createApplication;
        this.submitApplication = submitApplication;
        this.saveAnswer = saveAnswer;
        this.evaluateApplication = evaluateApplication;
    }

    @Override
    public void run(ApplicationArguments args) {
        userDirectory.findByEmail(DEMO_OWNER_EMAIL).ifPresent(owner -> {
            AuthenticatedUser principal =
                    new AuthenticatedUser(owner.userId(), DEMO_OWNER_EMAIL, Authorities.USER);
            try {
                asUser(principal, this::seedScenarios);
            } catch (RuntimeException ex) {
                // Demo data is a convenience — never block application startup over it (e.g. when the
                // active questionnaire definition doesn't carry the expected question codes).
                log.warn("Demo data seeding skipped: {}", ex.getMessage());
            }
        });
    }

    private void seedScenarios() {
        // ApplicationSeedData creates one empty draft; more than one case means demo data is present.
        if (applicationQueries.list(PageRequest.of(0, 2)).totalElements() > 1) {
            return;
        }

        // AI-012 approved boundary amounts, each evaluated to its expected route.
        evaluatedCase("25000");   // at the MFC lower bound -> MFC_PRELIMINARY
        evaluatedCase("1000000"); // at the MFC upper bound -> MFC_PRELIMINARY
        evaluatedCase("24999");   // just below              -> NOT_CURRENTLY_RECOMMENDED
        evaluatedCase("1000001"); // just above              -> COURT_PRELIMINARY

        // Mortgaged home -> MANUAL_REVIEW; a review task opens for the staff demo.
        Map<String, String> mortgage = baseAnswers("500000");
        mortgage.put("ownsMortgagedHome", "true");
        evaluatedCase(mortgage);

        // An in-progress draft with a partial questionnaire, for the resume/completion demo.
        UUID draft = createApplication.create();
        saveAnswer.save(draft, "totalDebtAmount", "300000");

        log.info("Seeded 6 demo cases for {} (boundary routes, manual review, partial draft)",
                DEMO_OWNER_EMAIL);
    }

    private void evaluatedCase(String debtAmount) {
        evaluatedCase(baseAnswers(debtAmount));
    }

    private void evaluatedCase(Map<String, String> answers) {
        UUID applicationId = createApplication.create();
        answers.forEach((code, value) -> saveAnswer.save(applicationId, code, value));
        submitApplication.submit(applicationId);
        evaluateApplication.evaluate(applicationId);
    }

    private Map<String, String> baseAnswers(String debtAmount) {
        Map<String, String> answers = new LinkedHashMap<>();
        answers.put("totalDebtAmount", debtAmount);
        answers.put("hasRegularIncome", "true");
        answers.put("ownsMortgagedHome", "false");
        answers.put("previousBankruptcy", "false");
        answers.put("recentPropertyTransaction", "none");
        answers.put("mfcStatutoryGround", "enforcement_ended");
        return answers;
    }

    private void asUser(AuthenticatedUser principal, Runnable action) {
        var authentication = new UsernamePasswordAuthenticationToken(principal, null,
                List.of(new SimpleGrantedAuthority(principal.role())));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        try {
            action.run();
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
