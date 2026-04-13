package com.example.agent.core.service;

import com.example.agent.mcp.service.McpService;
import com.example.agent.rag.RagService;
import com.example.agent.rag.dto.RagHit;
import com.example.agent.skills.service.SkillService;
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
    private final SkillService skillService;
    private final McpService mcpService;
    private final MultiLayerConversationService conversationService;
    private final ChatMemoryFactory chatMemoryFactory;

    public ChatService(ChatClient chatClient, RagService ragService,
                       SkillService skillService, McpService mcpService,
                       MultiLayerConversationService conversationService,
                       ChatMemoryFactory chatMemoryFactory) {
        this.chatClient = chatClient;
        this.ragService = ragService;
        this.skillService = skillService;
        this.mcpService = mcpService;
        this.conversationService = conversationService;
        this.chatMemoryFactory = chatMemoryFactory;
    }

    private String formatMessagesForPrompt(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("\n以下是之前的对话历史（最近的在最后）：\n");
        for (Message message : messages) {
            builder.append(message.getMessageType().name())
                    .append(": ")
                    .append(message.getText())
                    .append("\n");
        }
        builder.append("\n请基于以上对话历史回答用户的新问题。\n");
        return builder.toString();
    }

    private String buildEnhancedPrompt(String userMessage, String sessionId, String historyContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("你是一个 AI 助手，具有以下能力：\n");
        prompt.append("1. 对话能力：可以理解上下文并进行连续对话\n");
        prompt.append("2. 知识检索：可以检索知识库中的文档\n");
        prompt.append("3. 工具调用：可以调用外部工具完成任务\n");
        prompt.append("4. 技能执行：可以执行系统内置技能\n");

        if (!historyContext.trim().isEmpty()) {
            prompt.append(historyContext).append("\n");
        }

        List<RagHit> relevantDocs = ragService.searchSimilar(userMessage, sessionId, 3);
        if (!relevantDocs.isEmpty()) {
            prompt.append("相关参考文档：\n");
            for (int i = 0; i < relevantDocs.size(); i++) {
                RagHit hit = relevantDocs.get(i);
                prompt.append(i + 1).append(". ");
                if (hit.getTitle() != null && !hit.getTitle().isBlank()) {
                    prompt.append("[").append(hit.getTitle()).append("] ");
                }
                prompt.append(hit.getChunkContent());
                prompt.append(" (score=")
                        .append(String.format("%.3f", hit.getScore()))
                        .append(")\n");
            }
            prompt.append("\n");
        }

        var availableTools = mcpService.getAvailableTools();
        if (!availableTools.isEmpty()) {
            prompt.append("可用工具：\n");
            for (var tool : availableTools) {
                prompt.append("- ").append(tool.getName()).append(": ").append(tool.getDescription());
                if (tool.getParameters() != null && !tool.getParameters().isEmpty()) {
                    prompt.append(" (参数: ").append(String.join(", ", tool.getParameters())).append(")");
                }
                prompt.append("\n");
            }
            prompt.append("\n");
        }

        var skillInfos = skillService.getAllSkillInfos();
        if (!skillInfos.isEmpty()) {
            prompt.append("可用技能：\n");
            for (var skillInfo : skillInfos) {
                prompt.append("- ").append(skillInfo.get("name"))
                        .append(": ")
                        .append(skillInfo.get("description"))
                        .append("\n");
            }
            prompt.append("\n");
        }

        prompt.append("用户问题: ").append(userMessage).append("\n");
        prompt.append("助手回答: ");
        return prompt.toString();
    }

    private String handleToolCall(String toolName, Map<String, Object> parameters) {
        try {
            if (mcpService.hasTool(toolName)) {
                return mcpService.executeTool(toolName, parameters);
            }

            String skillResult = skillService.executeSkill(toolName, parameters.toString());
            if (!skillResult.contains("Skill not found")) {
                return skillResult;
            }

            return "工具或技能未找到: " + toolName;
        } catch (Exception exception) {
            return "执行工具时出错: " + exception.getMessage();
        }
    }

    private boolean needsToolCall(String message) {
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains("计算") || lowerMessage.contains("calculator")
                || lowerMessage.contains("搜索") || lowerMessage.contains("search")
                || lowerMessage.contains("天气") || lowerMessage.contains("weather")
                || lowerMessage.contains("时间") || lowerMessage.contains("time")
                || lowerMessage.contains("文件") || lowerMessage.contains("file");
    }

    public Flux<String> streamChat(String message, String sessionId) {
        ChatMemory chatMemory = chatMemoryFactory.getDefaultChatMemory();

        Message userMessage = new UserMessage(message);
        chatMemory.add(sessionId, userMessage);

        List<Message> history = chatMemory.get(sessionId);
        String historyContext = formatMessagesForPrompt(history);
        String prompt = buildEnhancedPrompt(message, sessionId, historyContext);

        StringBuilder fullResponse = new StringBuilder();

        return chatClient.prompt()
                .user(prompt)
                .stream()
                .chatResponse()
                .map(chatResponse -> chatResponse.getResult().getOutput().getText())
                .filter(content -> content != null)
                .doOnNext(token -> {
                    if (token != null) {
                        fullResponse.append(token);
                    }
                })
                .doOnComplete(() -> {
                    String response = fullResponse.toString();
                    if (!response.isEmpty()) {
                        chatMemory.add(sessionId, new AssistantMessage(response));
                    }
                })
                .doOnError(error -> chatMemory.add(sessionId,
                        new AssistantMessage("[抱歉，生成响应时出现错误: " + error.getMessage() + "]")));
    }

    public String chat(String message, String sessionId) {
        StringBuilder response = new StringBuilder();
        streamChat(message, sessionId)
                .doOnNext(response::append)
                .blockLast();
        return response.toString();
    }

    public Map<String, Object> getServiceStatus() {
        return Map.of(
                "ragEnabled", ragService != null,
                "skillsCount", skillService.getAllSkillInfos().size(),
                "mcpToolsCount", mcpService.getAvailableTools().size(),
                "conversationService", conversationService.getClass().getSimpleName()
        );
    }
}
