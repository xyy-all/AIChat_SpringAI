package com.example.aiagent.service;

import com.example.aiagent.config.AppConfiguration;
import com.example.aiagent.dto.ChatMessage;
import com.example.aiagent.dto.SessionMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

// @Service - 已由MultiLayerConversationService实现，此类保留供参考或测试使用
public class ConversationService implements IConversationService {

    private static final Logger logger = LoggerFactory.getLogger(ConversationService.class);

    // 内存存储：sessionId -> 消息列表
    private final Map<String, List<ChatMessage>> conversationStore = new ConcurrentHashMap<>();

    // 会话元数据存储：sessionId -> 会话元数据
    private final Map<String, SessionMetadata> sessionMetadataStore = new ConcurrentHashMap<>();

    // 最大历史消息数量（防止内存溢出）
    private static final int MAX_HISTORY_SIZE = 20;

    // 最大对话数量（防止内存溢出）
    private static final int MAX_CONVERSATIONS = 100;

    /**
     * 添加消息到对话历史
     */
    public void addMessage(String sessionId, String role, String content) {
        logger.debug("addMessage called - sessionId: {}, role: {}, content length: {}", sessionId, role, (content != null ? content.length() : 0));

        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        // 如果对话数量过多，清理最老的对话（LRU策略简化版）
        if (conversationStore.size() >= MAX_CONVERSATIONS && !conversationStore.containsKey(sessionId)) {
            cleanupOldConversations();
        }

        List<ChatMessage> messages = conversationStore.computeIfAbsent(sessionId,
            k -> new CopyOnWriteArrayList<>());

        messages.add(new ChatMessage(role, content, LocalDateTime.now()));

        // 更新会话元数据
        updateSessionMetadata(sessionId, role, content, messages.size());

        // 限制历史消息数量
        if (messages.size() > MAX_HISTORY_SIZE) {
            messages = messages.subList(messages.size() - MAX_HISTORY_SIZE, messages.size());
            conversationStore.put(sessionId, new CopyOnWriteArrayList<>(messages));
        }
    }

    /**
     * 获取对话历史（最近的消息在后）
     */
    public List<ChatMessage> getHistory(String sessionId) {
        if (sessionId == null || !conversationStore.containsKey(sessionId)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(conversationStore.get(sessionId));
    }

    /**
     * 获取最近N条消息
     */
    public List<ChatMessage> getRecentHistory(String sessionId, int maxMessages) {
        List<ChatMessage> allHistory = getHistory(sessionId);
        if (allHistory.size() <= maxMessages) {
            return allHistory;
        }
        return new ArrayList<>(allHistory.subList(allHistory.size() - maxMessages, allHistory.size()));
    }

    /**
     * 清除指定对话的历史
     */
    public void clearHistory(String sessionId) {
        conversationStore.remove(sessionId);
    }

    /**
     * 获取所有对话的sessionId
     */
    public List<String> getAllSessionIds() {
        return new ArrayList<>(conversationStore.keySet());
    }

    /**
     * 获取对话数量
     */
    public int getConversationCount() {
        return conversationStore.size();
    }

    /**
     * 清理最老的对话（当对话数量达到上限时）
     */
    private void cleanupOldConversations() {
        // 简单策略：删除第一个entry（不精确的LRU，但简单有效）
        if (!conversationStore.isEmpty()) {
            String firstKey = conversationStore.keySet().iterator().next();
            conversationStore.remove(firstKey);
        }
    }

    /**
     * 将历史消息格式化为字符串，用于AI提示词
     */
    public String formatHistoryForPrompt(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n以下是之前的对话历史（最近的在最后）：\n");

        for (ChatMessage message : history) {
            String role = message.getRole();
            String content = message.getContent();
            sb.append(role).append(": ").append(content).append("\n");
        }

        sb.append("\n请基于以上对话历史回答用户的新问题。");
        return sb.toString();
    }

    /**
     * 获取最近对话的格式化上下文（限制token数量）
     */
    public String getFormattedContext(String sessionId, int maxRecentMessages) {
        List<ChatMessage> recentHistory = getRecentHistory(sessionId, maxRecentMessages);
        return formatHistoryForPrompt(recentHistory);
    }

    // =================== 会话元数据管理方法 ===================

    /**
     * 更新会话元数据
     */
    private void updateSessionMetadata(String sessionId, String role, String content, int messageCount) {
        SessionMetadata metadata = sessionMetadataStore.computeIfAbsent(sessionId,
            k -> createDefaultSessionMetadata(sessionId, content));

        metadata.updateLastActive();
        metadata.setMessageCount(messageCount);

        // 如果这是第一条消息，且当前标题是默认值，尝试从内容生成标题
        if (messageCount == 1 && "user".equals(role)) {
            String currentTitle = metadata.getTitle();
            // 如果标题为空或是默认标题"新对话"，则生成新标题
            if (currentTitle == null || currentTitle.isEmpty() || "新对话".equals(currentTitle)) {
                String autoTitle = generateTitleFromContent(content);
                metadata.setTitle(autoTitle);
            }
        }
    }

    /**
     * 创建默认会话元数据
     */
    private SessionMetadata createDefaultSessionMetadata(String sessionId, String firstMessage) {
        String title = generateTitleFromContent(firstMessage);
        return new SessionMetadata(sessionId, title);
    }

    /**
     * 从消息内容生成标题
     */
    private String generateTitleFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "新对话";
        }

