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
                "Предварительная анкета о банкротстве (v1) — ЧЕРНОВИК, ожидает проверки юристом",
                VersionStatus.ACTIVE));

        sections.saveAll(List.of(
                new QuestionSectionEntity(UUID.randomUUID(), versionId, "debts", "Долги", 1),
                new QuestionSectionEntity(UUID.randomUUID(), versionId, "assets", "Имущество", 2),
                new QuestionSectionEntity(UUID.randomUUID(), versionId, "personal", "Личные данные", 3)));

        List<QuestionDefinitionEntity> defs = new ArrayList<>();
        defs.add(question(versionId, "debts", "totalDebtAmount", QuestionType.MONEY,
                "Общая сумма долгов (₽)", true, 1));
        defs.add(question(versionId, "debts", "hasRegularIncome", QuestionType.BOOLEAN,
                "Есть ли у вас регулярный доход?", true, 2));
        defs.add(question(versionId, "debts", "monthlyIncome", QuestionType.MONEY,
                "Примерный ежемесячный доход (₽)", false, 3));
        defs.add(question(versionId, "assets", "ownsMortgagedHome", QuestionType.BOOLEAN,
                "Есть ли у вас жильё в ипотеке?", true, 4));
        defs.add(question(versionId, "assets", "previousBankruptcy", QuestionType.BOOLEAN,
                "Признавались ли вы банкротом ранее?", true, 5));

        QuestionDefinitionEntity propertyTx = question(versionId, "assets", "recentPropertyTransaction",
                QuestionType.SINGLE_CHOICE, "Сделки с имуществом за последние 3 года", true, 6);
        defs.add(propertyTx);
        QuestionDefinitionEntity statutoryGround = question(versionId, "debts", "mfcStatutoryGround",
                QuestionType.SINGLE_CHOICE,
                "Подходите ли вы под одну из категорий для внесудебного банкротства?", true, 7);
        defs.add(statutoryGround);
        defs.add(question(versionId, "personal", "employmentStatus", QuestionType.SINGLE_CHOICE,
                "Текущая занятость", false, 8));

        questions.saveAll(defs);

        options.saveAll(List.of(
                new QuestionOptionEntity(UUID.randomUUID(), propertyTx.getId(), "none", "Нет", 1),
                new QuestionOptionEntity(UUID.randomUUID(), propertyTx.getId(), "sold", "Продавал(а) имущество", 2),
                new QuestionOptionEntity(UUID.randomUUID(), propertyTx.getId(), "gifted", "Дарил(а) имущество", 3)));

        options.saveAll(List.of(
                new QuestionOptionEntity(UUID.randomUUID(), statutoryGround.getId(), "enforcement_ended",
                        "Исполнительное производство окончено из-за отсутствия имущества, документ вернули "
                                + "взыскателю, новых производств после этого нет", 1),
                new QuestionOptionEntity(UUID.randomUUID(), statutoryGround.getId(), "pensioner_or_svo",
                        "Я пенсионер или участник СВО, исполнительный документ выдан более года назад, "
                                + "предъявлен и не исполнен, имущества для взыскания нет", 2),
                new QuestionOptionEntity(UUID.randomUUID(), statutoryGround.getId(), "child_benefit",
                        "Получаю ежемесячное пособие в связи с рождением и воспитанием ребёнка, исполнительный "
                                + "документ выдан более года назад, предъявлен и не исполнен, имущества нет", 3),
                new QuestionOptionEntity(UUID.randomUUID(), statutoryGround.getId(), "long_enforcement",
                        "Исполнительный документ имущественного характера выдан более 7 лет назад, предъявлен "
                                + "и до сих пор не исполнен полностью", 4),
                new QuestionOptionEntity(UUID.randomUUID(), statutoryGround.getId(), "none",
                        "Ни одна из ситуаций не относится ко мне", 5)));

        UUID employmentId = defs.get(defs.size() - 1).getId();
        options.saveAll(List.of(
                new QuestionOptionEntity(UUID.randomUUID(), employmentId, "employed", "Работаю по найму", 1),
                new QuestionOptionEntity(UUID.randomUUID(), employmentId, "unemployed", "Не работаю", 2),
                new QuestionOptionEntity(UUID.randomUUID(), employmentId, "self_employed", "Самозанятый / ИП", 3)));

        log.info("Seeded placeholder questionnaire version 'v1' with {} questions", defs.size());
    }

    private QuestionDefinitionEntity question(UUID versionId, String sectionCode, String code,
                                              QuestionType type, String label, boolean required, int order) {
        return new QuestionDefinitionEntity(UUID.randomUUID(), versionId, sectionCode, code, type, label,
                null, required, order, null);
    }
}
