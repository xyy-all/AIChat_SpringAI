package com.example.aiagent.service;

import com.example.aiagent.config.AppConfiguration;
import com.example.aiagent.dto.ChatMessage;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 工厂类，用于创建和管理不同窗口大小的ChatMemory实例
 * 支持动态窗口大小选择
 */
@Component
public class ChatMemoryFactory {

    // 缓存不同窗口大小的ChatMemory实例
    private final Map<Integer, ChatMemory> memoryCache = new ConcurrentHashMap<>();
    private final IConversationService conversationService;
    
    @Autowired
    private AppConfiguration appConfig;

    // 默认窗口大小 - 使用方法获取
    public static int DEFAULT_WINDOW_SIZE = 20; // 默认值

    public ChatMemoryFactory(IConversationService conversationService) {
        this.conversationService = conversationService;
    }
    
    @PostConstruct
    public void init() {
        // 初始化配置参数
        DEFAULT_WINDOW_SIZE = appConfig.getChatMemory().getDefaultWindowSize();
    }

    /**
     * 获取指定窗口大小的ChatMemory实例
     * @param windowSize 窗口大小（保留最近N条消息）
     * @return ChatMemory实例
     */
    public ChatMemory getWindowChatMemory(int windowSize) {
        if (windowSize <= 0) {
            throw new IllegalArgumentException("窗口大小必须大于0");
        }

        return memoryCache.computeIfAbsent(windowSize, size ->
            new CustomChatMemory(conversationService, size)
        );
    }

    /**
     * 获取默认窗口大小的ChatMemory实例（20条消息）
     * @return 默认ChatMemory实例
     */
    public ChatMemory getDefaultChatMemory() {
        return getWindowChatMemory(appConfig.getChatMemory().getDefaultWindowSize());
    }

    /**
     * 获取常用窗口大小的ChatMemory实例
     * @param sizeType 窗口大小类型
     * @return ChatMemory实例
     */
    public ChatMemory getChatMemory(WindowSizeType sizeType) {
        return switch (sizeType) {
            case SMALL -> getWindowChatMemory(appConfig.getChatMemory().getSmallWindowSize());
            case MEDIUM -> getWindowChatMemory(appConfig.getChatMemory().getMediumWindowSize());
            case LARGE -> getWindowChatMemory(appConfig.getChatMemory().getLargeWindowSize());
            case XLARGE -> getWindowChatMemory(appConfig.getChatMemory().getXlargeWindowSize());
        };
    }

    /**
     * 清理指定窗口大小的ChatMemory实例
     * @param windowSize 窗口大小
     */
    public void clearChatMemory(int windowSize) {
        memoryCache.remove(windowSize);
    }

    /**
     * 清理所有缓存的ChatMemory实例
     */
    public void clearAll() {
        memoryCache.clear();
    }

    /**
     * 窗口大小类型枚举
     */
    public enum WindowSizeType {
        SMALL,      // 10条消息
        MEDIUM,     // 20条消息（默认）
        LARGE,      // 50条消息
        XLARGE      // 100条消息
    }

    /**
     * 自定义ChatMemory实现，包装ConversationService
     */
    private static class CustomChatMemory implements ChatMemory {
        private final IConversationService conversationService;
        private final int windowSize;

        public CustomChatMemory(IConversationService conversationService, int windowSize) {
            this.conversationService = conversationService;
            this.windowSize = windowSize;
        }
                
        @Override
        public void add(String conversationId, List<Message> messages) {
            for (Message message : messages) {
                add(conversationId, message);
            }
        }
        
        @Override
        public void add(String sessionId, org.springframework.ai.chat.messages.Message message) {
            // 将Spring AI Message转换为自定义Message
            String role = extractRoleFromMessage(message);
            String content = extractContentFromMessage(message);
            conversationService.addMessage(sessionId, role, content);
        }

        @Override
        public java.util.List<org.springframework.ai.chat.messages.Message> get(String sessionId) {
            // 从ConversationService获取历史消息
            List<ChatMessage> customMessages = conversationService.getRecentHistory(sessionId, windowSize);
            // 转换为Spring AI Message列表
            return customMessages.stream()
                .map(this::convertToSpringAiMessage)
                .collect(java.util.stream.Collectors.toList());
        }

        @Override
        public void clear(String sessionId) {
            conversationService.clearHistory(sessionId);
        }

        // 转换辅助方法
        private String extractRoleFromMessage(org.springframework.ai.chat.messages.Message message) {
            // 尝试获取消息角色
            try {
                return message.getMessageType().name();
            } catch (Exception e) {
                // 回退方案：从内容或元数据推断
                return "user"; // 默认值
            }
        }

        private String extractContentFromMessage(org.springframework.ai.chat.messages.Message message) {
            try {
                return message.getText();
            } catch (Exception e) {
                return "";
            }
        }

        private org.springframework.ai.chat.messages.Message convertToSpringAiMessage(ChatMessage customMessage) {
            // 使用MessageBuilder创建Spring AI Message
            MessageType messageType = convertRoleToMessageType(customMessage.getRole());
            if (messageType == MessageType.USER) {
                return new UserMessage(customMessage.getContent());
            } else if (messageType == MessageType.ASSISTANT) {
                return new org.springframework.ai.chat.messages.AssistantMessage(customMessage.getContent());
            } else { // SYSTEM or other
                return new org.springframework.ai.chat.messages.SystemMessage(customMessage.getContent());
            }
        }

        private MessageType convertRoleToMessageType(String role) {
            if (role == null) {
                return MessageType.USER;
            }
            String upperRole = role.toUpperCase();
            try {
                return MessageType.valueOf(upperRole);
            } catch (IllegalArgumentException e) {
                // 如果role不是有效的MessageType，尝试映射
                if ("user".equalsIgnoreCase(role)) {
                    return MessageType.USER;
                } else if ("assistant".equalsIgnoreCase(role)) {
                    return MessageType.ASSISTANT;
                } else if ("system".equalsIgnoreCase(role)) {
                    return MessageType.SYSTEM;
                } else {
                    return MessageType.USER; // 默认值
                }
            }
        }
    }
}