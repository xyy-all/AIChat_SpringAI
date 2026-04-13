package com.example.agent.core.dto;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

public class SessionMetadata {
    private String sessionId;
    private String title;
    private LocalDateTime createdAt;
    private LocalDateTime lastActiveAt;
    private int messageCount;
    private Map<String, Object> customAttributes;

    public SessionMetadata() {
        this.customAttributes = new HashMap<>();
    }

    public SessionMetadata(String sessionId, String title) {
        this();
        this.sessionId = sessionId;
        this.title = title;
        this.createdAt = LocalDateTime.now();
        this.lastActiveAt = LocalDateTime.now();
        this.messageCount = 0;
    }

    public SessionMetadata(String sessionId, String title, LocalDateTime createdAt,
                          LocalDateTime lastActiveAt, int messageCount) {
        this.sessionId = sessionId;
        this.title = title;
        this.createdAt = createdAt;
        this.lastActiveAt = lastActiveAt;
        this.messageCount = messageCount;
        this.customAttributes = new HashMap<>();
    }

    // Getters and setters
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

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }

    public Map<String, Object> getCustomAttributes() {
        return customAttributes;
    }

    public void setCustomAttributes(Map<String, Object> customAttributes) {
        this.customAttributes = customAttributes;
    }

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
     * 添加自定义属性
     */
    public void addCustomAttribute(String key, Object value) {
        this.customAttributes.put(key, value);
    }

    /**
     * 获取自定义属性
     */
    public Object getCustomAttribute(String key) {
        return this.customAttributes.get(key);
    }

    /**
     * 移除自定义属性
     */
    public void removeCustomAttribute(String key) {
        this.customAttributes.remove(key);
    }

    @Override
    public String toString() {
        return "SessionMetadata{" +
                "sessionId='" + sessionId + '\'' +
                ", title='" + title + '\'' +
                ", createdAt=" + createdAt +
                ", lastActiveAt=" + lastActiveAt +
                ", messageCount=" + messageCount +
                ", customAttributes=" + customAttributes +
                '}';
    }
}
