package com.example.agent.api;

import com.example.agent.core.config.AppConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.example.agent.api",
        "com.example.agent.core",
        "com.example.agent.rag",
        "com.example.agent.mcp",
        "com.example.agent.skills"
})
@EnableConfigurationProperties(AppConfiguration.class)
@MapperScan({
        "com.example.agent.core.mapper",
        "com.example.agent.rag.mapper",
        "com.example.agent.skills.mapper",
        "com.example.agent.api.mapper"
})
public class AiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentApplication.class, args);
    }
}