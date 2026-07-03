/**
 * Document module: document template metadata, generation requests, generated-document metadata and
 * status tracking, a replaceable storage abstraction, and download authorization. Generation and
 * storage are interface-driven; the shipped implementations are development placeholders (in-memory
 * storage, stub generator) and are not production-grade. Consumes {@code application.api}; exposes
 * {@code document.api} events for {@code audit}.
 */
@org.springframework.modulith.ApplicationModule(displayName = "Document")
package ru.adiaphora.platform.document;
