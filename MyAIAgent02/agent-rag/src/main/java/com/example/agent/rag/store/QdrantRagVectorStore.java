package com.example.agent.rag.store;

import com.example.agent.rag.config.RagProperties;
import com.example.agent.rag.dto.RagHit;
import com.example.agent.rag.entity.DocumentEntity;
import com.example.agent.rag.entity.DocumentVectorEntity;
import com.example.agent.rag.enums.RagDocumentStatus;
import io.qdrant.client.PointIdFactory;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.ValueFactory;
import io.qdrant.client.VectorsFactory;
import io.qdrant.client.grpc.JsonWithInt;
import io.qdrant.client.grpc.Points;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "app.rag.vector-store", name = "provider", havingValue = "QDRANT")
public class QdrantRagVectorStore implements RagVectorStoreBackend {

    private static final String CONTENT_FIELD_NAME = "doc_content";
    private static final String METADATA_KNOWLEDGE_DOCUMENT_ID = "knowledgeDocumentId";
    private static final String METADATA_EXTERNAL_DOCUMENT_ID = "externalDocumentId";
    private static final String METADATA_SESSION_ID = "sessionId";
    private static final String METADATA_SCOPE_TYPE = "scopeType";
    private static final String METADATA_DOCUMENT_TYPE = "documentType";
    private static final String METADATA_TITLE = "title";
    private static final String METADATA_SOURCE_URI = "sourceUri";
    private static final String METADATA_CHUNK_INDEX = "chunkIndex";
    private static final String METADATA_TOTAL_CHUNKS = "totalChunks";
    private static final String METADATA_STATUS = "status";
    private static final String METADATA_IS_ACTIVE = "isActive";

    private final QdrantClient qdrantClient;
    private final VectorStore vectorStore;
    private final RagProperties ragProperties;

    public QdrantRagVectorStore(@Qualifier("qdrantClient") QdrantClient qdrantClient,
                                @Qualifier("qdrantVectorStore") VectorStore vectorStore,
                                RagProperties ragProperties) {
        this.qdrantClient = qdrantClient;
        this.vectorStore = vectorStore;
        this.ragProperties = ragProperties;
    }

    @Override
    public String getBackendId() {
        return "qdrant";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void indexDocument(DocumentEntity document, List<DocumentVectorEntity> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }

        try {
            // 先按文档主键删除旧 points，避免重新分块后残留历史向量。
            deleteDocument(document.getKnowledgeDocumentId());

            List<Points.PointStruct> points = chunks.stream()
                    .map(chunk -> toPointStruct(document, chunk, chunks.size()))
                    .toList();
            qdrantClient.upsertAsync(ragProperties.getVectorStore().getQdrant().getCollectionName(), points).get();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to upsert document chunks into Qdrant", ex);
        }
    }

    @Override
    public List<RagHit> searchSimilar(String query, String sessionId, int topK) {
        String normalizedQuery = normalizeText(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        SearchRequest request = SearchRequest.builder()
                .query(normalizedQuery)
                .topK(topK > 0 ? topK : ragProperties.getDefaultTopK())
                .similarityThreshold(ragProperties.getSimilarityThreshold())
                .filterExpression(buildSearchFilter(sessionId))
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(this::toHit)
                .toList();
    }

    @Override
    public void deleteDocument(Long knowledgeDocumentId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        vectorStore.delete(builder.eq(METADATA_KNOWLEDGE_DOCUMENT_ID, knowledgeDocumentId).build());
    }

    private Points.PointStruct toPointStruct(DocumentEntity document, DocumentVectorEntity chunk, int totalChunks) {
        if (chunk.getEmbeddingVector() == null || chunk.getEmbeddingVector().isEmpty()) {
            throw new IllegalStateException("Chunk embedding must be prepared before syncing to Qdrant");
        }

        Map<String, JsonWithInt.Value> payload = new LinkedHashMap<>();
        buildPayload(document, chunk, totalChunks).forEach((key, value) -> payload.put(key, toPayloadValue(value)));

        return Points.PointStruct.newBuilder()
                .setId(buildPointId(chunk))
                .setVectors(VectorsFactory.vectors(toFloatList(chunk.getEmbeddingVector())))
                .putAllPayload(payload)
                .build();
    }

    private Map<String, Object> buildPayload(DocumentEntity document, DocumentVectorEntity chunk, int totalChunks) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (chunk.getMetadata() != null) {
            chunk.getMetadata().forEach((key, value) -> putIfNotNull(payload, key, value));
        }

        payload.put(CONTENT_FIELD_NAME, chunk.getChunkContent());
        payload.put(METADATA_KNOWLEDGE_DOCUMENT_ID, document.getKnowledgeDocumentId());
        putIfNotNull(payload, METADATA_EXTERNAL_DOCUMENT_ID, document.getExternalDocumentId());
        putIfNotNull(payload, METADATA_SESSION_ID, document.getSessionId());
        payload.put(METADATA_SCOPE_TYPE, document.getSessionId() == null ? "GLOBAL" : "SESSION");
        payload.put(METADATA_DOCUMENT_TYPE, document.getSourceType());
        payload.put(METADATA_TITLE, document.getTitle());
        putIfNotNull(payload, METADATA_SOURCE_URI, document.getSourceUri());
        payload.put(METADATA_CHUNK_INDEX, chunk.getChunkIndex());
        payload.put(METADATA_TOTAL_CHUNKS, totalChunks);
        payload.put(METADATA_STATUS, document.getStatus().name());
        payload.put(METADATA_IS_ACTIVE, Boolean.TRUE);
        return payload;
    }

