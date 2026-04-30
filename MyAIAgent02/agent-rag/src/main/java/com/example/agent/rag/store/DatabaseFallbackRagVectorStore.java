package com.example.agent.rag.store;

import com.example.agent.rag.config.RagProperties;
import com.example.agent.rag.dto.RagHit;
import com.example.agent.rag.entity.DocumentEntity;
import com.example.agent.rag.entity.DocumentVectorEntity;
import com.example.agent.rag.enums.RagDocumentStatus;
import com.example.agent.rag.mapper.DocumentVectorMapper;
import com.example.agent.rag.support.RagMathUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class DatabaseFallbackRagVectorStore implements RagVectorStoreBackend {

    private final EmbeddingModel embeddingModel;
    private final DocumentVectorMapper documentVectorMapper;
    private final RagProperties ragProperties;

    public DatabaseFallbackRagVectorStore(EmbeddingModel embeddingModel,
                                          DocumentVectorMapper documentVectorMapper,
                                          RagProperties ragProperties) {
        this.embeddingModel = embeddingModel;
        this.documentVectorMapper = documentVectorMapper;
        this.ragProperties = ragProperties;
    }

    @Override
    public String getBackendId() {
        return "database";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void indexDocument(DocumentEntity document, List<DocumentVectorEntity> chunks) {
        // no-op
    }

    @Override
    public List<RagHit> searchSimilar(String query, String sessionId, int topK) {
        String normalizedQuery = normalizeText(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        List<DocumentVectorEntity> candidates = documentVectorMapper.selectSearchCandidates(
                trimToNull(sessionId),
                RagDocumentStatus.READY,
                ragProperties.getMaxCandidates());
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Double> queryEmbedding = RagMathUtils.toDoubleList(embeddingModel.embed(normalizedQuery));
        int limit = topK > 0 ? topK : ragProperties.getDefaultTopK();

        return candidates.stream()
                .map(candidate -> toHit(candidate, RagMathUtils.cosineSimilarity(queryEmbedding, candidate.getEmbeddingVector())))
                .filter(hit -> hit.getScore() >= ragProperties.getSimilarityThreshold())
                .sorted(Comparator.comparingDouble(RagHit::getScore).reversed())
                .limit(limit)
                .toList();
    }

    @Override
    public void deleteDocument(Long knowledgeDocumentId) {
        // no-op
    }

    private RagHit toHit(DocumentVectorEntity candidate, double score) {
        RagHit hit = new RagHit();
        hit.setKnowledgeDocumentId(candidate.getKnowledgeDocumentId());
        hit.setDocumentType(candidate.getDocumentType());
        hit.setChunkContent(candidate.getChunkContent());
        hit.setScore(score);
        hit.setMetadata(candidate.getMetadata());
        return hit;
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

    private String trimToNull(String value) {
        return StringUtils.trimToNull(value);
    }
}
