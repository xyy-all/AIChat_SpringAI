package com.example.agent.rag.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.agent.rag.handle.FastJsonTypeHandler;
import com.example.agent.rag.handle.ListDoubleTypeHandler;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 文档分块表实体，对应 ai_document_vector。
 *
 * <p>一条记录表示一个 chunk：
 * 包含 chunk 文本、向量、顺序位置以及少量检索辅助元数据。
 */
@TableName("ai_document_vector")
public class DocumentVectorEntity implements Serializable {

    /** 分块主键。 */
    private Long documentId;

    /** 关联 ai_document 的主键。 */
    private Long knowledgeDocumentId;

    /** 所属会话；为空表示全局知识。 */
    private String sessionId;

    /** 冗余保存来源类型，便于过滤和分析。 */
    private String documentType;

    /** 冗余保存原文，便于后续排查和重建。 */
    private String documentContent;

    /** 当前 chunk 在整篇文档中的顺序。 */
    private Integer chunkIndex;

    /** 分块文本。 */
    private String chunkContent;

    @TableField(typeHandler = ListDoubleTypeHandler.class)
    /** 向量化结果。 */
    private List<Double> embeddingVector;

    @TableField(typeHandler = FastJsonTypeHandler.class)
    /** 分块级元数据。 */
    private Map<String, Object> metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean isActive = true;

    public DocumentVectorEntity() {
    }

    /**
     * 简化构造器，主要用于测试或手动构造简单 chunk。
     */
    public DocumentVectorEntity(String sessionId, String documentType, String documentContent,
                                String chunkContent, List<Double> embeddingVector) {
        this.sessionId = sessionId;
        this.documentType = documentType;
        this.documentContent = documentContent;
        this.chunkContent = chunkContent;
        this.embeddingVector = embeddingVector;
        this.chunkIndex = 0;
    }

    public Long getDocumentId() {
        return documentId;
    }

    public void setDocumentId(Long documentId) {
        this.documentId = documentId;
    }

    public Long getKnowledgeDocumentId() {
        return knowledgeDocumentId;
    }

    public void setKnowledgeDocumentId(Long knowledgeDocumentId) {
        this.knowledgeDocumentId = knowledgeDocumentId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentContent() {
        return documentContent;
    }

    public void setDocumentContent(String documentContent) {
        this.documentContent = documentContent;
    }

    public Integer getChunkIndex() {
        return chunkIndex;
    }

    public void setChunkIndex(Integer chunkIndex) {
        this.chunkIndex = chunkIndex;
    }

    public String getChunkContent() {
        return chunkContent;
    }

    public void setChunkContent(String chunkContent) {
        this.chunkContent = chunkContent;
    }

    public List<Double> getEmbeddingVector() {
        return embeddingVector;
    }

    public void setEmbeddingVector(List<Double> embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void activate() {
        this.isActive = true;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(isActive);
    }

    @Override
    public String toString() {
        return "DocumentVectorEntity{" +
                "documentId=" + documentId +
                ", knowledgeDocumentId=" + knowledgeDocumentId +
                ", sessionId='" + sessionId + '\'' +
                ", documentType='" + documentType + '\'' +
                ", chunkIndex=" + chunkIndex +
                ", chunkContent='" + (chunkContent != null
                ? chunkContent.substring(0, Math.min(chunkContent.length(), 50)) + "..."
                : "null") + '\'' +
                ", embeddingVectorSize=" + (embeddingVector != null ? embeddingVector.size() : 0) +
                ", isActive=" + isActive +
                '}';
    }
}
