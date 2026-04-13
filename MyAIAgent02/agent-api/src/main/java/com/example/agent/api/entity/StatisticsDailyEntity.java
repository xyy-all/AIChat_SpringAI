package com.example.agent.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.type.Alias;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日统计实体类
 * 对应表：ai_statistics_daily
 */
@TableName("ai_statistics_daily")
public class StatisticsDailyEntity implements Serializable {

    @TableId
    private LocalDate statDate;  // 统计日期，主键

    private Integer totalSessions = 0;  // 总会话数

    private Integer activeSessions = 0;  // 活跃会话数

    private Integer newSessions = 0;  // 新增会话数

    private Integer totalMessages = 0;  // 总消息数

    private Integer userMessages = 0;  // 用户消息数

    private Integer assistantMessages = 0;  // 助理消息数

    private BigDecimal avgMessageLength = BigDecimal.ZERO;  // 平均消息长度

    private Long totalTokens = 0L;  // 总token数

    private Integer skillExecutions = 0;  // 技能执行次数

    private Integer documentUploads = 0;  // 文档上传次数

    private LocalDateTime updatedAt;  // 更新时间

    // 构造函数
    public StatisticsDailyEntity() {
    }

    public StatisticsDailyEntity(LocalDate statDate) {
        this.statDate = statDate;
    }

    // Getters and Setters

    public LocalDate getStatDate() {
        return statDate;
    }

    public void setStatDate(LocalDate statDate) {
        this.statDate = statDate;
    }

    public Integer getTotalSessions() {
        return totalSessions;
    }

    public void setTotalSessions(Integer totalSessions) {
        this.totalSessions = totalSessions;
    }

    public Integer getActiveSessions() {
        return activeSessions;
    }

    public void setActiveSessions(Integer activeSessions) {
        this.activeSessions = activeSessions;
    }

    public Integer getNewSessions() {
        return newSessions;
    }

    public void setNewSessions(Integer newSessions) {
        this.newSessions = newSessions;
    }

    public Integer getTotalMessages() {
        return totalMessages;
    }

    public void setTotalMessages(Integer totalMessages) {
        this.totalMessages = totalMessages;
    }

    public Integer getUserMessages() {
        return userMessages;
    }

    public void setUserMessages(Integer userMessages) {
        this.userMessages = userMessages;
    }

    public Integer getAssistantMessages() {
        return assistantMessages;
    }

    public void setAssistantMessages(Integer assistantMessages) {
        this.assistantMessages = assistantMessages;
    }

    public BigDecimal getAvgMessageLength() {
        return avgMessageLength;
    }

    public void setAvgMessageLength(BigDecimal avgMessageLength) {
        this.avgMessageLength = avgMessageLength;
    }

    public Long getTotalTokens() {
        return totalTokens;
    }

    public void setTotalTokens(Long totalTokens) {
        this.totalTokens = totalTokens;
    }

    public Integer getSkillExecutions() {
        return skillExecutions;
    }

    public void setSkillExecutions(Integer skillExecutions) {
        this.skillExecutions = skillExecutions;
    }

    public Integer getDocumentUploads() {
        return documentUploads;
    }

    public void setDocumentUploads(Integer documentUploads) {
        this.documentUploads = documentUploads;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "StatisticsDailyEntity{" +
                "statDate=" + statDate +
                ", totalSessions=" + totalSessions +
                ", activeSessions=" + activeSessions +
                ", newSessions=" + newSessions +
                ", totalMessages=" + totalMessages +
                ", userMessages=" + userMessages +
                ", assistantMessages=" + assistantMessages +
                ", avgMessageLength=" + avgMessageLength +
                ", totalTokens=" + totalTokens +
                ", skillExecutions=" + skillExecutions +
                ", documentUploads=" + documentUploads +
                ", updatedAt=" + updatedAt +
                '}';
    }
}