    private RagHit toHit(Document document) {
        RagHit hit = new RagHit();
        hit.setKnowledgeDocumentId(asLong(document.getMetadata().get(METADATA_KNOWLEDGE_DOCUMENT_ID)));
        hit.setTitle(asString(document.getMetadata().get(METADATA_TITLE)));
        hit.setDocumentType(asString(document.getMetadata().get(METADATA_DOCUMENT_TYPE)));
        hit.setChunkContent(document.getText());
        hit.setScore(document.getScore() == null ? 0d : document.getScore());
        hit.setMetadata(document.getMetadata());
        return hit;
    }

    private Filter.Expression buildSearchFilter(String sessionId) {
        FilterExpressionBuilder builder = new FilterExpressionBuilder();
        FilterExpressionBuilder.Op active = builder.eq(METADATA_IS_ACTIVE, true);
        FilterExpressionBuilder.Op ready = builder.eq(METADATA_STATUS, RagDocumentStatus.READY.name());
        FilterExpressionBuilder.Op scope;

        if (StringUtils.isNotBlank(sessionId)) {
            scope = builder.or(
                    builder.eq(METADATA_SESSION_ID, sessionId),
                    builder.eq(METADATA_SCOPE_TYPE, "GLOBAL"));
        } else {
            scope = builder.eq(METADATA_SCOPE_TYPE, "GLOBAL");
        }

        return builder.and(builder.and(active, ready), scope).build();
    }

    private Points.PointId buildPointId(DocumentVectorEntity chunk) {
        String rawId = chunk.getKnowledgeDocumentId() + "_" + chunk.getChunkIndex();
        return PointIdFactory.id(UUID.nameUUIDFromBytes(rawId.getBytes(StandardCharsets.UTF_8)));
    }

    private void putIfNotNull(Map<String, Object> payload, String key, Object value) {
        if (value != null) {
            payload.put(key, value);
        }
    }

    private JsonWithInt.Value toPayloadValue(Object value) {
        if (value == null) {
            return ValueFactory.nullValue();
        }
        if (value instanceof String stringValue) {
            return ValueFactory.value(stringValue);
        }
        if (value instanceof Boolean booleanValue) {
            return ValueFactory.value(booleanValue);
        }
        if (value instanceof Float || value instanceof Double) {
            return ValueFactory.value(((Number) value).doubleValue());
        }
        if (value instanceof Number numberValue) {
            return ValueFactory.value(numberValue.longValue());
        }
        if (value instanceof Map<?, ?> mapValue) {
            Map<String, JsonWithInt.Value> nestedValues = new LinkedHashMap<>();
            mapValue.forEach((key, nestedValue) -> nestedValues.put(String.valueOf(key), toPayloadValue(nestedValue)));
            return ValueFactory.value(nestedValues);
        }
        if (value instanceof List<?> listValue) {
            return ValueFactory.value(listValue.stream().map(this::toPayloadValue).toList());
        }
        return ValueFactory.value(String.valueOf(value));
    }

    private List<Float> toFloatList(List<Double> embeddingVector) {
        return embeddingVector.stream()
                .map(Double::floatValue)
                .toList();
    }

    private Long asLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String normalizeText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
