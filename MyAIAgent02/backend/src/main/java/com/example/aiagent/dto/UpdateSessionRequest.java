package com.example.aiagent.dto;

public class UpdateSessionRequest {
    private String title;

    public UpdateSessionRequest() {
    }

    public UpdateSessionRequest(String title) {
        this.title = title;
    }

    // Getters and setters
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
        return "UpdateSessionRequest{" +
                "title='" + title + '\'' +
                '}';
    }
}