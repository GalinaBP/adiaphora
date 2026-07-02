/**
 * Public API of the {@code rules} module: the evaluation service and its result types (outcome/
 * severity enums, findings, route), plus the rules-evaluated event consumed by {@code audit}. The
 * rule-authoring SPI and the concrete rules are internal to the module. Exposed as a Spring Modulith
 * named interface.
 */
@org.springframework.modulith.NamedInterface("api")
package ru.adiaphora.platform.rules.api;
