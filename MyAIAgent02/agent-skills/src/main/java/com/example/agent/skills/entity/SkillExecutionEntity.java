package com.example.agent.skills.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.type.Alias;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 技能执行记录实体类
 * 对应表：ai_skill_execution
 */
@TableName("ai_skill_execution")
public class SkillExecutionEntity implements Serializable {

    private Long executionId;  // 执行记录ID

    private String sessionId;  // 关联的会话ID

    private String skillName;  // 技能名称

    private String skillInput;  // 技能输入

    private String skillOutput;  // 技能输出

    private Integer executionTimeMs;  // 执行耗时（毫秒）

    private Integer status = 1;  // 执行状态：1-成功，2-失败，3-部分成功

    private String errorMessage;  // 错误信息

    private LocalDateTime createdAt;  // 执行时间

    @TableField(typeHandler = com.example.agent.skills.handle.FastJsonTypeHandler.class)
    private Map<String, Object> metadata;  // 执行元数据

    // 构造函数
    public SkillExecutionEntity() {
    }

    public SkillExecutionEntity(String sessionId, String skillName, String skillInput, String skillOutput) {
        this.sessionId = sessionId;
        this.skillName = skillName;
        this.skillInput = skillInput;
        this.skillOutput = skillOutput;
        this.status = 1;
    }

    // Getters and Setters

    public Long getExecutionId() {
        return executionId;
    }

    public void setExecutionId(Long executionId) {
        this.executionId = executionId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getSkillInput() {
        return skillInput;
    }

    public void setSkillInput(String skillInput) {
        this.skillInput = skillInput;
    }

    public String getSkillOutput() {
        return skillOutput;
    }

    public void setSkillOutput(String skillOutput) {
        this.skillOutput = skillOutput;
    }

    public Integer getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(Integer executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
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

    // 业务方法

    /**
     * 标记为成功
     */
    public void markAsSuccess() {
        this.status = 1;
    }

    /**
     * 标记为失败
     */
    public void markAsFailed(String errorMessage) {
        this.status = 2;
        this.errorMessage = errorMessage;
    }

    /**
     * 标记为部分成功
     */
    public void markAsPartialSuccess() {
        this.status = 3;
    }

    /**
     * 检查是否成功
     */
    public boolean isSuccess() {
        return this.status == 1;
    }

    /**
     * 检查是否失败
     */
    public boolean isFailed() {
        return this.status == 2;
    }

    /**
     * 检查是否部分成功
     */
    public boolean isPartialSuccess() {
        return this.status == 3;
    }

    @Override
    public String toString() {
        return "SkillExecutionEntity{" +
                "executionId=" + executionId +
                ", sessionId='" + sessionId + '\'' +
                ", skillName='" + skillName + '\'' +
                ", skillInput='" + (skillInput != null ? skillInput.substring(0, Math.min(skillInput.length(), 50)) + "..." : "null") + '\'' +
                ", skillOutput='" + (skillOutput != null ? skillOutput.substring(0, Math.min(skillOutput.length(), 50)) + "..." : "null") + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                '}';
    }
}