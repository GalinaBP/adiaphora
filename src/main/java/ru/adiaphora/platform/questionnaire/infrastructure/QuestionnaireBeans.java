package ru.adiaphora.platform.questionnaire.infrastructure;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.adiaphora.platform.questionnaire.domain.AnswerValidator;

/** Registers questionnaire domain services (kept framework-free) as Spring beans. */
@Configuration
class QuestionnaireBeans {

    @Bean
    AnswerValidator answerValidator() {
        return new AnswerValidator();
    }
}
