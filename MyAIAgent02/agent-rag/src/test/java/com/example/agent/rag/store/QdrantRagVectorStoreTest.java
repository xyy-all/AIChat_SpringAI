package com.example.agent.rag.store;

import com.example.agent.rag.config.RagProperties;
import com.example.agent.rag.dto.RagHit;
import com.example.agent.rag.entity.DocumentEntity;
import com.example.agent.rag.entity.DocumentVectorEntity;
import com.example.agent.rag.enums.RagDocumentStatus;
import com.google.common.util.concurrent.Futures;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.grpc.Points;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QdrantRagVectorStoreTest {

    @Test
    void searchSimilarShouldMapVectorStoreDocumentsToHits() {
        QdrantClient qdrantClient = mock(QdrantClient.class);
        VectorStore vectorStore = mock(VectorStore.class);
        RagProperties properties = new RagProperties();
        QdrantRagVectorStore store = new QdrantRagVectorStore(qdrantClient, vectorStore, properties);

        Document document = Document.builder()
                .id("100_0")
                .text("Qdrant chunk content")
                .metadata(Map.of(
                        "knowledgeDocumentId", 100L,
                        "title", "Qdrant Document",
                        "documentType", "text"))
                .score(0.91d)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(document));

        List<RagHit> hits = store.searchSimilar("test query", "session-1", 3);

        assertEquals(1, hits.size());
        assertEquals(100L, hits.get(0).getKnowledgeDocumentId());
        assertEquals("Qdrant Document", hits.get(0).getTitle());
        assertEquals("text", hits.get(0).getDocumentType());
        assertEquals("Qdrant chunk content", hits.get(0).getChunkContent());
        assertEquals(0.91d, hits.get(0).getScore());
    }

    @Test
    void indexDocumentShouldDeleteExistingPointsThenUpsertVectors() {
        QdrantClient qdrantClient = mock(QdrantClient.class);
        VectorStore vectorStore = mock(VectorStore.class);
        RagProperties properties = new RagProperties();
        QdrantRagVectorStore store = new QdrantRagVectorStore(qdrantClient, vectorStore, properties);

        DocumentEntity document = new DocumentEntity();
        document.setKnowledgeDocumentId(200L);
        document.setExternalDocumentId("doc-200");
        document.setTitle("Indexed Document");
        document.setSourceType("text");
        document.setStatus(RagDocumentStatus.READY);
        document.setIsActive(true);

        DocumentVectorEntity chunk = new DocumentVectorEntity();
        chunk.setKnowledgeDocumentId(200L);
        chunk.setChunkIndex(0);
        chunk.setChunkContent("chunk body");
        chunk.setEmbeddingVector(List.of(0.1d, 0.2d, 0.3d));
        chunk.setMetadata(Map.of("chunkIndex", 0));

        when(qdrantClient.upsertAsync(eq("ai_document_chunks"), anyList()))
                .thenReturn(Futures.immediateFuture(Points.UpdateResult.getDefaultInstance()));

        store.indexDocument(document, List.of(chunk));

        verify(vectorStore).delete(any(Filter.Expression.class));
        verify(qdrantClient).upsertAsync(eq("ai_document_chunks"), anyList());
    }
}
