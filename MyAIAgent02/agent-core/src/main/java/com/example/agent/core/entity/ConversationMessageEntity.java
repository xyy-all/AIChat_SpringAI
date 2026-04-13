package com.example.agent.core.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.agent.core.enums.MessageRole;
import org.apache.ibatis.type.Alias;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 消息表实体类
 * 对应表：ai_conversation_message
 */
@TableName("ai_conversation_message")
public class ConversationMessageEntity implements Serializable {

    private Long messageId;  // 主键ID

    private String sessionId;  // 会话ID

    private Integer messageIndex;  // 消息索引

    private MessageRole role = MessageRole.USER;  // 角色

    private String content;  // 内容

    private Integer tokens;  // token数

    private String model;  // 模型

    private Double temperature;  // 温度

    private LocalDateTime createdAt;  // 创建时间

    @TableField(typeHandler = com.example.agent.core.handle.FastJsonTypeHandler.class)
    private Map<String, Object> metadata;  // 元数据

    private Boolean isDeleted = false;  // 是否删除

    // 外键关系（可选，用于查询时关联）
    private ConversationSessionEntity session;  // 会话

    // 构造函数
    public ConversationMessageEntity() {
    }

    public ConversationMessageEntity(String sessionId, Integer messageIndex, MessageRole role, String content) {
        this.sessionId = sessionId;
        this.messageIndex = messageIndex;
        this.role = role;
        this.content = content;
    }

    // Getters and Setters

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getMessageIndex() {
        return messageIndex;
    }

    public void setMessageIndex(Integer messageIndex) {
        this.messageIndex = messageIndex;
    }

    public MessageRole getRole() {
        return role;
    }

    public void setRole(MessageRole role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getTokens() {
        return tokens;
    }

    public void setTokens(Integer tokens) {
        this.tokens = tokens;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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
     * 检查消息是否来自用户
     */
    public boolean isUserMessage() {
        return this.role == MessageRole.USER;
    }

    /**
     * 检查消息是否来自助手
     */
    public boolean isAssistantMessage() {
        return this.role == MessageRole.ASSISTANT;
    }

    /**
     * 检查消息是否来自系统
     */
    public boolean isSystemMessage() {
        return this.role == MessageRole.SYSTEM;
    }

    /**
     * 检查消息是否来自工具
     */
    public boolean isToolMessage() {
        return this.role == MessageRole.TOOL;
    }

    /**
     * 逻辑删除消息
     */
    public void markAsDeleted() {
        this.isDeleted = true;
    }

    /**
     * 恢复已删除的消息
     */
    public void restore() {
        this.isDeleted = false;
    }

    /**
     * 获取简化的角色字符串（小写）
     */
    public String getRoleString() {
        return this.role.name();
    }

    @Override
    public String toString() {
        return "ConversationMessageEntity{" +
                "messageId=" + messageId +
                ", sessionId='" + sessionId + '\'' +
                ", messageIndex=" + messageIndex +
                ", role=" + role +
                ", content='" + (content != null ? content.substring(0, Math.min(content.length(), 50)) + "..." : "null") + '\'' +
                ", createdAt=" + createdAt +
                ", isDeleted=" + isDeleted +
                '}';
    }
}
