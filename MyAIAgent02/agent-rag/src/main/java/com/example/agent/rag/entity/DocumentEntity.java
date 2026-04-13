package com.example.agent.rag.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.agent.rag.enums.RagDocumentStatus;
import com.example.agent.rag.handle.FastJsonTypeHandler;
import org.apache.ibatis.type.EnumTypeHandler;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@TableName(value = "ai_document", autoResultMap = true)
public class DocumentEntity implements Serializable {

    @TableId
    private Long knowledgeDocumentId;

    private String externalDocumentId;

    private String sessionId;

    private String title;

    private String sourceType;

    private String sourceUri;

    private String contentHash;

    private String parserVersion;

    private Integer chunkCount;

    @TableField(typeHandler = EnumTypeHandler.class)
    private RagDocumentStatus status;

    @TableField(typeHandler = FastJsonTypeHandler.class)
    private Map<String, Object> metadata;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Boolean isActive = true;

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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceUri() {
        return sourceUri;
    }

    public void setSourceUri(String sourceUri) {
        this.sourceUri = sourceUri;
    }

    public String getContentHash() {
        return contentHash;
    }

    public void setContentHash(String contentHash) {
        this.contentHash = contentHash;
    }

    public String getParserVersion() {
        return parserVersion;
    }

    public void setParserVersion(String parserVersion) {
        this.parserVersion = parserVersion;
    }

    public Integer getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(Integer chunkCount) {
        this.chunkCount = chunkCount;
    }

    public RagDocumentStatus getStatus() {
        return status;
    }

    public void setStatus(RagDocumentStatus status) {
        this.status = status;
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
}
