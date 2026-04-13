package com.example.agent.mcp.registry;

import com.example.agent.mcp.model.McpTool;
import com.example.agent.mcp.service.McpService;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MCP工具注册表，管理和缓存工具信息
 */
@Component
public class McpToolRegistry {

    private final McpService mcpService;
    private final Map<String, McpTool> toolCache = new ConcurrentHashMap<>();
    private final Map<String, String> toolDescriptions = new ConcurrentHashMap<>();
    private volatile boolean initialized = false;

    public McpToolRegistry(McpService mcpService) {
        this.mcpService = mcpService;
    }

    /**
     * 初始化工具注册表
     */
    public synchronized void initialize() {
        if (!initialized) {
            List<McpTool> tools = mcpService.getAvailableTools();
            tools.forEach(tool -> {
                toolCache.put(tool.getName(), tool);
                toolDescriptions.put(tool.getName(), tool.getDescription());
            });
            initialized = true;
        }
    }

    /**
     * 获取所有工具
     */
    public List<McpTool> getAllTools() {
        ensureInitialized();
        return new ArrayList<>(toolCache.values());
    }

    /**
     * 获取工具名称列表
     */
    public List<String> getToolNames() {
        ensureInitialized();
        return new ArrayList<>(toolCache.keySet());
    }

    /**
     * 根据名称获取工具
     */
    public Optional<McpTool> getTool(String name) {
        ensureInitialized();
        return Optional.ofNullable(toolCache.get(name));
    }

    /**
     * 检查工具是否存在
     */
    public boolean containsTool(String name) {
        ensureInitialized();
        return toolCache.containsKey(name);
    }

    /**
     * 获取工具描述
     */
    public Optional<String> getToolDescription(String name) {
        ensureInitialized();
        return Optional.ofNullable(toolDescriptions.get(name));
    }

    /**
     * 搜索工具（根据名称或描述）
     */
    public List<McpTool> searchTools(String keyword) {
        ensureInitialized();
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllTools();
        }

        String lowerKeyword = keyword.toLowerCase();
        return toolCache.values().stream()
                .filter(tool -> tool.getName().toLowerCase().contains(lowerKeyword) ||
                        tool.getDescription().toLowerCase().contains(lowerKeyword))
                .toList();
    }

    /**
     * 按类别分组工具（模拟分类）
     */
    public Map<String, List<McpTool>> getToolsByCategory() {
        ensureInitialized();
        Map<String, List<McpTool>> categories = new HashMap<>();

        // 模拟分类
        for (McpTool tool : toolCache.values()) {
            String category = guessCategory(tool);
            categories.computeIfAbsent(category, k -> new ArrayList<>()).add(tool);
        }

        return categories;
    }

    /**
     * 根据工具名称猜测类别
     */
    private String guessCategory(McpTool tool) {
        String name = tool.getName().toLowerCase();
        if (name.contains("search")) {
            return "search";
        } else if (name.contains("calc") || name.contains("math")) {
            return "calculation";
        } else if (name.contains("weather") || name.contains("time")) {
            return "information";
        } else {
            return "general";
        }
    }

    /**
     * 获取工具数量
     */
    public int getToolCount() {
        ensureInitialized();
        return toolCache.size();
    }

    /**
     * 清空缓存并重新初始化
     */
    public synchronized void refresh() {
        toolCache.clear();
        toolDescriptions.clear();
        initialized = false;
        initialize();
    }

    /**
     * 确保注册表已初始化
     */
    private void ensureInitialized() {
        if (!initialized) {
            initialize();
        }
    }
}