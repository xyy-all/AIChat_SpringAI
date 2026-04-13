package com.example.agent.rag.dto;

import com.example.agent.rag.enums.RagDocumentStatus;

import java.time.LocalDateTime;

public class DocumentIngestResult {

    private Long knowledgeDocumentId;

    private String externalDocumentId;

    private String sessionId;

    private String title;

    private String documentType;

    private String sourceUri;

    private RagDocumentStatus status;

    private Integer chunkCount;

    private boolean reused;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getKnowledgeDocumentId() {
        return knowledgeDocumentId;
    }

    public void setKnowledgeDocumentId(Long knowledgeDocumentId) {
        this.knowledgeDocumentId = knowledgeDocumentId;
    }

    public String getExternalDocumentId() {
        return externalDocumentId;
    }

    public void setExternalDocumentId(String externalDocumentId) {
        this.externalDocumentId = externalDocumentId;
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

    public RagDocumentStatus getStatus() {
        return status;
    }

    public void setStatus(RagDocumentStatus status) {
        this.status = status;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public boolean isReused() {
        return reused;
    }

    public void setReused(boolean reused) {
        this.reused = reused;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
