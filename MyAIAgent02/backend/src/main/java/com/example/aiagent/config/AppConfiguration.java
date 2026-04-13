package com.example.aiagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.PropertySource;

/**
 * 应用程序配置类，集中管理所有可配置参数
 */
@ConfigurationProperties(prefix = "app.config")
@PropertySource(value = "classpath:application-config.properties", encoding = "UTF-8")
public class AppConfiguration {

    // 内存缓存配置
    private MemoryCache memoryCache = new MemoryCache();
    
    // 对话服务配置
    private Conversation conversation = new Conversation();
    
    // ChatMemory工厂配置
    private ChatMemory chatMemory = new ChatMemory();

    // Getters and Setters
    public MemoryCache getMemoryCache() {
        return memoryCache;
    }

    public void setMemoryCache(MemoryCache memoryCache) {
        this.memoryCache = memoryCache;
    }

    public Conversation getConversation() {
        return conversation;
    }

    public void setConversation(Conversation conversation) {
        this.conversation = conversation;
    }

    public ChatMemory getChatMemory() {
        return chatMemory;
    }

    public void setChatMemory(ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
    }

    // 内存缓存配置内部类
    public static class MemoryCache {
        private int maxSessions = 100;           // 最大会话数
        private int maxMessagesPerSession = 20;  // 每个会话最大消息数
        private int ttlSeconds = 3600;           // TTL（秒）

        // Getters and Setters
        public int getMaxSessions() {
            return maxSessions;
        }

        public void setMaxSessions(int maxSessions) {
            this.maxSessions = maxSessions;
        }

        public int getMaxMessagesPerSession() {
            return maxMessagesPerSession;
        }

        public void setMaxMessagesPerSession(int maxMessagesPerSession) {
            this.maxMessagesPerSession = maxMessagesPerSession;
        }

        public int getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    // 对话服务配置内部类
    public static class Conversation {
        private int maxHistorySize = 20;         // 最大历史消息数
        private int maxConversations = 100;      // 最大对话数
        private int defaultWindowSize = 20;      // 默认窗口大小

        // Getters and Setters
        public int getMaxHistorySize() {
            return maxHistorySize;
        }

        public void setMaxHistorySize(int maxHistorySize) {
            this.maxHistorySize = maxHistorySize;
        }

        public int getMaxConversations() {
            return maxConversations;
        }

        public void setMaxConversations(int maxConversations) {
            this.maxConversations = maxConversations;
        }

        public int getDefaultWindowSize() {
            return defaultWindowSize;
        }

        public void setDefaultWindowSize(int defaultWindowSize) {
            this.defaultWindowSize = defaultWindowSize;
        }
    }

    // ChatMemory配置内部类
    public static class ChatMemory {
        private int defaultWindowSize = 20;      // 默认窗口大小
        private int smallWindowSize = 10;        // 小窗口大小
        private int mediumWindowSize = 20;       // 中等窗口大小
        private int largeWindowSize = 50;        // 大窗口大小
        private int xlargeWindowSize = 100;      // 超大窗口大小

        // Getters and Setters
        public int getDefaultWindowSize() {
            return defaultWindowSize;
        }

        public void setDefaultWindowSize(int defaultWindowSize) {
            this.defaultWindowSize = defaultWindowSize;
        }

        public int getSmallWindowSize() {
            return smallWindowSize;
        }

        public void setSmallWindowSize(int smallWindowSize) {
            this.smallWindowSize = smallWindowSize;
        }

        public int getMediumWindowSize() {
            return mediumWindowSize;
        }

        public void setMediumWindowSize(int mediumWindowSize) {
            this.mediumWindowSize = mediumWindowSize;
        }

        public int getLargeWindowSize() {
            return largeWindowSize;
        }

        public void setLargeWindowSize(int largeWindowSize) {
            this.largeWindowSize = largeWindowSize;
        }

        public int getXlargeWindowSize() {
            return xlargeWindowSize;
        }

        public void setXlargeWindowSize(int xlargeWindowSize) {
            this.xlargeWindowSize = xlargeWindowSize;
        }
    }
}