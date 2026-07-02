package ru.adiaphora.platform.application.api;

/**
 * Preliminary recommended route for a case, produced by the rules module. {@code MFC} is the
 * out-of-court (extrajudicial) route via the multifunctional centre; {@code COURT} is the judicial
 * route. Values are preliminary and subject to manual/legal review.
 */
public enum BankruptcyRoute {
    NOT_EVALUATED,
    MFC_PRELIMINARY,
    COURT_PRELIMINARY,
    MANUAL_REVIEW,
    INSUFFICIENT_INFORMATION,
    NOT_CURRENTLY_RECOMMENDED
}
