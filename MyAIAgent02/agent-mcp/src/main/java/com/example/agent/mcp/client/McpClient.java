package com.example.agent.mcp.client;

import com.example.agent.mcp.model.McpTool;
import com.example.agent.mcp.service.McpService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * MCP客户端，封装对MCP服务的调用
 */
@Component
public class McpClient {

    private final McpService mcpService;

    public McpClient(McpService mcpService) {
        this.mcpService = mcpService;
    }

    /**
     * 列出所有可用的工具
     */
    public List<McpTool> listTools() {
        return mcpService.getAvailableTools();
    }

    /**
     * 调用工具执行
     */
    public String callTool(String toolName, Map<String, Object> parameters) {
        if (!mcpService.hasTool(toolName)) {
            throw new IllegalArgumentException("Tool not found: " + toolName);
        }

        try {
            return mcpService.executeTool(toolName, parameters);
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute tool: " + toolName, e);
        }
    }

    /**
     * 批量调用工具
     */
    public Map<String, String> callTools(Map<String, Map<String, Object>> toolCalls) {
        return toolCalls.entrySet().stream()
                .collect(java.util.stream.Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> callTool(entry.getKey(), entry.getValue())
                ));
    }

    /**
     * 检查工具是否存在
     */
    public boolean hasTool(String toolName) {
        return mcpService.hasTool(toolName);
    }

    /**
     * 获取工具描述
     */
    public String getToolDescription(String toolName) {
        return mcpService.getToolDescription(toolName);
    }

    /**
     * 获取工具数量
     */
    public int getToolCount() {
        return mcpService.getAvailableTools().size();
    }
}