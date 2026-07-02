package ru.adiaphora.platform.questionnaire.domain;

/** A selectable option for a SINGLE_CHOICE/MULTIPLE_CHOICE question. */
public record QuestionOption(String value, String label, int displayOrder) {
}
