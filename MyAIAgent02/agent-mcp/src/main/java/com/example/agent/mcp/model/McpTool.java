package com.example.agent.mcp.model;

import java.util.List;

/**
 * MCP工具模型类
 */
public class McpTool {

    private String name;
    private String description;
    private List<String> parameters;

    public McpTool() {
    }

    public McpTool(String name, String description, List<String> parameters) {
        this.name = name;
        this.description = description;
        this.parameters = parameters;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters;
    }

    @Override
    public String toString() {
        return "McpTool{name='" + name + "', description='" + description + "', parameters=" + parameters + "}";
    }
}