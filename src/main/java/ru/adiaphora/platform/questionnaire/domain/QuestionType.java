package ru.adiaphora.platform.questionnaire.domain;

/** The supported answer types. Determines how a raw answer value is validated and normalised. */
public enum QuestionType {
    TEXT,
    TEXTAREA,
    INTEGER,
    MONEY,
    BOOLEAN,
    SINGLE_CHOICE,
    MULTIPLE_CHOICE,
    DATE
}
