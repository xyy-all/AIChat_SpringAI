package com.example.agent.rag;

import com.example.agent.rag.config.RagProperties;
import com.example.agent.rag.dto.DocumentIngestRequest;
import com.example.agent.rag.dto.DocumentIngestResult;
import com.example.agent.rag.dto.RagHit;
import com.example.agent.rag.entity.DocumentEntity;
import com.example.agent.rag.entity.DocumentVectorEntity;
import com.example.agent.rag.enums.RagDocumentStatus;
import com.example.agent.rag.mapper.DocumentMapper;
import com.example.agent.rag.mapper.DocumentVectorMapper;
import com.example.agent.rag.support.RagMathUtils;
import com.example.agent.rag.support.TextChunker;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * RAG 核心服务。
 *
 * <p>职责分成两部分：
 * 1. 文档入库：文本清洗、去重、分块、向量化、写入文档主表和分块表。
 * 2. 文档检索：按会话范围拉取候选分块，做向量相似度计算，再返回结构化命中结果。
 *
 * <p>当前实现是“数据库持久化 + 应用层相似度计算”的第一阶段版本，
 * 重点先保证文档不会因服务重启而丢失。
 */
@Service
public class RagService {

    private static final String PARSER_VERSION = "text-v1";

    private final EmbeddingModel embeddingModel;
    private final DocumentMapper documentMapper;
    private final DocumentVectorMapper documentVectorMapper;
    private final RagProperties ragProperties;

    public RagService(EmbeddingModel embeddingModel,
                      DocumentMapper documentMapper,
                      DocumentVectorMapper documentVectorMapper,
                      RagProperties ragProperties) {
        this.embeddingModel = embeddingModel;
        this.documentMapper = documentMapper;
        this.documentVectorMapper = documentVectorMapper;
        this.ragProperties = ragProperties;
    }

