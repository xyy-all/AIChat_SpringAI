package com.example.aiagent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.aiagent.entity.ConversationMessageEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 消息表Mapper接口
 */
@Mapper
public interface ConversationMessageMapper extends BaseMapper<ConversationMessageEntity> {


    /**
     * 批量插入消息
     */
    int batchInsert(@Param("list") List<ConversationMessageEntity> messages);

    /**
     * 批量插入或更新消息（ON DUPLICATE KEY UPDATE）
     */
    int upsertBatch(@Param("list") List<ConversationMessageEntity> messages);

    /**
     * 根据会话ID查询消息（未删除的）
     */
    List<ConversationMessageEntity> selectBySessionId(String sessionId);

    /**
     * 根据会话ID和消息索引查询
     */
    ConversationMessageEntity selectBySessionIdAndIndex(@Param("sessionId") String sessionId, @Param("messageIndex") Integer messageIndex);

    /**
     * 查询会话的消息数量
     */
    int countBySessionId(String sessionId);

    /**
     * 逻辑删除消息
     */
    int deleteBySessionId(String sessionId);

    /**
     * 物理删除消息（谨慎使用）
     */
    int deleteBySessionIdPermanently(String sessionId);


    /**
     * 根据消息ID查询
     */
    ConversationMessageEntity selectByMessageId(Long messageId);

    /**
     * 查询最近的消息
     */
    List<ConversationMessageEntity> selectRecentMessages(@Param("sessionId") String sessionId, @Param("limit") int limit);

    /**
     * 获取会话的最大消息索引
     */
    int getMaxMessageIndex(String sessionId);


}