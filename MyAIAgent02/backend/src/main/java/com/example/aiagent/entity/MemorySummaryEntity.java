package com.example.aiagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.type.Alias;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 记忆摘要表实体类
 * 对应表：ai_memory_summary
 */
@TableName("ai_memory_summary")
public class MemorySummaryEntity implements Serializable {

    private Long memoryId;

    private String sessionId;

    private String summaryType;

    private String summaryContent;

    private List<Double> embeddingVector;

    private Double importanceScore = 0.5;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime expiresAt;

    private Map<String, Object> metadata;

    private Boolean isDeleted = false;  // 是否删除

    // 外键关系（可选，用于查询时关联）
    private ConversationSessionEntity session;

    // 构造函数
    public MemorySummaryEntity() {
    }

    public MemorySummaryEntity(String sessionId, String summaryType, String summaryContent) {
        this.sessionId = sessionId;
        this.summaryType = summaryType;
        this.summaryContent = summaryContent;
    }

    public MemorySummaryEntity(String sessionId, String summaryType, String summaryContent, Double importanceScore) {
        this.sessionId = sessionId;
        this.summaryType = summaryType;
        this.summaryContent = summaryContent;
        this.importanceScore = importanceScore;
    }

    // Getters and Setters

    public Long getMemoryId() {
        return memoryId;
    }

    public void setMemoryId(Long memoryId) {
        this.memoryId = memoryId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSummaryType() {
        return summaryType;
    }

    public void setSummaryType(String summaryType) {
        this.summaryType = summaryType;
    }

    public String getSummaryContent() {
        return summaryContent;
    }

    public void setSummaryContent(String summaryContent) {
        this.summaryContent = summaryContent;
    }

    public List<Double> getEmbeddingVector() {
        return embeddingVector;
    }

    public void setEmbeddingVector(List<Double> embeddingVector) {
        this.embeddingVector = embeddingVector;
    }

    public Double getImportanceScore() {
        return importanceScore;
    }

    public void setImportanceScore(Double importanceScore) {
        this.importanceScore = importanceScore;
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

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public Boolean getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Boolean isDeleted) {
        this.isDeleted = isDeleted;
    }

    public ConversationSessionEntity getSession() {
        return session;
    }

    public void setSession(ConversationSessionEntity session) {
        this.session = session;
    }

    // 业务方法

    /**
     * 检查摘要是否过期
     */
    public boolean isExpired() {
        return this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt);
    }

    /**
     * 检查摘要是否即将过期（在指定天数内）
     */
    public boolean isExpiringSoon(int days) {
        if (this.expiresAt == null) {
            return false;
        }
        LocalDateTime threshold = LocalDateTime.now().plusDays(days);
        return this.expiresAt.isBefore(threshold) && !isExpired();
    }

    /**
     * 设置过期时间（从当前时间开始的天数）
     */
    public void setExpiresInDays(int days) {
        this.expiresAt = LocalDateTime.now().plusDays(days);
    }

    /**
     * 更新重要性评分
     */
    public void updateImportanceScore(Double newScore) {
        this.importanceScore = newScore;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 检查是否是重要记忆（评分高于阈值）
     */
    public boolean isImportant(double threshold) {
        return this.importanceScore != null && this.importanceScore >= threshold;
    }

    /**
     * 获取摘要类型枚举
     */
    public SummaryType getSummaryTypeEnum() {
        try {
            return SummaryType.valueOf(this.summaryType.toUpperCase());
        } catch (IllegalArgumentException e) {
            return SummaryType.CONVERSATION_SUMMARY;
        }
    }

    /**
     * 摘要类型枚举
     */
    public enum SummaryType {
        CONVERSATION_SUMMARY("conversation_summary"),
        ENTITY_MEMORY("entity_memory"),
        USER_PREFERENCE("user_preference"),
        KEY_DECISION("key_decision"),
        ACTION_ITEM("action_item"),
        OTHER("other");

        private final String value;

        SummaryType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static SummaryType fromValue(String value) {
            for (SummaryType type : values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return OTHER;
        }
    }

    /**
     * 设置摘要类型枚举
     */
    public void setSummaryType(SummaryType summaryType) {
        this.summaryType = summaryType.getValue();
    }

    @Override
    public String toString() {
        return "MemorySummaryEntity{" +
                "memoryId=" + memoryId +
                ", sessionId='" + sessionId + '\'' +
                ", summaryType='" + summaryType + '\'' +
                ", summaryContent='" + (summaryContent != null ? summaryContent.substring(0, Math.min(summaryContent.length(), 50)) + "..." : "null") + '\'' +
                ", importanceScore=" + importanceScore +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                '}';
    }
}