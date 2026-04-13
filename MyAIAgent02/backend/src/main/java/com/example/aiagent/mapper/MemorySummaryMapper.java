package com.example.aiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiagent.entity.MemorySummaryEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 记忆摘要表Mapper接口
 */
@Mapper
public interface MemorySummaryMapper extends BaseMapper<MemorySummaryEntity> {

    /**
     * 查询指定会话的所有记忆摘要
     */
    List<MemorySummaryEntity> selectBySessionId(@Param("sessionId") String sessionId);

    /**
     * 更新记忆摘要
     */
    int update(MemorySummaryEntity entity);

    /**
     * 根据会话ID删除记忆摘要
     */
    int deleteBySessionId(@Param("sessionId") String sessionId);

}