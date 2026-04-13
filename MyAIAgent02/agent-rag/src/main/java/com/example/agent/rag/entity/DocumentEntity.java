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

/**
 * 文档主表实体，对应 ai_document。
 *
 * <p>一条记录表示“一篇文档”的生命周期和元信息，
 * 真正用于检索的分块和向量落在 ai_document_vector。
 */
@TableName(value = "ai_document", autoResultMap = true)
public class DocumentEntity implements Serializable {

    @TableId
    /** 主键，系统内部文档 ID。 */
    private Long knowledgeDocumentId;

    /** 业务侧传入的文档 ID，可为空。 */
    private String externalDocumentId;

    /** 所属会话；为空表示全局知识。 */
    private String sessionId;

    /** 展示用标题。 */
    private String title;

    /** 文档来源类型，例如 text / pdf / url。 */
    private String sourceType;

    /** 文档来源地址或来源标识。 */
    private String sourceUri;

    /** 文档正文哈希，用于轻量去重。 */
    private String contentHash;

    /** 当前文档解析器版本，便于后续重建索引。 */
    private String parserVersion;

    /** 文档被切成了多少个 chunk。 */
    private Integer chunkCount;

    @TableField(typeHandler = EnumTypeHandler.class)
    /** 入库状态，统一使用枚举而不是裸字符串。 */
    private RagDocumentStatus status;

    @TableField(typeHandler = FastJsonTypeHandler.class)
    /** 文档级元数据。 */
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
