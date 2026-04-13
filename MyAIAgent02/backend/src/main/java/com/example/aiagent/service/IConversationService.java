package com.example.aiagent.service;

import com.example.aiagent.dto.ChatMessage;
import com.example.aiagent.dto.SessionMetadata;

import java.util.List;

/**
 * 对话服务接口
 * 定义对话记忆存储的核心操作
 */
public interface IConversationService {

    /**
     * 添加消息到对话历史
     */
    void addMessage(String sessionId, String role, String content);

    /**
     * 获取对话历史（最近的消息在后）
     */
    List<ChatMessage> getHistory(String sessionId);

    /**
     * 获取最近N条消息
     */
    List<ChatMessage> getRecentHistory(String sessionId, int maxMessages);

    /**
     * 清除指定对话的历史
     */
    void clearHistory(String sessionId);

    /**
     * 获取所有对话的sessionId
     */
    List<String> getAllSessionIds();

    /**
     * 获取对话数量
     */
    int getConversationCount();

    /**
     * 将历史消息格式化为字符串，用于AI提示词
     */
    String formatHistoryForPrompt(List<ChatMessage> history);

    /**
     * 获取最近对话的格式化上下文（限制token数量）
     */
    String getFormattedContext(String sessionId, int maxRecentMessages);

    /**
     * 获取会话元数据
     */
    SessionMetadata getSessionMetadata(String sessionId);

    /**
     * 获取所有会话的元数据列表
     */
    List<SessionMetadata> getAllSessionMetadata();

    /**
     * 更新会话标题
     */
    boolean updateSessionTitle(String sessionId, String newTitle);

    /**
     * 删除会话（包括历史消息和元数据）
     */
    boolean deleteSession(String sessionId);

    /**
     * 检查会话是否存在
     */
    boolean sessionExists(String sessionId);

    /**
     * 获取活跃会话数量
     */
    int getActiveSessionCount();

    /**
     * 批量清理所有超过限制的会话
     */
    void cleanupExcessSessions();

    /**
     * 创建新会话（无初始消息）
     */
    SessionMetadata createSession(String sessionId, String title);
}