package com.example.aiagent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import com.example.aiagent.config.AppConfiguration;

@SpringBootApplication
@EnableConfigurationProperties(AppConfiguration.class)
@MapperScan("com.example.aiagent.mapper")
public class AiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiAgentApplication.class, args);
    }
}