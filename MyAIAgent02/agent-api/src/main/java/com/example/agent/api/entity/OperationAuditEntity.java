package com.example.agent.api.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.type.Alias;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 操作审计实体类
 * 对应表：ai_operation_audit
 */
@TableName("ai_operation_audit")
public class OperationAuditEntity implements Serializable {

    private Long auditId;  // 审计ID

    private String operationType;  // 操作类型

    private String sessionId;  // 关联的会话ID

    private String userId;  // 操作用户ID

    @TableField(typeHandler = com.example.agent.core.handle.FastJsonTypeHandler.class)
    private Map<String, Object> operationDetails;  // 操作详情，JSON格式

    private String ipAddress;  // 操作IP地址

    private String userAgent;  // 用户代理

    private LocalDateTime createdAt;  // 操作时间

    // 构造函数
    public OperationAuditEntity() {
    }

    public OperationAuditEntity(String operationType, String sessionId, String userId,
                                Map<String, Object> operationDetails, String ipAddress, String userAgent) {
        this.operationType = operationType;
        this.sessionId = sessionId;
        this.userId = userId;
        this.operationDetails = operationDetails;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    // Getters and Setters

    public Long getAuditId() {
        return auditId;
    }

    public void setAuditId(Long auditId) {
        this.auditId = auditId;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Map<String, Object> getOperationDetails() {
        return operationDetails;
    }

    public void setOperationDetails(Map<String, Object> operationDetails) {
        this.operationDetails = operationDetails;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return "OperationAuditEntity{" +
                "auditId=" + auditId +
                ", operationType='" + operationType + '\'' +
                ", sessionId='" + sessionId + '\'' +
                ", userId='" + userId + '\'' +
                ", ipAddress='" + ipAddress + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}