        // 取前30个字符作为标题，避免过长
        String trimmed = content.trim();
        if (trimmed.length() <= 30) {
            return trimmed;
        }

        return trimmed.substring(0, 30) + "...";
    }

    /**
     * 获取会话元数据
     */
    public SessionMetadata getSessionMetadata(String sessionId) {
        return sessionMetadataStore.get(sessionId);
    }

    /**
     * 获取所有会话的元数据列表
     */
    public List<SessionMetadata> getAllSessionMetadata() {
        return new ArrayList<>(sessionMetadataStore.values());
    }

    /**
     * 更新会话标题
     */
    public boolean updateSessionTitle(String sessionId, String newTitle) {
        if (sessionId == null || newTitle == null || newTitle.trim().isEmpty()) {
            return false;
        }

        SessionMetadata metadata = sessionMetadataStore.computeIfAbsent(sessionId,
            k -> new SessionMetadata(sessionId, newTitle.trim()));

        metadata.setTitle(newTitle.trim());
        metadata.updateLastActive();
        return true;
    }

    /**
     * 删除会话（包括历史消息和元数据）
     */
    public boolean deleteSession(String sessionId) {
        if (sessionId == null) {
            return false;
        }

        boolean removedHistory = conversationStore.remove(sessionId) != null;
        boolean removedMetadata = sessionMetadataStore.remove(sessionId) != null;

        return removedHistory || removedMetadata;
    }

    /**
     * 检查会话是否存在
     */
    public boolean sessionExists(String sessionId) {
        return conversationStore.containsKey(sessionId) || sessionMetadataStore.containsKey(sessionId);
    }

    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return sessionMetadataStore.size();
    }

    /**
     * 清理旧的会话元数据（当数量超过上限时）
     */
    private void cleanupOldSessionMetadata() {
        // 如果会话元数据数量超过上限，清理最老的
        if (sessionMetadataStore.size() > MAX_CONVERSATIONS) {
            // 简单策略：按最后活跃时间排序，删除最老的
            List<SessionMetadata> sorted = new ArrayList<>(sessionMetadataStore.values());
            sorted.sort((a, b) -> a.getLastActiveAt().compareTo(b.getLastActiveAt()));

            int toRemove = sorted.size() - MAX_CONVERSATIONS;
            for (int i = 0; i < toRemove && i < sorted.size(); i++) {
                SessionMetadata metadata = sorted.get(i);
                sessionMetadataStore.remove(metadata.getSessionId());
                conversationStore.remove(metadata.getSessionId());
            }
        }
    }

    /**
     * 批量清理所有超过MAX_CONVERSATIONS限制的会话
     */
    public void cleanupExcessSessions() {
        if (sessionMetadataStore.size() <= MAX_CONVERSATIONS) {
            return;
        }

        cleanupOldSessionMetadata();
    }

    /**
     * 创建新会话（无初始消息）
     */
    public SessionMetadata createSession(String sessionId, String title) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }

        // 检查会话是否已存在
        if (sessionExists(sessionId)) {
            return getSessionMetadata(sessionId);
        }

        // 创建会话元数据
        SessionMetadata metadata = new SessionMetadata(sessionId, title);
        metadata.updateLastActive();

        // 存储到元数据存储
        sessionMetadataStore.put(sessionId, metadata);

        // 创建空消息列表
        conversationStore.put(sessionId, new CopyOnWriteArrayList<>());

        return metadata;
    }
}