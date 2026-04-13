package com.example.agent.core.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.agent.core.entity.ConversationSessionEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 会话表Mapper接口
 */
@Mapper
public interface ConversationSessionMapper extends BaseMapper<ConversationSessionEntity> {

    /**
     * 根据会话ID查询
     */
    ConversationSessionEntity selectBySessionId(String sessionId);

    /**
     * 更新会话信息
     */
    int update(ConversationSessionEntity entity);

    /**
     * 删除会话（逻辑删除）
     */
    int deleteBySessionId(String sessionId);

    /**
     * 查询所有活跃会话
     */
    List<ConversationSessionEntity> selectAllActive();

    /**
     * 查询指定用户的会话
     */
    List<ConversationSessionEntity> selectByUserId(String userId);



    /**
     * 更新最后活跃时间
     */
    int updateLastActive(@Param("sessionId") String sessionId, @Param("lastActiveAt") LocalDateTime lastActiveAt);

    /**
     * 更新消息计数
     */
    int incrementMessageCount(String sessionId);

    /**
     * 更新存储层级
     */
    int updateStorageLevel(@Param("sessionId") String sessionId, @Param("storageLevel") Integer storageLevel);

    /**
     * 检查会话是否存在
     */
    boolean existsBySessionId(String sessionId);


}