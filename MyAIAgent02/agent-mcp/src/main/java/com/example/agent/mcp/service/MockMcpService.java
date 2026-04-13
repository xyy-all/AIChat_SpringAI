package com.example.agent.mcp.service;

import com.example.agent.mcp.model.McpTool;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * MCP服务的模拟实现
 */
@Service
public class MockMcpService implements McpService {

    private final List<McpTool> availableTools = List.of(
            new McpTool("file_search", "Search files in the filesystem", List.of("query", "path")),
            new McpTool("calculator", "Calculate mathematical expressions", List.of("expression")),
            new McpTool("web_search", "Search the web for information", List.of("query")),
            new McpTool("weather", "Get weather information for a location", List.of("location")),
            new McpTool("time", "Get current time for a location", List.of("location"))
    );

    @Override
    public List<McpTool> getAvailableTools() {
        return availableTools;
    }

    @Override
    public String executeTool(String toolName, Map<String, Object> parameters) {
        // 模拟工具执行
        switch (toolName) {
            case "file_search":
                String query = (String) parameters.getOrDefault("query", "");
                String path = (String) parameters.getOrDefault("path", ".");
                return String.format("Mock file search result for query '%s' in path '%s': Found 3 matching files.", query, path);

            case "calculator":
                String expression = (String) parameters.getOrDefault("expression", "0");
                try {
                    // 简单计算器模拟
                    if (expression.contains("+")) {
                        String[] parts = expression.split("\\+");
                        double result = Double.parseDouble(parts[0].trim()) + Double.parseDouble(parts[1].trim());
                        return String.format("Calculation result: %s = %.2f", expression, result);
                    } else if (expression.contains("-")) {
                        String[] parts = expression.split("-");
                        double result = Double.parseDouble(parts[0].trim()) - Double.parseDouble(parts[1].trim());
                        return String.format("Calculation result: %s = %.2f", expression, result);
                    } else if (expression.contains("*")) {
                        String[] parts = expression.split("\\*");
                        double result = Double.parseDouble(parts[0].trim()) * Double.parseDouble(parts[1].trim());
                        return String.format("Calculation result: %s = %.2f", expression, result);
                    } else if (expression.contains("/")) {
                        String[] parts = expression.split("/");
                        double result = Double.parseDouble(parts[0].trim()) / Double.parseDouble(parts[1].trim());
                        return String.format("Calculation result: %s = %.2f", expression, result);
                    } else {
                        return String.format("Calculation result: %s = %s", expression, expression);
                    }
                } catch (Exception e) {
                    return "Error calculating expression: " + expression;
                }

            case "web_search":
                String searchQuery = (String) parameters.getOrDefault("query", "");
                return String.format("Mock web search results for '%s': 10 results found. Most relevant: Wikipedia article about %s.",
                        searchQuery, searchQuery);

            case "weather":
                String location = (String) parameters.getOrDefault("location", "Beijing");
                return String.format("Mock weather for %s: Sunny, 25°C, Humidity: 60%%, Wind: 10 km/h", location);

            case "time":
                String timeLocation = (String) parameters.getOrDefault("location", "UTC");
                return String.format("Mock current time for %s: 2026-04-05 14:30:00", timeLocation);

            default:
                return "Mock MCP tool execution result for " + toolName + " with parameters: " + parameters;
        }
    }

    @Override
    public boolean hasTool(String toolName) {
        return availableTools.stream().anyMatch(tool -> tool.getName().equals(toolName));
    }

    @Override
    public String getToolDescription(String toolName) {
        return availableTools.stream()
                .filter(tool -> tool.getName().equals(toolName))
                .map(McpTool::getDescription)
                .findFirst()
                .orElse(null);
    }
}