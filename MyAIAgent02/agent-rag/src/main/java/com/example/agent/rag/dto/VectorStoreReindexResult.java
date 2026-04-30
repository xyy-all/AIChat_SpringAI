package com.example.agent.rag.dto;

import java.util.ArrayList;
import java.util.List;

public class VectorStoreReindexResult {

    private String backend;
    private int documentsScanned;
    private int documentsIndexed;
    private int chunksIndexed;
    private int skippedDocuments;
    private List<Long> failedDocumentIds = new ArrayList<>();

    public String getBackend() {
        return backend;
    }

    public void setBackend(String backend) {
        this.backend = backend;
    }

    public int getDocumentsScanned() {
        return documentsScanned;
    }

    public void setDocumentsScanned(int documentsScanned) {
        this.documentsScanned = documentsScanned;
    }

    public int getDocumentsIndexed() {
        return documentsIndexed;
    }

    public void setDocumentsIndexed(int documentsIndexed) {
        this.documentsIndexed = documentsIndexed;
    }

    public int getChunksIndexed() {
        return chunksIndexed;
    }

    public void setChunksIndexed(int chunksIndexed) {
        this.chunksIndexed = chunksIndexed;
    }

    public int getSkippedDocuments() {
        return skippedDocuments;
    }

    public void setSkippedDocuments(int skippedDocuments) {
        this.skippedDocuments = skippedDocuments;
    }

    public List<Long> getFailedDocumentIds() {
        return failedDocumentIds;
    }

    public void setFailedDocumentIds(List<Long> failedDocumentIds) {
        this.failedDocumentIds = failedDocumentIds == null ? new ArrayList<>() : new ArrayList<>(failedDocumentIds);
    }
}
