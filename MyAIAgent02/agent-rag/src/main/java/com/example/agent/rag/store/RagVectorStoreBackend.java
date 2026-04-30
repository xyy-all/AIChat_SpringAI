package com.example.agent.rag.store;

import com.example.agent.rag.dto.RagHit;
import com.example.agent.rag.entity.DocumentEntity;
import com.example.agent.rag.entity.DocumentVectorEntity;

import java.util.List;

public interface RagVectorStoreBackend {

    String getBackendId();

    boolean isAvailable();

    void indexDocument(DocumentEntity document, List<DocumentVectorEntity> chunks);

    List<RagHit> searchSimilar(String query, String sessionId, int topK);

    void deleteDocument(Long knowledgeDocumentId);
}
