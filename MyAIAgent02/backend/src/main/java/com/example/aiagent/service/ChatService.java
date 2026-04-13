package com.example.aiagent.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final RagService ragService;
    private final MultiLayerConversationService conversationService;
    private final ChatMemoryFactory chatMemoryFactory;

    public ChatService(ChatClient chatClient, RagService ragService,
                      MultiLayerConversationService conversationService, ChatMemoryFactory chatMemoryFactory) {
        this.chatClient = chatClient;
        this.ragService = ragService;
        this.conversationService = conversationService;
        this.chatMemoryFactory = chatMemoryFactory;
    }

    /**
     * 格式化Spring AI Message列表为提示词字符串
     */
    private String formatMessagesForPrompt(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("\n以下是之前的对话历史（最近的在最后）：\n");

        for (Message message : messages) {
            String role = message.getMessageType().name();
            String content = message.getText();
            sb.append(role).append(": ").append(content).append("\n");
        }

        sb.append("\n请基于以上对话历史回答用户的新问题。");
        return sb.toString();
    }

    public Flux<String> streamChat(String message, String sessionId) {
        // 1. 获取ChatMemory实例（默认窗口大小）
        ChatMemory chatMemory = chatMemoryFactory.getDefaultChatMemory();

        // 2. 创建用户消息并添加到ChatMemory
        Message userMessage = new UserMessage(message);
        chatMemory.add(sessionId, userMessage);

        // 3. 获取历史消息并格式化
        List<Message> history = chatMemory.get(sessionId);
        String historyContext = formatMessagesForPrompt(history);

        // 4. 构建包含历史上下文的提示词
        String prompt;
        if (historyContext.trim().isEmpty()) {
            prompt = message;
        } else {
            prompt = historyContext + "\n\n用户新问题: " + message + "\n助手回答: ";
        }

        // 5. 用于收集完整响应的StringBuilder
        StringBuilder fullResponse = new StringBuilder();

        // 6. 生成流式响应并收集
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .chatResponse()
                .map(chatResponse -> chatResponse.getResult().getOutput().getText())
                .filter(content -> content != null)
                .doOnNext(token -> {
                    // 收集每个token
                    if (token != null) {
                        fullResponse.append(token);
                    }
                })
                .doOnComplete(() -> {
                    // 流完成后添加助手消息到ChatMemory
                    String response = fullResponse.toString();
                    if (!response.isEmpty()) {
                        Message assistantMessage = new AssistantMessage(response);
                        chatMemory.add(sessionId, assistantMessage);
                    }
                })
                .doOnError(error -> {
                    // 发生错误时，添加错误消息到ChatMemory
                    Message errorMessage = new AssistantMessage(
                        "[抱歉，生成响应时出现错误: " + error.getMessage() + "]");
                    chatMemory.add(sessionId, errorMessage);
                });

    }
}