    @Transactional
    /**
     * 将一份文档写入知识库。
     *
     * <p>处理流程：
     * 1. 标准化文本并校验空值
     * 2. 计算内容哈希，避免同一会话重复入库
     * 3. 先写 ai_document，记录入库状态
     * 4. 分块并为每个 chunk 生成 embedding
     * 5. 批量写入 ai_document_vector
     * 6. 将文档状态更新为 READY / FAILED
     */
    public DocumentIngestResult ingestDocument(DocumentIngestRequest request) {
        String normalizedText = normalizeText(request.getText());
        if (normalizedText.isEmpty()) {
            throw new IllegalArgumentException("Document text must not be empty");
        }

        String sessionId = trimToNull(request.getSessionId());
        String contentHash = sha256(normalizedText);
        // 同一会话内如果已经存在完全相同的文档内容，则直接复用已有入库结果。
        DocumentEntity existing = documentMapper.selectActiveByHashAndSessionId(sessionId, contentHash);
        if (existing != null && RagDocumentStatus.READY == existing.getStatus()) {
            return toResult(existing, true);
        }

        LocalDateTime now = LocalDateTime.now();
        DocumentEntity document = new DocumentEntity();
        document.setExternalDocumentId(trimToNull(request.getExternalDocumentId()));
        document.setSessionId(sessionId);
        document.setTitle(resolveTitle(request, normalizedText));
        document.setSourceType(resolveDocumentType(request));
        document.setSourceUri(trimToNull(request.getSourceUri()));
        document.setContentHash(contentHash);
        document.setParserVersion(PARSER_VERSION);
        document.setChunkCount(0);
        document.setStatus(RagDocumentStatus.PENDING);
        document.setMetadata(buildDocumentMetadata(request, normalizedText));
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        document.setIsActive(true);
        documentMapper.insert(document);

        try {
            document.setStatus(RagDocumentStatus.PROCESSING);
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);

            // 先按段落/句子边界分块，切不动时再按长度硬切。
            List<String> chunks = buildChunker().split(normalizedText);
            if (chunks.isEmpty()) {
                chunks = List.of(normalizedText);
            }

            List<DocumentVectorEntity> vectorEntities = new ArrayList<>();
            for (int index = 0; index < chunks.size(); index++) {
                String chunk = chunks.get(index);
                DocumentVectorEntity entity = new DocumentVectorEntity();
                entity.setKnowledgeDocumentId(document.getKnowledgeDocumentId());
                entity.setSessionId(sessionId);
                entity.setDocumentType(document.getSourceType());
                entity.setDocumentContent(normalizedText);
                entity.setChunkIndex(index);
                entity.setChunkContent(chunk);
                // Spring AI 的 embedding 模型直接返回 float[]，这里统一转成可落库的 List<Double>。
                entity.setEmbeddingVector(RagMathUtils.toDoubleList(embeddingModel.embed(chunk)));
                entity.setMetadata(buildChunkMetadata(document, request, index, chunks.size()));
                entity.setCreatedAt(now);
                entity.setUpdatedAt(now);
                entity.setIsActive(true);
                vectorEntities.add(entity);
            }

            if (!vectorEntities.isEmpty()) {
                documentVectorMapper.batchInsert(vectorEntities);
            }

            document.setChunkCount(vectorEntities.size());
            document.setStatus(RagDocumentStatus.READY);
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);
            return toResult(document, false);
        } catch (Exception exception) {
            // 失败时保留失败状态和错误信息，便于后续排查或重试。
            document.setStatus(RagDocumentStatus.FAILED);
            document.setUpdatedAt(LocalDateTime.now());
            Map<String, Object> metadata = new HashMap<>(document.getMetadata() == null ? Map.of() : document.getMetadata());
            metadata.put("errorMessage", exception.getMessage());
            document.setMetadata(metadata);
            documentMapper.updateById(document);
            throw new IllegalStateException("Failed to ingest document", exception);
        }
    }

    public List<RagHit> searchSimilar(String query, int k) {
        return searchSimilar(query, null, k);
    }

    /**
     * 检索与 query 最相近的文档分块。
     *
     * <p>这里先从数据库里按会话范围取一批候选分块，再在应用层做余弦相似度计算。
     * 这样虽然不如专业向量库高效，但实现简单，且已经具备持久化能力。
     */
    public List<RagHit> searchSimilar(String query, String sessionId, int k) {
        String normalizedQuery = normalizeText(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        // 只检索 READY 状态且仍处于激活状态的文档。
        List<DocumentVectorEntity> candidates = documentVectorMapper.selectSearchCandidates(
                trimToNull(sessionId), RagDocumentStatus.READY, ragProperties.getMaxCandidates());
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<Double> queryEmbedding = RagMathUtils.toDoubleList(embeddingModel.embed(normalizedQuery));
        int topK = k > 0 ? k : ragProperties.getDefaultTopK();

        // 先按相似度过滤，再按分数倒序截断 topK。
        List<ScoredChunk> scoredChunks = candidates.stream()
                .map(candidate -> new ScoredChunk(candidate,
                        RagMathUtils.cosineSimilarity(queryEmbedding, candidate.getEmbeddingVector())))
                .filter(scored -> scored.score >= ragProperties.getSimilarityThreshold())
                .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                .limit(topK)
                .toList();

        if (scoredChunks.isEmpty()) {
            return List.of();
        }

        Map<Long, DocumentEntity> documentsById = documentMapper.selectActiveByIds(
                        scoredChunks.stream()
                                .map(scored -> scored.chunk.getKnowledgeDocumentId())
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList())
                .stream()
                .collect(Collectors.toMap(DocumentEntity::getKnowledgeDocumentId, value -> value));

        return scoredChunks.stream()
                .map(scored -> toHit(scored.chunk, scored.score, documentsById.get(scored.chunk.getKnowledgeDocumentId())))
                .toList();
    }

    public List<DocumentIngestResult> listDocuments(String sessionId) {
        return documentMapper.selectBySessionIdOrGlobal(trimToNull(sessionId)).stream()
                .map(document -> toResult(document, false))
                .toList();
    }

    public DocumentIngestResult getDocument(Long knowledgeDocumentId) {
        DocumentEntity entity = documentMapper.selectById(knowledgeDocumentId);
        if (entity == null || !Boolean.TRUE.equals(entity.getIsActive())) {
            return null;
        }
        return toResult(entity, false);
    }

    @Transactional
    public boolean deactivateDocument(Long knowledgeDocumentId) {
        DocumentEntity entity = documentMapper.selectById(knowledgeDocumentId);
        if (entity == null || !Boolean.TRUE.equals(entity.getIsActive())) {
            return false;
        }
        documentMapper.deactivateByKnowledgeDocumentId(knowledgeDocumentId);
        documentVectorMapper.deactivateByKnowledgeDocumentId(knowledgeDocumentId);
        return true;
    }

    /**
     * 将数据库实体转成 API 返回对象，避免把表结构直接暴露到接口层。
     */
    private DocumentIngestResult toResult(DocumentEntity document, boolean reused) {
        DocumentIngestResult result = new DocumentIngestResult();
        result.setKnowledgeDocumentId(document.getKnowledgeDocumentId());
        result.setExternalDocumentId(document.getExternalDocumentId());
        result.setSessionId(document.getSessionId());
        result.setTitle(document.getTitle());
        result.setDocumentType(document.getSourceType());
        result.setSourceUri(document.getSourceUri());
        result.setStatus(document.getStatus());
        result.setChunkCount(document.getChunkCount());
        result.setReused(reused);
        result.setCreatedAt(document.getCreatedAt());
        result.setUpdatedAt(document.getUpdatedAt());
        return result;
    }

    /**
     * 将检索得到的 chunk 和文档主表信息组装成结构化命中结果。
     */
    private RagHit toHit(DocumentVectorEntity entity, double score, DocumentEntity document) {
        RagHit hit = new RagHit();
        hit.setKnowledgeDocumentId(entity.getKnowledgeDocumentId());
        hit.setTitle(document != null ? document.getTitle() : null);
        hit.setDocumentType(entity.getDocumentType());
        hit.setChunkContent(entity.getChunkContent());
        hit.setScore(score);
        hit.setMetadata(entity.getMetadata());
        return hit;
    }

    /**
     * 文档级元数据：描述整篇文档的来源和基础属性。
     */
    private Map<String, Object> buildDocumentMetadata(DocumentIngestRequest request, String text) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (request.getMetadata() != null) {
            metadata.putAll(request.getMetadata());
        }
        metadata.put("title", resolveTitle(request, text));
        metadata.put("documentType", resolveDocumentType(request));
        metadata.put("sourceUri", trimToNull(request.getSourceUri()));
        metadata.put("textLength", text.length());
        return metadata;
    }

    /**
     * 分块级元数据：描述当前 chunk 在整篇文档中的位置。
     */
    private Map<String, Object> buildChunkMetadata(DocumentEntity document,
                                                   DocumentIngestRequest request,
                                                   int chunkIndex,
                                                   int totalChunks) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        if (request.getMetadata() != null) {
            metadata.putAll(request.getMetadata());
        }
        metadata.put("knowledgeDocumentId", document.getKnowledgeDocumentId());
        metadata.put("title", document.getTitle());
        metadata.put("sourceUri", document.getSourceUri());
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("totalChunks", totalChunks);
        return metadata;
    }

    private TextChunker buildChunker() {
        return new TextChunker(ragProperties.getChunkSize(), ragProperties.getChunkOverlap());
    }

    /**
     * 标题优先级：
     * 显式 title > externalDocumentId > 文本前 60 个字符。
     */
    private String resolveTitle(DocumentIngestRequest request, String text) {
        if (StringUtils.isNotBlank(request.getTitle())) {
            return request.getTitle().trim();
        }
        if (StringUtils.isNotBlank(request.getExternalDocumentId())) {
            return request.getExternalDocumentId().trim();
        }
        String singleLine = text.replace('\n', ' ').trim();
        return singleLine.substring(0, Math.min(singleLine.length(), 60));
    }

    private String resolveDocumentType(DocumentIngestRequest request) {
        return StringUtils.defaultIfBlank(trimToNull(request.getDocumentType()), "text");
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

    /**
     * 使用内容哈希做轻量去重，不依赖外部 documentId 是否稳定。
     */
    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte value : bytes) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 algorithm is unavailable", exception);
        }
    }

    /**
     * 检索过程中的临时对象，只在内存里保存 chunk 和相似度分数。
     */
    private record ScoredChunk(DocumentVectorEntity chunk, double score) {
    }
}
