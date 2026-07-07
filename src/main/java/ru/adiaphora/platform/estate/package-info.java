/**
 * Estate module: the debtor's financial inventory attached to a bankruptcy case — creditors (claims
 * owed) and assets (property owned). Provides owner-scoped CRUD with duplicate warnings. Every
 * operation is authorized against the owning application through {@code application.api}, so a normal
 * user may only touch their own case. Backs the {@code creditors} / {@code assets} tables
 * (migrations V010/V011).
 */
@org.springframework.modulith.ApplicationModule(displayName = "Estate")
package ru.adiaphora.platform.estate;
