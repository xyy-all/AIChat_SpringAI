package com.example.agent.api.dto;

public class CreateSessionRequest {
    private String title;
    private String sessionId; // 可选，如果为空则由系统生成

    public CreateSessionRequest() {
    }

    public CreateSessionRequest(String title) {
        this.title = title;
    }

    public CreateSessionRequest(String title, String sessionId) {
        this.title = title;
        this.sessionId = sessionId;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * 验证请求是否有效
     */
    public boolean isValid() {
        return title != null && !title.trim().isEmpty();
    }

    /**
     * 获取清理后的标题（去除首尾空格）
     */
    public String getCleanTitle() {
        return title != null ? title.trim() : "";
    }

    @Override
    public String toString() {
        return "CreateSessionRequest{" +
                "title='" + title + '\'' +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}