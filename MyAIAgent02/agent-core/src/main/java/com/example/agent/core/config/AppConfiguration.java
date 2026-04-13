package com.example.agent.core.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 会话与记忆模块的自定义配置。
 *
 * <p>这类配置不再混在 application.yml 中，而是统一放在独立的
 * {@code app-session.yml} 里，通过 {@code app.session.*} 前缀绑定。
 */
@ConfigurationProperties(prefix = "app.session")
public class AppConfiguration {

    /** 内存缓存层的容量和保留策略。 */
    private MemoryCache memoryCache = new MemoryCache();

    /** 会话服务本身的通用限制。 */
    private Conversation conversation = new Conversation();

    /** Spring AI ChatMemory 的窗口大小预设。 */
    private ChatMemory chatMemory = new ChatMemory();

    /** 会话数据与数据库之间的同步策略。 */
    private DbSync dbSync = new DbSync();

    /** 会话摘要相关配置，当前主要用于集中管理默认值。 */
    private Summary summary = new Summary();

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

    public DbSync getDbSync() {
        return dbSync;
    }

    public void setDbSync(DbSync dbSync) {
        this.dbSync = dbSync;
    }

    public Summary getSummary() {
        return summary;
    }

    public void setSummary(Summary summary) {
        this.summary = summary;
    }

    public static class MemoryCache {
        private int maxSessions = 100;
        private int maxMessagesPerSession = 20;
        private int ttlSeconds = 3600;

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

    public static class Conversation {
        private int maxHistorySize = 20;
        private int maxConversations = 100;
        private int defaultWindowSize = 20;

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

    public static class ChatMemory {
        private int defaultWindowSize = 20;
        private int smallWindowSize = 10;
        private int mediumWindowSize = 20;
        private int largeWindowSize = 50;
        private int xlargeWindowSize = 100;

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

    public static class DbSync {
        private boolean enabled = true;
        private boolean lazyLoad = true;
        private boolean writeThrough = true;
        private int batchSize = 50;
        private int flushIntervalMs = 5000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isLazyLoad() {
            return lazyLoad;
        }

        public void setLazyLoad(boolean lazyLoad) {
            this.lazyLoad = lazyLoad;
        }

        public boolean isWriteThrough() {
            return writeThrough;
        }

        public void setWriteThrough(boolean writeThrough) {
            this.writeThrough = writeThrough;
        }

        public int getBatchSize() {
            return batchSize;
        }

        public void setBatchSize(int batchSize) {
            this.batchSize = batchSize;
        }

        public int getFlushIntervalMs() {
            return flushIntervalMs;
        }

        public void setFlushIntervalMs(int flushIntervalMs) {
            this.flushIntervalMs = flushIntervalMs;
        }
    }

    public static class Summary {
        private boolean enabled = true;
        private int generateThreshold = 10;
        private int maxSummariesPerSession = 50;
        private double similarityThreshold = 0.7d;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getGenerateThreshold() {
            return generateThreshold;
        }

        public void setGenerateThreshold(int generateThreshold) {
            this.generateThreshold = generateThreshold;
        }

        public int getMaxSummariesPerSession() {
            return maxSummariesPerSession;
        }

        public void setMaxSummariesPerSession(int maxSummariesPerSession) {
            this.maxSummariesPerSession = maxSummariesPerSession;
        }

        public double getSimilarityThreshold() {
            return similarityThreshold;
        }

        public void setSimilarityThreshold(double similarityThreshold) {
            this.similarityThreshold = similarityThreshold;
        }
    }
}
