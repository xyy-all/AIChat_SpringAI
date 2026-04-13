package com.example.agent.api.dto;

import com.example.agent.rag.dto.DocumentIngestRequest;

import java.util.HashMap;
import java.util.Map;

public class DocumentUploadRequest {

    private String text;

    private String documentId;

    private String sessionId;

    private String title;

    private String documentType;

    private String sourceUri;

    private Map<String, Object> metadata = new HashMap<>();

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getDocumentId() {
        return documentId;
    }

    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata == null ? new HashMap<>() : new HashMap<>(metadata);
    }

    public DocumentIngestRequest toIngestRequest() {
        DocumentIngestRequest request = new DocumentIngestRequest();
        request.setText(text);
        request.setExternalDocumentId(documentId);
        request.setSessionId(sessionId);
        request.setTitle(title);
        request.setDocumentType(documentType);
        request.setSourceUri(sourceUri);
        request.setMetadata(metadata);
        return request;
    }
}
