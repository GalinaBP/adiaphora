package ru.adiaphora.platform.questionnaire.domain;

/** A titled group of questions within a questionnaire version. */
public record QuestionSection(String code, String title, int displayOrder) {
}
