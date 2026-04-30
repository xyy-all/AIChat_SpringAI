package com.example.agent.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.agent.rag.config.RagProperties;
import com.example.agent.rag.dto.DocumentIngestRequest;
import com.example.agent.rag.dto.DocumentIngestResult;
import com.example.agent.rag.dto.RagHit;
import com.example.agent.rag.dto.VectorStoreReindexResult;
import com.example.agent.rag.entity.DocumentEntity;
import com.example.agent.rag.entity.DocumentVectorEntity;
import com.example.agent.rag.enums.RagDocumentStatus;
import com.example.agent.rag.mapper.DocumentMapper;
import com.example.agent.rag.mapper.DocumentVectorMapper;
import com.example.agent.rag.store.RagVectorStoreBackend;
import com.example.agent.rag.support.RagMathUtils;
import com.example.agent.rag.support.TextChunker;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RagServiceImpl implements RagService {

    private static final Logger logger = LoggerFactory.getLogger(RagServiceImpl.class);
    private static final String PARSER_VERSION = "text-v1";
    private static final String DATABASE_BACKEND_ID = "database";
    private static final String QDRANT_BACKEND_ID = "qdrant";

    private final EmbeddingModel embeddingModel;
    private final DocumentMapper documentMapper;
    private final DocumentVectorMapper documentVectorMapper;
    private final RagProperties ragProperties;
    private final Map<String, RagVectorStoreBackend> vectorStoreBackends;

    public RagServiceImpl(EmbeddingModel embeddingModel,
                          DocumentMapper documentMapper,
                          DocumentVectorMapper documentVectorMapper,
                          RagProperties ragProperties,
                          List<RagVectorStoreBackend> vectorStoreBackends) {
        this.embeddingModel = embeddingModel;
        this.documentMapper = documentMapper;
        this.documentVectorMapper = documentVectorMapper;
        this.ragProperties = ragProperties;
        this.vectorStoreBackends = vectorStoreBackends.stream()
                .collect(Collectors.toMap(RagVectorStoreBackend::getBackendId, Function.identity()));
    }

    @Override
    @Transactional
    public DocumentIngestResult ingestDocument(DocumentIngestRequest request) {
        String normalizedText = normalizeText(request.getText());
        if (normalizedText.isEmpty()) {
            throw new IllegalArgumentException("Document text must not be empty");
        }

        String sessionId = trimToNull(request.getSessionId());
        String contentHash = sha256(normalizedText);
        DocumentEntity existing = documentMapper.selectActiveByHashAndSessionId(sessionId, contentHash);
        if (existing != null && existing.getStatus() == RagDocumentStatus.READY) {
            return toResult(existing, true);
        }

        LocalDateTime now = LocalDateTime.now();
        DocumentEntity document = buildDocumentEntity(request, normalizedText, sessionId, contentHash, now);
        documentMapper.insert(document);

        try {
            document.setStatus(RagDocumentStatus.PROCESSING);
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);

            List<DocumentVectorEntity> vectorEntities = buildVectorEntities(document, request, normalizedText, now);
            if (!vectorEntities.isEmpty()) {
                documentVectorMapper.batchInsert(vectorEntities);
            }

            resolveVectorStoreBackend().indexDocument(document, vectorEntities);

            document.setChunkCount(vectorEntities.size());
            document.setStatus(RagDocumentStatus.READY);
            document.setUpdatedAt(LocalDateTime.now());
            documentMapper.updateById(document);
            return toResult(document, false);
        } catch (Exception exception) {
            markDocumentFailed(document, exception);
            throw new IllegalStateException("Failed to ingest document", exception);
        }
    }

    @Override
    public List<RagHit> searchSimilar(String query, int k) {
        return searchSimilar(query, null, k);
    }

    @Override
    public List<RagHit> searchSimilar(String query, String sessionId, int k) {
        // 第一步：让当前激活的向量检索后端只负责“召回最相似的 chunk”。
        // 这里关注的是语义相似度，不直接承担完整业务校验职责。
        List<RagHit> hits = resolveVectorStoreBackend().searchSimilar(query, sessionId, k);
        if (hits.isEmpty()) {
            return List.of();
        }

        // 第二步：回到 ai_document 主表按文档 ID 做一次业务层校验。
        // 原因是向量库或 chunk 表只能保证“命中相似片段”，但不能天然保证：
        // 1. 文档当前仍然处于可用状态
        // 2. 文档没有被逻辑删除或停用
        // 3. 标题、类型等主数据仍然是最新值
        // 所以这里始终以业务主表作为最终真相源。
        Map<Long, DocumentEntity> documentsById = loadActiveDocuments(hits.stream()
                .map(RagHit::getKnowledgeDocumentId)
                .filter(Objects::nonNull)
                .distinct()
                .toList());

        // 第三步：过滤掉业务上已经不可用的命中，并用主表数据补全返回结果。
        // 这样最终返回给上层的是“既相似，又在业务上允许使用”的结果。
        return hits.stream()
                .filter(hit -> hit.getKnowledgeDocumentId() == null || documentsById.containsKey(hit.getKnowledgeDocumentId()))
                .map(hit -> enrichHit(hit, documentsById.get(hit.getKnowledgeDocumentId())))
                .toList();
    }

    @Override
    public List<DocumentIngestResult> listDocuments(String sessionId) {
        return documentMapper.selectBySessionIdOrGlobal(trimToNull(sessionId)).stream()
                .map(document -> toResult(document, false))
                .toList();
    }

    @Override
    public DocumentIngestResult getDocument(Long knowledgeDocumentId) {
        DocumentEntity entity = documentMapper.selectById(knowledgeDocumentId);
        if (entity == null || !Boolean.TRUE.equals(entity.getIsActive())) {
            return null;
        }
        return toResult(entity, false);
    }

    @Override
    @Transactional
    public boolean deactivateDocument(Long knowledgeDocumentId) {
        DocumentEntity entity = documentMapper.selectById(knowledgeDocumentId);
        if (entity == null || !Boolean.TRUE.equals(entity.getIsActive())) {
            return false;
        }

        documentMapper.deactivateByKnowledgeDocumentId(knowledgeDocumentId);
        documentVectorMapper.deactivateByKnowledgeDocumentId(knowledgeDocumentId);
        resolveVectorStoreBackend().deleteDocument(knowledgeDocumentId);
        return true;
    }

    @Override
    public VectorStoreReindexResult reindexActiveDocuments() {
        RagVectorStoreBackend backend = resolveVectorStoreBackend();
        VectorStoreReindexResult result = new VectorStoreReindexResult();
        result.setBackend(backend.getBackendId());

        if (DATABASE_BACKEND_ID.equals(backend.getBackendId())) {
            return result;
        }

        List<DocumentEntity> documents = documentMapper.selectList(new LambdaQueryWrapper<DocumentEntity>()
                .eq(DocumentEntity::getIsActive, true)
                .eq(DocumentEntity::getStatus, RagDocumentStatus.READY)
                .orderByAsc(DocumentEntity::getKnowledgeDocumentId));
        result.setDocumentsScanned(documents.size());

        for (DocumentEntity document : documents) {
            List<DocumentVectorEntity> chunks = documentVectorMapper.selectList(new LambdaQueryWrapper<DocumentVectorEntity>()
                    .eq(DocumentVectorEntity::getKnowledgeDocumentId, document.getKnowledgeDocumentId())
                    .eq(DocumentVectorEntity::getIsActive, true)
                    .orderByAsc(DocumentVectorEntity::getChunkIndex));

            if (chunks.isEmpty()) {
                result.setSkippedDocuments(result.getSkippedDocuments() + 1);
                continue;
            }

            try {
                backend.indexDocument(document, chunks);
                result.setDocumentsIndexed(result.getDocumentsIndexed() + 1);
                result.setChunksIndexed(result.getChunksIndexed() + chunks.size());
            } catch (Exception exception) {
                logger.error("Failed to reindex document {}", document.getKnowledgeDocumentId(), exception);
                List<Long> failed = new ArrayList<>(result.getFailedDocumentIds());
                failed.add(document.getKnowledgeDocumentId());
                result.setFailedDocumentIds(failed);
            }
        }

        return result;
    }

    private DocumentEntity buildDocumentEntity(DocumentIngestRequest request,
                                               String normalizedText,
                                               String sessionId,
                                               String contentHash,
                                               LocalDateTime now) {
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
        return document;
    }

    private List<DocumentVectorEntity> buildVectorEntities(DocumentEntity document,
                                                           DocumentIngestRequest request,
                                                           String normalizedText,
                                                           LocalDateTime now) {
        List<String> chunks = buildChunker().split(normalizedText);
        if (chunks.isEmpty()) {
            chunks = List.of(normalizedText);
        }

        List<DocumentVectorEntity> vectorEntities = new ArrayList<>();
        for (int index = 0; index < chunks.size(); index++) {
            String chunk = chunks.get(index);
            DocumentVectorEntity entity = new DocumentVectorEntity();
            entity.setKnowledgeDocumentId(document.getKnowledgeDocumentId());
            entity.setSessionId(document.getSessionId());
            entity.setDocumentType(document.getSourceType());
            entity.setDocumentContent(normalizedText);
            entity.setChunkIndex(index);
            entity.setChunkContent(chunk);
            entity.setEmbeddingVector(RagMathUtils.toDoubleList(embeddingModel.embed(chunk)));
            entity.setMetadata(buildChunkMetadata(document, request, index, chunks.size()));
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setIsActive(true);
            vectorEntities.add(entity);
        }
        return vectorEntities;
    }

    private RagVectorStoreBackend resolveVectorStoreBackend() {
        RagProperties.Provider provider = ragProperties.getVectorStore().getProvider();
        if (provider == RagProperties.Provider.QDRANT) {
            RagVectorStoreBackend qdrantBackend = vectorStoreBackends.get(QDRANT_BACKEND_ID);
            if (qdrantBackend != null && qdrantBackend.isAvailable()) {
                return qdrantBackend;
            }
            if (ragProperties.getVectorStore().isLocalFallbackEnabled()) {
                logger.warn("Qdrant backend is unavailable, falling back to database vector search");
                return requireDatabaseBackend();
            }
            throw new IllegalStateException("Qdrant backend is required but unavailable");
        }
        return requireDatabaseBackend();
    }

    private RagVectorStoreBackend requireDatabaseBackend() {
        RagVectorStoreBackend backend = vectorStoreBackends.get(DATABASE_BACKEND_ID);
        if (backend == null) {
            throw new IllegalStateException("Database fallback vector backend is not configured");
        }
        return backend;
    }

    private Map<Long, DocumentEntity> loadActiveDocuments(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        return documentMapper.selectActiveByIds(ids).stream()
                .collect(Collectors.toMap(DocumentEntity::getKnowledgeDocumentId, Function.identity()));
    }

    private RagHit enrichHit(RagHit hit, DocumentEntity document) {
        if (document == null) {
            return hit;
        }
        hit.setTitle(document.getTitle());
        if (StringUtils.isBlank(hit.getDocumentType())) {
            hit.setDocumentType(document.getSourceType());
        }
        return hit;
    }

    private void markDocumentFailed(DocumentEntity document, Exception exception) {
        document.setStatus(RagDocumentStatus.FAILED);
        document.setUpdatedAt(LocalDateTime.now());
        Map<String, Object> metadata = new HashMap<>(document.getMetadata() == null ? Map.of() : document.getMetadata());
        metadata.put("errorMessage", exception.getMessage());
        document.setMetadata(metadata);
        documentMapper.updateById(document);
    }

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
}
