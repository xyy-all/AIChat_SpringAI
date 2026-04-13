package com.example.aiagent.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.type.Alias;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 会话表实体类
 * 对应表：ai_conversation_session
 */
@TableName("ai_conversation_session")
public class ConversationSessionEntity implements Serializable {

    private String sessionId;

    private String title = "新对话";

    private LocalDateTime createdAt;

    private LocalDateTime lastActiveAt;

    private Integer messageCount = 0;

    /**
     * 会话状态：
     * 1-活跃，2-归档，3-删除（逻辑删除），4-隐藏
     */
    private Integer status = 1;

    /**
     * 存储层级：
     * 1-内存（近期活跃），2-数据库（完整存储），3-归档（冷存储）
     */
    private Integer storageLevel = 1;

    private String userId;

    private String model;

    private Double temperature;

    private Integer maxTokens;

    private Map<String, Object> customAttributes;

    private Map<String, Object> metadata;

    private Boolean isDeleted = false;  // 是否删除

    // 构造函数
    public ConversationSessionEntity() {
    }

    public ConversationSessionEntity(String sessionId, String title) {
        this.sessionId = sessionId;
        this.title = title;
    }

    // Getters and Setters

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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(Integer messageCount) {
        this.messageCount = messageCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getStorageLevel() {
        return storageLevel;
    }

    public void setStorageLevel(Integer storageLevel) {
        this.storageLevel = storageLevel;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
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

    // 业务方法

    /**
     * 更新最后活跃时间
     */
    public void updateLastActive() {
        this.lastActiveAt = LocalDateTime.now();
    }

    /**
     * 增加消息计数
     */
    public void incrementMessageCount() {
        this.messageCount++;
    }

    /**
     * 减少消息计数
     */
    public void decrementMessageCount() {
        if (this.messageCount > 0) {
            this.messageCount--;
        }
    }

    /**
     * 检查会话是否活跃
     */
    public boolean isActive() {
        return this.status == 1;
    }

    /**
     * 检查会话是否已删除（逻辑删除）
     */
    public boolean isDeleted() {
        return this.status == 3;
    }

    /**
     * 检查会话是否归档
     */
    public boolean isArchived() {
        return this.status == 2;
    }

    /**
     * 检查会话是否隐藏
     */
    public boolean isHidden() {
        return this.status == 4;
    }

    /**
     * 检查是否存储在内存中
     */
    public boolean isInMemoryStorage() {
        return this.storageLevel == 1;
    }

    /**
     * 检查是否存储在数据库中
     */
    public boolean isInDatabaseStorage() {
        return this.storageLevel == 2;
    }

    /**
     * 检查是否归档存储
     */
    public boolean isInArchiveStorage() {
        return this.storageLevel == 3;
    }

    @Override
    public String toString() {
        return "ConversationSessionEntity{" +
                "sessionId='" + sessionId + '\'' +
                ", title='" + title + '\'' +
                ", createdAt=" + createdAt +
                ", lastActiveAt=" + lastActiveAt +
                ", messageCount=" + messageCount +
                ", status=" + status +
                ", storageLevel=" + storageLevel +
                ", userId='" + userId + '\'' +
                '}';
    }
}