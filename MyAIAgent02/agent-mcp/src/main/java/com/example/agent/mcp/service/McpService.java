package com.example.agent.mcp.service;

import com.example.agent.mcp.model.McpTool;

import java.util.List;
import java.util.Map;

/**
 * MCP服务接口
 */
public interface McpService {

    /**
     * 获取可用的MCP工具列表
     */
    List<McpTool> getAvailableTools();

    /**
     * 执行MCP工具
     * @param toolName 工具名称
     * @param parameters 工具参数
     * @return 执行结果
     */
    String executeTool(String toolName, Map<String, Object> parameters);

    /**
     * 检查工具是否存在
     * @param toolName 工具名称
     * @return 是否存在
     */
    boolean hasTool(String toolName);

    /**
     * 获取工具描述
     * @param toolName 工具名称
     * @return 工具描述，如果不存在返回null
     */
    String getToolDescription(String toolName);
}