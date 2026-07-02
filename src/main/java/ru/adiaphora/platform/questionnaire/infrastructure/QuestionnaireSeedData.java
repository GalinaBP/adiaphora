package ru.adiaphora.platform.questionnaire.infrastructure;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.adiaphora.platform.questionnaire.domain.QuestionType;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Seeds one active, synthetic questionnaire version for local development. Idempotent: skips if an
 * active version already exists. Questions are <strong>placeholders pending legal review</strong> and
 * contain no real personal data.
 */
@Component
@ConditionalOnProperty(prefix = "adiaphora.seed", name = "enabled", havingValue = "true")
@Order(0)
class QuestionnaireSeedData implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(QuestionnaireSeedData.class);

    private final QuestionnaireVersionJpaRepository versions;
    private final QuestionSectionJpaRepository sections;
    private final QuestionDefinitionJpaRepository questions;
    private final QuestionOptionJpaRepository options;

    QuestionnaireSeedData(QuestionnaireVersionJpaRepository versions, QuestionSectionJpaRepository sections,
                          QuestionDefinitionJpaRepository questions, QuestionOptionJpaRepository options) {
        this.versions = versions;
        this.sections = sections;
        this.questions = questions;
        this.options = options;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (versions.existsByStatus(VersionStatus.ACTIVE)) {
            return;
        }
        UUID versionId = UUID.randomUUID();
        versions.save(new QuestionnaireVersionEntity(versionId, "v1",
                "Preliminary bankruptcy questionnaire (v1) — PLACEHOLDER, pending legal review",
                VersionStatus.ACTIVE));

        sections.saveAll(List.of(
                new QuestionSectionEntity(UUID.randomUUID(), versionId, "debts", "Debts", 1),
                new QuestionSectionEntity(UUID.randomUUID(), versionId, "assets", "Assets & property", 2),
                new QuestionSectionEntity(UUID.randomUUID(), versionId, "personal", "Personal", 3)));

        List<QuestionDefinitionEntity> defs = new ArrayList<>();
        defs.add(question(versionId, "debts", "totalDebtAmount", QuestionType.MONEY,
                "Total amount of debts (RUB)", true, 1));
        defs.add(question(versionId, "debts", "hasRegularIncome", QuestionType.BOOLEAN,
                "Do you have a regular income?", true, 2));
        defs.add(question(versionId, "debts", "monthlyIncome", QuestionType.MONEY,
                "Approximate monthly income (RUB)", false, 3));
        defs.add(question(versionId, "assets", "ownsMortgagedHome", QuestionType.BOOLEAN,
                "Do you own a home under mortgage?", true, 4));
        defs.add(question(versionId, "assets", "previousBankruptcy", QuestionType.BOOLEAN,
                "Have you been declared bankrupt before?", true, 5));

        QuestionDefinitionEntity propertyTx = question(versionId, "assets", "recentPropertyTransaction",
                QuestionType.SINGLE_CHOICE, "Property transactions in the last 3 years", true, 6);
        defs.add(propertyTx);
        defs.add(question(versionId, "personal", "employmentStatus", QuestionType.SINGLE_CHOICE,
                "Current employment status", false, 7));

        questions.saveAll(defs);

        options.saveAll(List.of(
                new QuestionOptionEntity(UUID.randomUUID(), propertyTx.getId(), "none", "None", 1),
                new QuestionOptionEntity(UUID.randomUUID(), propertyTx.getId(), "sold", "Sold property", 2),
                new QuestionOptionEntity(UUID.randomUUID(), propertyTx.getId(), "gifted", "Gifted property", 3)));

        UUID employmentId = defs.get(defs.size() - 1).getId();
        options.saveAll(List.of(
                new QuestionOptionEntity(UUID.randomUUID(), employmentId, "employed", "Employed", 1),
                new QuestionOptionEntity(UUID.randomUUID(), employmentId, "unemployed", "Unemployed", 2),
                new QuestionOptionEntity(UUID.randomUUID(), employmentId, "self_employed", "Self-employed", 3)));

        log.info("Seeded placeholder questionnaire version 'v1' with {} questions", defs.size());
    }

    private QuestionDefinitionEntity question(UUID versionId, String sectionCode, String code,
                                              QuestionType type, String label, boolean required, int order) {
        return new QuestionDefinitionEntity(UUID.randomUUID(), versionId, sectionCode, code, type, label,
                null, required, order, null);
    }
}
