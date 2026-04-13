package com.example.agent.core.service;

import com.example.agent.core.config.AppConfiguration;
import com.example.agent.core.dto.ChatMessage;
import com.example.agent.core.dto.SessionMetadata;
import com.example.agent.core.entity.ConversationMessageEntity;
import com.example.agent.core.entity.ConversationSessionEntity;
import com.example.agent.core.enums.MessageRole;
import com.example.agent.core.mapper.ConversationMessageMapper;
import com.example.agent.core.mapper.ConversationSessionMapper;
import com.example.agent.core.mapper.MemorySummaryMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 多层对话记忆服务
 * 实现懒加载+写时同步策略：
 * 1. 读取：优先从内存读取，未命中则从数据库加载到内存
 * 2. 写入：先写入内存，然后根据配置同步到数据库（实时/异步/批量）
 * 3. 缓存管理：LRU策略，限制内存中会话数量和消息数量
 */
@Service
@Primary
public class MultiLayerConversationService implements IConversationService {

    private static final Logger logger = LoggerFactory.getLogger(MultiLayerConversationService.class);

    // =================== 内存存储（一级缓存） ===================

    /** 会话消息存储：sessionId -> 消息列表 */
    private final Map<String, List<ChatMessage>> memoryMessageStore = new ConcurrentHashMap<>();

    /** 会话元数据存储：sessionId -> 会话元数据 */
    private final Map<String, SessionMetadata> memoryMetadataStore = new ConcurrentHashMap<>();

    /** 会话访问顺序，用于LRU淘汰 */
    private final LinkedHashSet<String> accessOrder = new LinkedHashSet<>();

    /** 会话已同步的最大消息索引：sessionId -> 已同步的最大消息索引 */
    private final Map<String, Integer> sessionMaxSyncedIndex = new ConcurrentHashMap<>();

    /** 会话下一个消息索引：sessionId -> 下一个要分配的消息索引 */
    private final Map<String, Integer> sessionNextMessageIndex = new ConcurrentHashMap<>();

    /** 消息索引存储：sessionId -> 消息索引列表（与memoryMessageStore中的消息一一对应） */
    private final Map<String, List<Integer>> memoryMessageIndexStore = new ConcurrentHashMap<>();

    // =================== 配置参数 ===================

    @Autowired
    private AppConfiguration appConfig;

    /** 内存最大会话数 */
    private int memoryCacheMaxSessions;

    /** 每个会话最大消息数 */
    private int memoryCacheMaxMessagesPerSession;

    /** 内存缓存TTL（秒） */
    private int memoryCacheTtlSeconds;

    /** 数据库同步是否启用 */
    private boolean dbSyncEnabled = true;

    /** 懒加载是否启用 */
    private boolean dbSyncLazyLoad = true;

    /** 写时同步是否启用 */
    private boolean dbSyncWriteThrough = true;

    /** 批量操作大小 */
    private int dbSyncBatchSize = 50;

    /** 异步刷新间隔（毫秒） */
    private int dbSyncFlushIntervalMs = 5000;

    // =================== 数据库Mapper ===================

    @Autowired
    private ConversationSessionMapper sessionMapper;

    @Autowired
    private ConversationMessageMapper messageMapper;

    @Autowired
    private MemorySummaryMapper memorySummaryMapper;

    // =================== 构造函数 ===================

    public MultiLayerConversationService() {
    }

    @PostConstruct
    private void initializeConfigValues() {
        this.memoryCacheMaxSessions = appConfig.getMemoryCache().getMaxSessions();
        this.memoryCacheMaxMessagesPerSession = appConfig.getMemoryCache().getMaxMessagesPerSession();
        this.memoryCacheTtlSeconds = appConfig.getMemoryCache().getTtlSeconds();
        this.dbSyncEnabled = appConfig.getDbSync().isEnabled();
        this.dbSyncLazyLoad = appConfig.getDbSync().isLazyLoad();
        this.dbSyncWriteThrough = appConfig.getDbSync().isWriteThrough();
        this.dbSyncBatchSize = appConfig.getDbSync().getBatchSize();
        this.dbSyncFlushIntervalMs = appConfig.getDbSync().getFlushIntervalMs();
    }


    // =================== 核心业务方法 ===================

