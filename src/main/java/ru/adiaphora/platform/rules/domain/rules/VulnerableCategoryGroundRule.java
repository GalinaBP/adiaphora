package ru.adiaphora.platform.rules.domain.rules;

import ru.adiaphora.platform.rules.api.RuleOutcome;
import ru.adiaphora.platform.rules.api.RuleSeverity;
import ru.adiaphora.platform.rules.domain.BankruptcyRule;
import ru.adiaphora.platform.rules.domain.RuleContext;
import ru.adiaphora.platform.rules.domain.RuleEvaluation;
import ru.adiaphora.platform.rules.domain.RuleInputs;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Block B of the category questionnaire, shared by the pensioner (пп. 2 п. 1 ст. 223.2), unified
 * child-benefit recipient (пп. 3) and SVO-participant (пп. 2) grounds: the underlying conditions are
 * identical — an enforcement document presented for collection a year ago or earlier, the debt still
 * unpaid, and no sellable property. Evaluated once even when several of these categories are
 * selected. The child-benefit category additionally requires confirming the unified benefit (единое
 * пособие) itself; failing that check drops only the child-benefit category, the others are still
 * evaluated.
 */
public class VulnerableCategoryGroundRule implements BankruptcyRule {

    private static final Set<String> BLOCK_CATEGORIES = Set.of(
            RuleInputs.GROUND_PENSIONER, RuleInputs.GROUND_CHILD_BENEFIT, RuleInputs.GROUND_SVO_PARTICIPANT);

    private static final List<String> CATEGORY_ORDER = List.of(
            RuleInputs.GROUND_PENSIONER, RuleInputs.GROUND_CHILD_BENEFIT, RuleInputs.GROUND_SVO_PARTICIPANT);

    @Override
    public String code() {
        return "MFC-GROUND-VULNERABLE-CATEGORY";
    }

    @Override
    public int order() {
        return 100;
    }

    @Override
    public RuleEvaluation evaluate(RuleContext context) {
        Set<String> grounds = context.multi(RuleInputs.MFC_STATUTORY_GROUNDS);
        Set<String> selected = CATEGORY_ORDER.stream()
                .filter(grounds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (selected.isEmpty()) {
            return new RuleEvaluation(code(), RuleOutcome.NOT_APPLICABLE, RuleSeverity.INFO,
                    "no pensioner/child-benefit/SVO category selected", null, false);
        }

        Set<String> active = new LinkedHashSet<>(selected);
        if (selected.contains(RuleInputs.GROUND_CHILD_BENEFIT)) {
            Optional<TriStateAnswer> confirmed =
                    TriStateAnswer.read(context, RuleInputs.CHILD_BENEFIT_CONFIRMED);
            if (confirmed.isEmpty()) {
                return needsInformation("child-benefit confirmation unanswered",
                        "Подтвердите, назначено ли вам именно единое пособие через Социальный фонд (СФР).");
            }
            if (confirmed.get() == TriStateAnswer.NOT_SURE) {
                return manualReview("child-benefit confirmation answered not_sure",
                        "Проверьте назначение единого пособия в личном кабинете на Госуслугах "
                                + "(раздел «Выплаты») или на sfr.gov.ru.");
            }
            if (confirmed.get() == TriStateAnswer.NO) {
                active.remove(RuleInputs.GROUND_CHILD_BENEFIT);
                if (active.isEmpty()) {
                    return new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.INFO,
                            "unified child benefit not confirmed, no other category in the block",
                            "Единое пособие не подтверждено — основание «получатель единого пособия» "
                                    + "не применимо.",
                            false, RuleInputs.BASIS_CHILD_BENEFIT);
                }
            }
        }

        Optional<TriStateAnswer> writ = TriStateAnswer.read(context, RuleInputs.WRIT_UNPAID_OVER_ONE_YEAR);
        Optional<TriStateAnswer> property = TriStateAnswer.read(context, RuleInputs.OWNS_SELLABLE_PROPERTY);
        if (writ.isEmpty()) {
            return needsInformation("writ-over-one-year question unanswered",
                    "Ответьте, предъявлен ли исполнительный документ к взысканию не менее года назад "
                            + "и остаётся ли долг непогашенным.");
        }
        if (property.isEmpty()) {
            return needsInformation("sellable-property question unanswered",
                    "Ответьте, есть ли у вас имущество, которое можно продать в счёт долга.");
        }
        if (writ.get() == TriStateAnswer.NOT_SURE || property.get() == TriStateAnswer.NOT_SURE) {
            return manualReview("writ or property question answered not_sure",
                    "Проверьте документы: дату возбуждения производства на fssp.gov.ru (или списания "
                            + "по счёту в банке / удержания работодателем) и состав вашего имущества.");
        }

        String basis = basisFor(active);
        if (writ.get() == TriStateAnswer.YES && property.get() == TriStateAnswer.NO) {
            return new RuleEvaluation(code(), RuleOutcome.PASSED, RuleSeverity.INFO,
                    "vulnerable-category conditions met for " + active,
                    "Основание подтверждено (" + categoryLabels(active) + "): исполнительный документ "
                            + "предъявлен более года назад, долг не погашен, имущества для продажи нет.",
                    false, basis);
        }
        String reason = writ.get() != TriStateAnswer.YES
                ? "исполнительный документ предъявлен менее года назад или долг уже погашен"
                : "есть имущество, которое может быть продано в счёт долга";
        return new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.INFO,
                "vulnerable-category conditions not met for " + active,
                "Основание (" + categoryLabels(active) + ") не подтверждено: " + reason + ".",
                false, basis);
    }

    private RuleEvaluation needsInformation(String internalReason, String userMessage) {
        return new RuleEvaluation(code(), RuleOutcome.NEEDS_INFORMATION, RuleSeverity.WARNING,
                internalReason, userMessage, true);
    }

    private RuleEvaluation manualReview(String internalReason, String userMessage) {
        return new RuleEvaluation(code(), RuleOutcome.FAILED, RuleSeverity.MANUAL_REVIEW,
                internalReason, userMessage, true, basisFor(BLOCK_CATEGORIES));
    }

    private String basisFor(Set<String> categories) {
        boolean pensionerOrSvo = categories.contains(RuleInputs.GROUND_PENSIONER)
                || categories.contains(RuleInputs.GROUND_SVO_PARTICIPANT);
        boolean childBenefit = categories.contains(RuleInputs.GROUND_CHILD_BENEFIT);
        if (pensionerOrSvo && childBenefit) {
            return "пп. 2 и 3 п. 1 ст. 223.2 Закона № 127-ФЗ";
        }
        return childBenefit ? RuleInputs.BASIS_CHILD_BENEFIT : RuleInputs.BASIS_PENSIONER_SVO;
    }

    private String categoryLabels(Set<String> categories) {
        return categories.stream().map(category -> switch (category) {
            case RuleInputs.GROUND_PENSIONER -> "пенсионер";
            case RuleInputs.GROUND_CHILD_BENEFIT -> "получатель единого пособия";
            case RuleInputs.GROUND_SVO_PARTICIPANT -> "участник СВО";
            default -> category;
        }).collect(Collectors.joining(", "));
    }
}
