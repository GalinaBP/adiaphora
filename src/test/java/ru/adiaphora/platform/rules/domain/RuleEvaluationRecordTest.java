package ru.adiaphora.platform.rules.domain;

import org.junit.jupiter.api.Test;
import ru.adiaphora.platform.questionnaire.api.QuestionnaireSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEvaluationRecordTest {

    @Test
    void hashIsDeterministicAndIndependentOfAnswerInsertionOrder() {
        Map<String, String> ordered = new LinkedHashMap<>();
        ordered.put("totalDebtAmount", "100000");
        ordered.put("hasRegularIncome", "true");

        Map<String, String> reversed = new LinkedHashMap<>();
        reversed.put("hasRegularIncome", "true");
        reversed.put("totalDebtAmount", "100000");

        String first = RuleEvaluationRecord.hashOf(snapshot("v1", ordered));
        String second = RuleEvaluationRecord.hashOf(snapshot("v1", reversed));

        assertThat(first).isEqualTo(second).matches("[0-9a-f]{64}");
    }

    @Test
    void hashChangesWhenAnAnswerOrTheVersionChanges() {
        String base = RuleEvaluationRecord.hashOf(snapshot("v1", Map.of("totalDebtAmount", "100000")));

        assertThat(RuleEvaluationRecord.hashOf(snapshot("v1", Map.of("totalDebtAmount", "100001"))))
                .isNotEqualTo(base);
        assertThat(RuleEvaluationRecord.hashOf(snapshot("v2", Map.of("totalDebtAmount", "100000"))))
                .isNotEqualTo(base);
    }

    @Test
    void hashHandlesMissingVersionAndEmptyAnswers() {
        assertThat(RuleEvaluationRecord.hashOf(snapshot(null, Map.of()))).matches("[0-9a-f]{64}");
    }

    private QuestionnaireSnapshot snapshot(String version, Map<String, String> answers) {
        return new QuestionnaireSnapshot(UUID.randomUUID(), version, answers);
    }
}
