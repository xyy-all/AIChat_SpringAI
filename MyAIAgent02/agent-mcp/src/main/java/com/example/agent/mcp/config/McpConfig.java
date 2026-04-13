package com.example.agent.mcp.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * MCP模块配置类
 */
@Configuration
@ComponentScan("com.example.agent.mcp")
public class McpConfig {

    // 这个配置类确保MCP模块的组件被正确扫描
    // 由于使用了@ComponentScan，所有标记了@Component、@Service、@Repository等的类都会被自动注册

    // 如果需要配置特定的MCP相关bean，可以在这里添加

    // 示例：如果需要配置HTTP客户端用于真实的MCP连接
    // @Bean
    // public RestTemplate mcpRestTemplate() {
    //     return new RestTemplate();
    // }
}