    /**
     * 添加消息到对话历史（写时同步）
     */
    @Transactional
    public void addMessage(String sessionId, String role, String content) {
        logger.debug("addMessage called - sessionId: {}, role: {}, content length: {}", sessionId, role, (content != null ? content.length() : 0));

        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }

        // 0. 确保会话已加载到内存（如果存在数据库中）
        ensureSessionLoaded(sessionId);

        // 1. 更新内存缓存
        addMessageToMemory(sessionId, role, content);

        // 2. 同步到数据库
        if (dbSyncEnabled) {
            logger.debug("dbSyncEnabled=true, syncing to database");
            syncSessionToDatabase(sessionId);
        } else {
            logger.debug("dbSyncEnabled=false, skipping database sync");
        }
    }

    /**
     * 确保会话已加载到内存（如果存在于数据库中）
     */
    private void ensureSessionLoaded(String sessionId) {
        if (sessionId == null) {
            return;
        }

        // 如果内存中已有该会话，直接返回
        if (memoryMessageStore.containsKey(sessionId)) {
            return;
        }

        // 如果启用了数据库同步和懒加载，从数据库加载会话
        if (dbSyncEnabled && dbSyncLazyLoad) {
            logger.info("ensureSessionLoaded - Loading session from database: {}", sessionId);
            loadSessionFromDatabase(sessionId);
        } else {
            logger.debug("ensureSessionLoaded - Skipping load (dbSyncEnabled={}, dbSyncLazyLoad={})", dbSyncEnabled, dbSyncLazyLoad);
        }
    }

    /**
     * 获取对话历史（懒加载）
     */
    public List<ChatMessage> getHistory(String sessionId) {
        if (sessionId == null) {
            return new ArrayList<>();
        }

        // 1. 检查内存中是否存在
        if (memoryMessageStore.containsKey(sessionId)) {
            updateAccessOrder(sessionId);
            return new ArrayList<>(memoryMessageStore.get(sessionId));
        }

        // 2. 如果启用了懒加载，从数据库加载
        if (dbSyncEnabled && dbSyncLazyLoad) {
            loadSessionFromDatabase(sessionId);
            return new ArrayList<>(memoryMessageStore.getOrDefault(sessionId, new ArrayList<>()));
        }

        return new ArrayList<>();
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
    @Transactional
    public void clearHistory(String sessionId) {
        // 1. 清除内存缓存
        memoryMessageStore.remove(sessionId);
        memoryMetadataStore.remove(sessionId);
        accessOrder.remove(sessionId);

        // 2. 清除数据库
        if (dbSyncEnabled) {
            messageMapper.deleteBySessionId(sessionId);
            sessionMapper.deleteBySessionId(sessionId);
        }
    }

    /**
     * 获取所有对话的sessionId
     */
    public List<String> getAllSessionIds() {
        // 返回内存中所有的会话ID
        return new ArrayList<>(memoryMessageStore.keySet());
    }

    /**
     * 获取会话元数据
     */
    public SessionMetadata getSessionMetadata(String sessionId) {
        if (sessionId == null) {
            return null;
        }

        // 1. 检查内存中是否存在
        if (memoryMetadataStore.containsKey(sessionId)) {
            updateAccessOrder(sessionId);
            return memoryMetadataStore.get(sessionId);
        }

        // 2. 如果启用了懒加载，从数据库加载
        if (dbSyncEnabled && dbSyncLazyLoad) {
            loadSessionFromDatabase(sessionId);
            return memoryMetadataStore.get(sessionId);
        }

        return null;
    }

    /**
     * 获取所有会话的元数据列表
     * 如果内存中没有会话且启用了数据库同步，从数据库加载所有会话元数据
     */
    public List<SessionMetadata> getAllSessionMetadata() {
        // 如果内存中没有会话元数据，且启用了数据库同步，尝试从数据库加载
        if (memoryMetadataStore.isEmpty() && dbSyncEnabled) {
            loadAllSessionsMetadataFromDatabase();
        }
        return new ArrayList<>(memoryMetadataStore.values());
    }

    /**
     * 从数据库加载所有会话的元数据到内存（不加载消息）
     */
    private void loadAllSessionsMetadataFromDatabase() {
        if (!dbSyncEnabled) {
            return;
        }

        try {
            logger.info("loadAllSessionsMetadataFromDatabase - Loading all sessions metadata from database");
            List<ConversationSessionEntity> sessionEntities = sessionMapper.selectAllActive();
            logger.info("loadAllSessionsMetadataFromDatabase - Found {} active sessions in database", sessionEntities.size());

            for (ConversationSessionEntity entity : sessionEntities) {
                String sessionId = entity.getSessionId();
                // 如果内存中还没有该会话的元数据，添加它
                if (!memoryMetadataStore.containsKey(sessionId)) {
                    SessionMetadata metadata = convertToSessionMetadata(entity);
                    memoryMetadataStore.put(sessionId, metadata);
                    logger.debug("loadAllSessionsMetadataFromDatabase - Loaded metadata for session: {}, title: {}", sessionId, metadata.getTitle());
                }
            }
            logger.info("loadAllSessionsMetadataFromDatabase - Total sessions in memory: {}", memoryMetadataStore.size());
        } catch (Exception e) {
            logger.error("loadAllSessionsMetadataFromDatabase - Error loading sessions metadata", e);
        }
    }

    /**
     * 更新会话标题
     */
    @Transactional
    public boolean updateSessionTitle(String sessionId, String newTitle) {
        if (sessionId == null || newTitle == null || newTitle.trim().isEmpty()) {
            return false;
        }

        // 0. 检查会话是否存在（内存或数据库）
        if (!sessionExists(sessionId)) {
            logger.warn("updateSessionTitle - Session does not exist: {}", sessionId);
            return false;
        }

        // 1. 确保会话已加载到内存
        ensureSessionLoaded(sessionId);

        // 2. 更新内存中的元数据
        SessionMetadata metadata = memoryMetadataStore.computeIfAbsent(sessionId,
            k -> new SessionMetadata(sessionId, newTitle.trim()));
        metadata.setTitle(newTitle.trim());
        metadata.updateLastActive();

        // 3. 更新数据库中的会话记录
        if (dbSyncEnabled) {
            ConversationSessionEntity sessionEntity = convertToSessionEntity(sessionId, metadata);
            if (sessionMapper.existsBySessionId(sessionId)) {
                sessionMapper.update(sessionEntity);
            } else {
                sessionMapper.insert(sessionEntity);
            }
        }

        return true;
    }

    /**
     * 删除会话（包括历史消息和元数据）
     */
    @Transactional
    public boolean deleteSession(String sessionId) {
        if (sessionId == null) {
            return false;
        }

        // 0. 检查会话是否存在
        if (!sessionExists(sessionId)) {
            logger.warn("deleteSession - Session does not exist: {}", sessionId);
            return false;
        }

        // 1. 从内存中删除
        boolean removedFromMemory = (memoryMessageStore.remove(sessionId) != null) ||
                                   (memoryMetadataStore.remove(sessionId) != null);
        accessOrder.remove(sessionId);

        // 2. 从数据库中删除（逻辑删除）
        if (dbSyncEnabled) {
            sessionMapper.deleteBySessionId(sessionId);
            messageMapper.deleteBySessionId(sessionId);
            memorySummaryMapper.deleteBySessionId(sessionId);
        }

        return true; // 会话存在并被删除（或已删除）
    }

    /**
     * 检查会话是否存在
     */
    public boolean sessionExists(String sessionId) {
        if (sessionId == null) {
            return false;
        }

        // 检查内存
        if (memoryMessageStore.containsKey(sessionId) || memoryMetadataStore.containsKey(sessionId)) {
            return true;
        }

        // 检查数据库
        if (dbSyncEnabled) {
            return sessionMapper.existsBySessionId(sessionId);
        }

        return false;
    }

    // =================== 私有辅助方法 ===================

    /**
     * 添加消息到内存缓存
     */
    private void addMessageToMemory(String sessionId, String role, String content) {
        // 如果对话数量过多，清理最老的对话
        if (memoryMessageStore.size() >= memoryCacheMaxSessions && !memoryMessageStore.containsKey(sessionId)) {
            cleanupOldConversations();
        }

        // 获取或创建消息列表
        List<ChatMessage> messages = memoryMessageStore.computeIfAbsent(sessionId,
            k -> new CopyOnWriteArrayList<>());

        // 获取或创建消息索引列表
        List<Integer> messageIndices = memoryMessageIndexStore.computeIfAbsent(sessionId,
            k -> new CopyOnWriteArrayList<>());

        // 确定新消息的索引（从sessionNextMessageIndex获取或从0开始）
        int currentIndex = sessionNextMessageIndex.getOrDefault(sessionId, 0);

        // 添加新消息和对应的索引
        messages.add(new ChatMessage(role, content, LocalDateTime.now()));
        messageIndices.add(currentIndex);

        // 更新下一个消息索引（递增）
        sessionNextMessageIndex.put(sessionId, currentIndex + 1);

        // 更新会话元数据
        updateSessionMetadataInMemory(sessionId, role, content, messages.size());

        // 限制历史消息数量
        if (messages.size() > memoryCacheMaxMessagesPerSession) {
            // 计算需要删除的消息数量
            int toRemove = messages.size() - memoryCacheMaxMessagesPerSession;

            // 删除最早的消息和对应的索引
            messages = messages.subList(toRemove, messages.size());
            messageIndices = messageIndices.subList(toRemove, messageIndices.size());

            // 更新存储
            memoryMessageStore.put(sessionId, new CopyOnWriteArrayList<>(messages));
            memoryMessageIndexStore.put(sessionId, new CopyOnWriteArrayList<>(messageIndices));
        }

        // 更新访问顺序
        updateAccessOrder(sessionId);
    }

    /**
     * 更新内存中的会话元数据
     */
    private void updateSessionMetadataInMemory(String sessionId, String role, String content, int messageCount) {
        SessionMetadata metadata = memoryMetadataStore.computeIfAbsent(sessionId,
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
     * 清理最老的对话（当对话数量达到上限时）
     */
    private void cleanupOldConversations() {
        // 使用LRU策略：删除最久未访问的会话
        if (!accessOrder.isEmpty()) {
            String oldestSessionId = accessOrder.iterator().next();
            memoryMessageStore.remove(oldestSessionId);
            memoryMetadataStore.remove(oldestSessionId);
            accessOrder.remove(oldestSessionId);
        }
    }

    /**
     * 更新访问顺序（LRU）
     */
    private void updateAccessOrder(String sessionId) {
        accessOrder.remove(sessionId);
        accessOrder.add(sessionId);
    }

    /**
     * 将内存中的会话同步到数据库
     */
    @Transactional
    public void syncSessionToDatabase(String sessionId) {
        logger.debug("syncSessionToDatabase called - sessionId: {}", sessionId);
        if (!dbSyncEnabled || sessionId == null) {
            logger.debug("Skipping sync - dbSyncEnabled: {}, sessionId: {}", dbSyncEnabled, sessionId);
            return;
        }

        try {
            // 1. 同步会话元数据
            SessionMetadata metadata = memoryMetadataStore.get(sessionId);
            if (metadata != null) {
                ConversationSessionEntity sessionEntity = convertToSessionEntity(sessionId, metadata);
                if (sessionMapper.existsBySessionId(sessionId)) {
                    sessionMapper.update(sessionEntity);
                } else {
                    sessionMapper.insert(sessionEntity);
                }
            }

            // 2. 增量同步消息
            List<ChatMessage> messages = memoryMessageStore.get(sessionId);
            List<Integer> messageIndices = memoryMessageIndexStore.get(sessionId);

            if (messages != null && !messages.isEmpty() && messageIndices != null && messageIndices.size() == messages.size()) {
                // 获取已同步的最大索引（先检查内存，再查询数据库）
                Integer maxSyncedInMemory = sessionMaxSyncedIndex.get(sessionId);
                int maxSyncedIndex = (maxSyncedInMemory != null) ? maxSyncedInMemory : -1;

                // 如果内存中没有记录，从数据库查询
                if (maxSyncedIndex < 0 && dbSyncEnabled) {
                    maxSyncedIndex = messageMapper.getMaxMessageIndex(sessionId);
                    sessionMaxSyncedIndex.put(sessionId, maxSyncedIndex);
                }

                // 筛选未同步的消息（索引大于已同步索引）
                List<ConversationMessageEntity> unsyncedEntities = new ArrayList<>();
                int maxIndexInBatch = -1;

                for (int i = 0; i < messages.size(); i++) {
                    int currentIndex = messageIndices.get(i);
                    // 只同步索引大于已同步索引的消息
                    if (currentIndex > maxSyncedIndex) {
                        ChatMessage msg = messages.get(i);
                        ConversationMessageEntity entity = convertToMessageEntity(sessionId, currentIndex, msg);
                        unsyncedEntities.add(entity);

                        // 更新批次中的最大索引
                        if (currentIndex > maxIndexInBatch) {
                            maxIndexInBatch = currentIndex;
                        }
                    }
                }

                // 批量插入未同步的消息（使用upsert避免重复）
                if (!unsyncedEntities.isEmpty()) {
                    messageMapper.upsertBatch(unsyncedEntities);
                    // 更新内存中的已同步索引为批次中的最大索引
                    if (maxIndexInBatch > maxSyncedIndex) {
                        sessionMaxSyncedIndex.put(sessionId, maxIndexInBatch);
                    }
                }
            }
        } catch (Exception e) {
            // 记录日志，但不要抛出异常，避免影响主流程
            logger.error("Error syncing to database", e);
        }
        logger.debug("syncSessionToDatabase completed for sessionId: {}", sessionId);
    }

    /**
     * 从数据库加载会话到内存
     */
    @Transactional
    public void loadSessionFromDatabase(String sessionId) {
        if (!dbSyncEnabled || sessionId == null) {
            logger.debug("loadSessionFromDatabase - Skipping load (dbSyncEnabled={}, sessionId={})", dbSyncEnabled, sessionId);
            return;
        }

        try {
            logger.info("loadSessionFromDatabase - Loading session from database: {}", sessionId);

            // 1. 加载会话元数据
            ConversationSessionEntity sessionEntity = sessionMapper.selectBySessionId(sessionId);
            if (sessionEntity != null) {
                logger.info("loadSessionFromDatabase - Found session in database: {}, messageCount: {}", sessionEntity.getTitle(), sessionEntity.getMessageCount());

                SessionMetadata metadata = convertToSessionMetadata(sessionEntity);
                memoryMetadataStore.put(sessionId, metadata);
                logger.debug("loadSessionFromDatabase - Loaded metadata: {}", metadata.getTitle());

                // 2. 加载消息
                List<ConversationMessageEntity> messageEntities = messageMapper.selectBySessionId(sessionId);
                logger.info("loadSessionFromDatabase - Found {} messages in database", messageEntities.size());

                List<ChatMessage> messages = new ArrayList<>();
                List<Integer> messageIndices = new ArrayList<>();

                // 查找最大消息索引
                int maxIndex = -1;
                for (ConversationMessageEntity entity : messageEntities) {
                    ChatMessage msg = convertToChatMessage(entity);
                    messages.add(msg);
                    messageIndices.add(entity.getMessageIndex());
                    if (entity.getMessageIndex() > maxIndex) {
                        maxIndex = entity.getMessageIndex();
                    }
                }

                memoryMessageStore.put(sessionId, new CopyOnWriteArrayList<>(messages));
                memoryMessageIndexStore.put(sessionId, new CopyOnWriteArrayList<>(messageIndices));
                logger.info("loadSessionFromDatabase - Loaded {} messages, maxIndex: {}", messages.size(), maxIndex);

                // 3. 初始化sessionNextMessageIndex
                if (maxIndex >= 0) {
                    sessionNextMessageIndex.put(sessionId, maxIndex + 1);
                    logger.debug("loadSessionFromDatabase - sessionNextMessageIndex set to: {}", maxIndex + 1);
                } else {
                    sessionNextMessageIndex.put(sessionId, 0);
                    logger.debug("loadSessionFromDatabase - No messages found, sessionNextMessageIndex set to: 0");
                }

                // 4. 初始化sessionMaxSyncedIndex
                if (dbSyncEnabled && !messageEntities.isEmpty()) {
                    sessionMaxSyncedIndex.put(sessionId, maxIndex);
                    logger.debug("loadSessionFromDatabase - sessionMaxSyncedIndex set to: {}", maxIndex);
                } else {
                    sessionMaxSyncedIndex.put(sessionId, -1);
                    logger.debug("loadSessionFromDatabase - sessionMaxSyncedIndex set to: -1");
                }

                // 5. 更新访问顺序
                updateAccessOrder(sessionId);
                logger.info("loadSessionFromDatabase - Successfully loaded session: {}", sessionId);
            } else {
                logger.info("loadSessionFromDatabase - Session not found in database: {}", sessionId);
            }
        } catch (Exception e) {
            // 记录日志
            logger.error("loadSessionFromDatabase - Error loading session", e);
        }
    }

    /**
     * 转换SessionMetadata为ConversationSessionEntity
     */
    private ConversationSessionEntity convertToSessionEntity(String sessionId, SessionMetadata metadata) {
        ConversationSessionEntity entity = new ConversationSessionEntity();
        entity.setSessionId(sessionId);
        entity.setTitle(metadata.getTitle());
        entity.setCreatedAt(metadata.getCreatedAt());
        entity.setLastActiveAt(metadata.getLastActiveAt());
        entity.setMessageCount(metadata.getMessageCount());
        entity.setStatus(1); // 活跃状态
        entity.setStorageLevel(1); // 内存存储层级
        // 其他字段可以根据需要设置
        return entity;
    }

    /**
     * 转换ConversationSessionEntity为SessionMetadata
     */
    private SessionMetadata convertToSessionMetadata(ConversationSessionEntity entity) {
        SessionMetadata metadata = new SessionMetadata(entity.getSessionId(), entity.getTitle());
        metadata.setCreatedAt(entity.getCreatedAt());
        metadata.setLastActiveAt(entity.getLastActiveAt());
        metadata.setMessageCount(entity.getMessageCount());
        return metadata;
    }

    /**
     * 转换ChatMessage为ConversationMessageEntity
     */
    private ConversationMessageEntity convertToMessageEntity(String sessionId, int index, ChatMessage message) {
        ConversationMessageEntity entity = new ConversationMessageEntity();
        entity.setSessionId(sessionId);
        entity.setMessageIndex(index);
        entity.setRole(MessageRole.valueOf(message.getRole()));
        entity.setContent(message.getContent());
        entity.setCreatedAt(message.getTimestamp());
        entity.setIsDeleted(false);
        return entity;
    }

    /**
     * 转换ConversationMessageEntity为ChatMessage
     */
    private ChatMessage convertToChatMessage(ConversationMessageEntity entity) {
        return new ChatMessage(entity.getRole().name(), entity.getContent(), entity.getCreatedAt());
    }

    // =================== 兼容性方法 ===================

    /**
     * 获取对话数量
     */
    public int getConversationCount() {
        return memoryMessageStore.size();
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

    /**
     * 获取活跃会话数量
     */
    public int getActiveSessionCount() {
        return memoryMetadataStore.size();
    }

    /**
     * 批量清理所有超过限制的会话
     */
    public void cleanupExcessSessions() {
        // MultiLayerConversationService已经在addMessageToMemory中实现了LRU清理
        // 这个方法保持为空，以兼容现有API
        // 具体清理逻辑在cleanupOldConversations()中实现
    }

    /**
     * 创建新会话（无初始消息）
     */
    @Transactional
    public SessionMetadata createSession(String sessionId, String title) {
        if (sessionId == null || sessionId.trim().isEmpty()) {
            throw new IllegalArgumentException("会话ID不能为空");
        }

        // 检查会话是否已存在
        if (sessionExists(sessionId)) {
            return memoryMetadataStore.get(sessionId);
        }

        // 创建会话元数据
        SessionMetadata metadata = new SessionMetadata(sessionId, title);
        metadata.updateLastActive();

        // 存储到内存
        memoryMetadataStore.put(sessionId, metadata);

        // 创建空消息列表和索引列表
        memoryMessageStore.put(sessionId, new CopyOnWriteArrayList<>());
        memoryMessageIndexStore.put(sessionId, new CopyOnWriteArrayList<>());

        // 初始化下一个消息索引为0
        sessionNextMessageIndex.put(sessionId, 0);

        // 初始化已同步索引为-1（表示没有消息已同步）
        sessionMaxSyncedIndex.put(sessionId, -1);

        // 更新访问顺序
        updateAccessOrder(sessionId);

        // 同步到数据库（如果启用）
        if (dbSyncEnabled) {
            ConversationSessionEntity sessionEntity = convertToSessionEntity(sessionId, metadata);
            sessionMapper.insert(sessionEntity);
        }

        return metadata;
    }
}
