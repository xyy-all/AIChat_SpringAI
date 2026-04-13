package com.example.agent.core.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiConfig {

    /*@Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.create(chatModel);
    }*/

    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.build();
    }

    // 移除未使用的InMemoryChatMemory配置，使用ChatMemoryFactory代替
    // @Bean
    // public ChatMemory chatMemory() {
    //     InMemoryChatMemoryProperties properties = new InMemoryChatMemoryProperties();
    //     properties.setWindowSize(20); // 默认窗口大小：保留最近20条消息
    //     return new InMemoryChatMemory(properties);
    // }

    // DashScope 配置等...
}