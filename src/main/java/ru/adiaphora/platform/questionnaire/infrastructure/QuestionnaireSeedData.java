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
 * active version already exists (run {@code docker compose down -v} for a fresh definition after a
 * seed change). Question set follows the approved MFC eligibility-flow ticket (127-ФЗ, ст. 223.2);
 * wording is <strong>pending legal review</strong> and contains no real personal data.
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
        versions.save(new QuestionnaireVersionEntity(versionId, "v2",
                "Анкета внесудебного банкротства (ст. 223.2 127-ФЗ) — ЧЕРНОВИК, ожидает проверки юристом",
                VersionStatus.ACTIVE));

        sections.saveAll(List.of(
                new QuestionSectionEntity(UUID.randomUUID(), versionId, "debts", "Долги", 1),
                new QuestionSectionEntity(UUID.randomUUID(), versionId, "assets", "Имущество", 2),
                new QuestionSectionEntity(UUID.randomUUID(), versionId, "personal", "Личные данные", 3)));

        List<QuestionDefinitionEntity> defs = new ArrayList<>();
        // Stage 1: debt amount (hard gate, 25 000–1 000 000 ₽).
        defs.add(question(versionId, "debts", "totalDebtAmount", QuestionType.MONEY,
                "Общая сумма долгов (₽)", true, 1));
        // Stage 2: prior bankruptcy (hard gate, 5-year repeat-filing bar).
        defs.add(question(versionId, "personal", "previousBankruptcy", QuestionType.BOOLEAN,
                "Признавались ли вы банкротом ранее (через МФЦ или суд)?", true, 2));
        defs.add(question(versionId, "personal", "previousBankruptcyEndedOn", QuestionType.DATE,
                "Когда завершилась или была прекращена предыдущая процедура?", false, 3));
        // Stage 3.1: statutory categories (multi-select, OR across categories).
        QuestionDefinitionEntity grounds = question(versionId, "debts", "mfcStatutoryGrounds",
                QuestionType.MULTIPLE_CHOICE, "Какие из этих ситуаций к вам относятся?", true, 4);
        defs.add(grounds);
        // Stage 3.2 follow-up blocks (tri-state answers; asked only for the selected categories).
        QuestionDefinitionEntity bailiffs = question(versionId, "debts", "bailiffsCaseClosedNoNew",
                QuestionType.SINGLE_CHOICE,
                "Пристав окончил производство из-за отсутствия имущества, и новых производств нет?",
                false, 5);
        defs.add(bailiffs);
        QuestionDefinitionEntity childBenefit = question(versionId, "personal", "childBenefitConfirmed",
                QuestionType.SINGLE_CHOICE,
                "Вам назначено именно единое пособие через Социальный фонд (СФР)?", false, 6);
        defs.add(childBenefit);
        QuestionDefinitionEntity writYear = question(versionId, "debts", "writUnpaidOverOneYear",
                QuestionType.SINGLE_CHOICE,
                "Исполнительный документ предъявлен к взысканию не менее года назад и долг не погашен?",
                false, 7);
        defs.add(writYear);
        QuestionDefinitionEntity sellable = question(versionId, "assets", "ownsSellableProperty",
                QuestionType.SINGLE_CHOICE,
                "Есть ли имущество, которое можно продать в счёт долга (кроме единственного жилья)?",
                false, 8);
        defs.add(sellable);
        QuestionDefinitionEntity writSeven = question(versionId, "debts", "writIssuedOverSevenYears",
                QuestionType.SINGLE_CHOICE,
                "Исполнительный документ выдан не менее семи лет назад и предъявлялся к взысканию?",
                false, 9);
        defs.add(writSeven);
        // Supplementary context questions (not part of the ст. 223.2 eligibility decision).
        defs.add(question(versionId, "debts", "hasRegularIncome", QuestionType.BOOLEAN,
                "Есть ли у вас регулярный доход?", false, 10));
        defs.add(question(versionId, "debts", "monthlyIncome", QuestionType.MONEY,
                "Примерный ежемесячный доход (₽)", false, 11));
        defs.add(question(versionId, "assets", "ownsMortgagedHome", QuestionType.BOOLEAN,
                "Есть ли у вас жильё в ипотеке?", false, 12));
        QuestionDefinitionEntity propertyTx = question(versionId, "assets", "recentPropertyTransaction",
                QuestionType.SINGLE_CHOICE, "Сделки с имуществом за последние 3 года", false, 13);
        defs.add(propertyTx);
        defs.add(question(versionId, "personal", "employmentStatus", QuestionType.SINGLE_CHOICE,
                "Текущая занятость", false, 14));

        questions.saveAll(defs);

        options.saveAll(List.of(
                new QuestionOptionEntity(UUID.randomUUID(), grounds.getId(), "enforcement_ended",
                        "Приставы уже работали с долгом и закрыли дело", 1),
                new QuestionOptionEntity(UUID.randomUUID(), grounds.getId(), "pensioner",
                        "Пенсионер — пенсия единственный или основной доход", 2),
                new QuestionOptionEntity(UUID.randomUUID(), grounds.getId(), "child_benefit",
                        "Получаю единое пособие на ребёнка (через СФР)", 3),
                new QuestionOptionEntity(UUID.randomUUID(), grounds.getId(), "svo_participant",
                        "Участвую или участвовал(а) в СВО", 4),
                new QuestionOptionEntity(UUID.randomUUID(), grounds.getId(), "long_enforcement",
                        "Долг взыскивают уже 7 лет или дольше", 5),
                new QuestionOptionEntity(UUID.randomUUID(), grounds.getId(), "none",
                        "Ни одна ситуация не подходит", 6)));

        for (QuestionDefinitionEntity triState : List.of(bailiffs, childBenefit, writYear, sellable, writSeven)) {
            options.saveAll(List.of(
                    new QuestionOptionEntity(UUID.randomUUID(), triState.getId(), "yes", "Да", 1),
                    new QuestionOptionEntity(UUID.randomUUID(), triState.getId(), "no", "Нет", 2),
                    new QuestionOptionEntity(UUID.randomUUID(), triState.getId(), "not_sure",
                            "Не уверен(а), нужно проверить документы", 3)));
        }

        options.saveAll(List.of(
                new QuestionOptionEntity(UUID.randomUUID(), propertyTx.getId(), "none", "Нет", 1),
                new QuestionOptionEntity(UUID.randomUUID(), propertyTx.getId(), "sold", "Продавал(а) имущество", 2),
                new QuestionOptionEntity(UUID.randomUUID(), propertyTx.getId(), "gifted", "Дарил(а) имущество", 3)));

        UUID employmentId = defs.get(defs.size() - 1).getId();
        options.saveAll(List.of(
                new QuestionOptionEntity(UUID.randomUUID(), employmentId, "employed", "Работаю по найму", 1),
                new QuestionOptionEntity(UUID.randomUUID(), employmentId, "unemployed", "Не работаю", 2),
                new QuestionOptionEntity(UUID.randomUUID(), employmentId, "self_employed", "Самозанятый / ИП", 3)));

        log.info("Seeded questionnaire version 'v2' (MFC eligibility flow) with {} questions", defs.size());
    }

    private QuestionDefinitionEntity question(UUID versionId, String sectionCode, String code,
                                              QuestionType type, String label, boolean required, int order) {
        return new QuestionDefinitionEntity(UUID.randomUUID(), versionId, sectionCode, code, type, label,
                null, required, order, null);
    }
}
