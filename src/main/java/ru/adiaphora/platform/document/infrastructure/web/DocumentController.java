package ru.adiaphora.platform.document.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import ru.adiaphora.platform.common.web.ApiPaths;
import ru.adiaphora.platform.document.application.DocumentQueries;
import ru.adiaphora.platform.document.application.DownloadDocumentUseCase;
import ru.adiaphora.platform.document.application.DownloadedDocument;
import ru.adiaphora.platform.document.application.RequestDocumentUseCase;

import java.util.List;
import java.util.UUID;

/** REST endpoints for requesting, listing, viewing, and downloading generated documents. */
@Tag(name = "Documents")
@RestController
class DocumentController {

    private final RequestDocumentUseCase requestDocument;
    private final DocumentQueries queries;
    private final DownloadDocumentUseCase downloadDocument;

    DocumentController(RequestDocumentUseCase requestDocument, DocumentQueries queries,
                       DownloadDocumentUseCase downloadDocument) {
        this.requestDocument = requestDocument;
        this.queries = queries;
        this.downloadDocument = downloadDocument;
    }

    @Operation(summary = "Request generation of a document for an application")
    @PostMapping(ApiPaths.API_V1 + "/applications/{applicationId}/documents")
    @ResponseStatus(HttpStatus.CREATED)
    DocumentDtos.DocumentResponse create(@PathVariable UUID applicationId,
                                         @Valid @RequestBody(required = false) DocumentDtos.CreateDocumentRequest request) {
        String templateCode = request == null ? null : request.templateCode();
        return DocumentDtos.DocumentResponse.from(requestDocument.request(applicationId, templateCode));
    }

    @Operation(summary = "List documents for an application")
    @GetMapping(ApiPaths.API_V1 + "/applications/{applicationId}/documents")
    List<DocumentDtos.DocumentResponse> list(@PathVariable UUID applicationId) {
        return queries.listForApplication(applicationId).stream()
                .map(DocumentDtos.DocumentResponse::from)
                .toList();
    }

    @Operation(summary = "Get document metadata")
    @GetMapping(ApiPaths.API_V1 + "/documents/{documentId}")
    DocumentDtos.DocumentResponse get(@PathVariable UUID documentId) {
        return DocumentDtos.DocumentResponse.from(queries.get(documentId));
    }

    @Operation(summary = "Download a document's content")
    @GetMapping(ApiPaths.API_V1 + "/documents/{documentId}/download")
    ResponseEntity<byte[]> download(@PathVariable UUID documentId) {
        DownloadedDocument document = downloadDocument.download(documentId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + document.filename() + "\"")
                .contentType(MediaType.parseMediaType(document.contentType()))
                .body(document.content());
    }
}
