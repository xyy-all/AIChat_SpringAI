package com.example.agent.rag;

import com.example.agent.rag.dto.DocumentIngestRequest;
import com.example.agent.rag.dto.DocumentIngestResult;
import com.example.agent.rag.dto.RagHit;
import com.example.agent.rag.dto.VectorStoreReindexResult;

import java.util.List;

public interface RagService {

    DocumentIngestResult ingestDocument(DocumentIngestRequest request);

    List<RagHit> searchSimilar(String query, int k);

    List<RagHit> searchSimilar(String query, String sessionId, int k);

    List<DocumentIngestResult> listDocuments(String sessionId);

    DocumentIngestResult getDocument(Long knowledgeDocumentId);

    boolean deactivateDocument(Long knowledgeDocumentId);

    VectorStoreReindexResult reindexActiveDocuments();
}
