package com.example.agent.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.agent.api.entity.SystemConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 系统配置表Mapper接口
 */
@Mapper
public interface SystemConfigMapper extends BaseMapper<SystemConfigEntity> {

    /**
     * 根据配置类型查询配置
     */
    List<SystemConfigEntity> selectByConfigType(String configType);

    /**
     * 批量插入或更新配置
     */
    int batchUpsert(@Param("list") List<SystemConfigEntity> configs);

    /**
     * 根据配置键前缀查询配置
     */
    List<SystemConfigEntity> selectByKeyPrefix(String keyPrefix);

    /**
     * 删除多个配置键
     */
    int deleteByKeys(@Param("keys") List<String> keys);

    /**
     * 检查配置键是否存在
     */
    boolean existsByKey(String configKey);

    /**
     * 获取配置值（字符串）
     */
    String getStringValue(String configKey);

    /**
     * 获取配置值（整数）
     */
    Integer getIntValue(String configKey);

    /**
     * 获取配置值（布尔值）
     */
    Boolean getBooleanValue(String configKey);

    /**
     * 获取配置值（浮点数）
     */
    Double getDoubleValue(String configKey);
}