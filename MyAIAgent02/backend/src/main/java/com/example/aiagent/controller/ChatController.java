package com.example.aiagent.controller;

import com.example.aiagent.dto.*;
import com.example.aiagent.service.ChatService;
import com.example.aiagent.service.MultiLayerConversationService;
import com.example.aiagent.service.RagService;
import com.example.aiagent.service.SkillService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final RagService ragService;
    private final SkillService skillService;
    private final MultiLayerConversationService conversationService;

    public ChatController(ChatService chatService, RagService ragService, SkillService skillService, MultiLayerConversationService conversationService) {
        this.chatService = chatService;
        this.ragService = ragService;
        this.skillService = skillService;
        this.conversationService = conversationService;
    }


    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        return chatService.streamChat(request.getMessage(), request.getSessionId());
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadDocument(@RequestBody DocumentUploadRequest request) {
        ragService.addDocument(request.getText(), request.getDocumentId());
        return ResponseEntity.ok("Document uploaded successfully");
    }

    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Agent is running");
    }

    @PostMapping("/skill")
    public ResponseEntity<String> executeSkill(@RequestBody SkillRequest request) {
        String result = skillService.executeSkill(request.getSkillName(), request.getInput());
        return ResponseEntity.ok(result);
    }

    @GetMapping("/skills")
    public ResponseEntity<?> listSkills() {
        return ResponseEntity.ok(skillService.getAllSkillInfos());
    }

    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(conversationService.getHistory(sessionId));
    }

    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<?> clearHistory(@PathVariable String sessionId) {
        conversationService.clearHistory(sessionId);
        return ResponseEntity.ok("对话历史已清除");
    }

    // =================== 会话管理API ===================

    /**
     * 获取所有会话列表（含元数据）
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionMetadata>> listSessions() {
        List<SessionMetadata> sessions = conversationService.getAllSessionMetadata();
        return ResponseEntity.ok(sessions);
    }

    /**
     * 获取特定会话的元数据
     */
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionMetadata> getSession(@PathVariable String sessionId) {
        SessionMetadata metadata = conversationService.getSessionMetadata(sessionId);
        if (metadata == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(metadata);
    }

    /**
     * 创建新会话
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionMetadata> createSession(@RequestBody CreateSessionRequest request) {
        if (!request.isValid()) {
            return ResponseEntity.badRequest().build();
        }

        // 生成sessionId（如果请求中没有提供）
        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = generateSessionId();
        }

        // 检查会话是否已存在
        if (conversationService.sessionExists(sessionId)) {
            return ResponseEntity.status(409).build(); // 冲突
        }

        // 使用MultiLayerConversationService创建新会话
        SessionMetadata metadata = conversationService.createSession(sessionId, request.getCleanTitle());

        return ResponseEntity.ok(metadata);
    }

    /**
     * 更新会话（重命名）
     */
    @PutMapping("/sessions/{sessionId}")
    public ResponseEntity<SessionMetadata> updateSession(@PathVariable String sessionId,
                                                         @RequestBody UpdateSessionRequest request) {
        if (!request.isValid()) {
            return ResponseEntity.badRequest().build();
        }

        boolean updated = conversationService.updateSessionTitle(sessionId, request.getCleanTitle());
        if (!updated) {
            return ResponseEntity.notFound().build();
        }

        SessionMetadata metadata = conversationService.getSessionMetadata(sessionId);
        return ResponseEntity.ok(metadata);
    }

    /**
     * 删除会话（包括历史消息和元数据）
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> deleteSession(@PathVariable String sessionId) {
        boolean deleted = conversationService.deleteSession(sessionId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("会话已删除");
    }

    /**
     * 清理超过限制的会话
     */
    @PostMapping("/sessions/cleanup")
    public ResponseEntity<?> cleanupSessions() {
        conversationService.cleanupExcessSessions();
        return ResponseEntity.ok("已清理过期会话");
    }

    /**
     * 生成唯一的sessionId
     */
    private String generateSessionId() {
        return "session-" + UUID.randomUUID().toString().substring(0, 8);
    }
}