package com.example.agent.api.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import org.apache.ibatis.type.Alias;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 系统配置实体类
 * 对应表：ai_system_config
 */
@TableName("ai_system_config")
public class SystemConfigEntity implements Serializable {

    @TableId
    private String configKey;  // 配置键，主键

    private String configValue;  // 配置值

    private String configType = "string";  // 配置类型：string, number, boolean, json

    private String description;  // 配置描述

    private LocalDateTime createdAt;  // 创建时间

    private LocalDateTime updatedAt;  // 更新时间

    private String updatedBy;  // 最后更新人

    // 构造函数
    public SystemConfigEntity() {
    }

    public SystemConfigEntity(String configKey, String configValue, String configType, String description) {
        this.configKey = configKey;
        this.configValue = configValue;
        this.configType = configType;
        this.description = description;
    }

    // Getters and Setters

    public String getConfigKey() {
        return configKey;
    }

    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }

    public String getConfigValue() {
        return configValue;
    }

    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }

    public String getConfigType() {
        return configType;
    }

    public void setConfigType(String configType) {
        this.configType = configType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    // 业务方法

    /**
     * 获取配置值的整数形式
     */
    public Integer getIntValue() {
        try {
            return Integer.parseInt(configValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 获取配置值的布尔形式
     */
    public Boolean getBooleanValue() {
        if ("true".equalsIgnoreCase(configValue)) {
            return true;
        } else if ("false".equalsIgnoreCase(configValue)) {
            return false;
        } else if ("1".equals(configValue)) {
            return true;
        } else if ("0".equals(configValue)) {
            return false;
        }
        return null;
    }

    /**
     * 获取配置值的浮点数形式
     */
    public Double getDoubleValue() {
        try {
            return Double.parseDouble(configValue);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 检查是否为JSON类型
     */
    public boolean isJsonType() {
        return "json".equalsIgnoreCase(configType);
    }

    @Override
    public String toString() {
        return "SystemConfigEntity{" +
                "configKey='" + configKey + '\'' +
                ", configType='" + configType + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}