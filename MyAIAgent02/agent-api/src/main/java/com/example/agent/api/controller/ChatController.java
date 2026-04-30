package com.example.agent.api.controller;

import com.example.agent.api.dto.CreateSessionRequest;
import com.example.agent.api.dto.DocumentUploadRequest;
import com.example.agent.api.dto.SkillRequest;
import com.example.agent.api.dto.UpdateSessionRequest;
import com.example.agent.core.dto.ChatRequest;
import com.example.agent.core.dto.SessionMetadata;
import com.example.agent.core.service.ChatService;
import com.example.agent.core.service.MultiLayerConversationService;
import com.example.agent.rag.RagService;
import com.example.agent.rag.dto.DocumentIngestResult;
import com.example.agent.rag.dto.VectorStoreReindexResult;
import com.example.agent.skills.service.SkillService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;
    private final RagService ragService;
    private final SkillService skillService;
    private final MultiLayerConversationService conversationService;

    public ChatController(ChatService chatService,
                          RagService ragService,
                          SkillService skillService,
                          MultiLayerConversationService conversationService) {
        this.chatService = chatService;
        this.ragService = ragService;
        this.skillService = skillService;
        this.conversationService = conversationService;
    }

    /**
     * 流式返回聊天结果，便于前端按增量内容实时渲染。
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody ChatRequest request) {
        return chatService.streamChat(request.getMessage(), request.getSessionId());
    }

    /**
     * 兼容旧版客户端的文档上传入口，内部仍走统一的文档入库流程。
     */
    @PostMapping("/upload")
    public ResponseEntity<DocumentIngestResult> uploadDocument(@RequestBody DocumentUploadRequest request) {
        return ResponseEntity.ok(ragService.ingestDocument(request.toIngestRequest()));
    }

    /**
     * 将文档写入持久化 RAG 知识库。
     */
    @PostMapping("/documents")
    public ResponseEntity<DocumentIngestResult> ingestDocument(@RequestBody DocumentUploadRequest request) {
        return ResponseEntity.ok(ragService.ingestDocument(request.toIngestRequest()));
    }

    /**
     * 查询当前 RAG 管理的文档列表，可按会话 ID 进行过滤。
     */
    @GetMapping("/documents")
    public ResponseEntity<List<DocumentIngestResult>> listDocuments(@RequestParam(required = false) String sessionId) {
        return ResponseEntity.ok(ragService.listDocuments(sessionId));
    }

    /**
     * 根据知识库文档 ID 查询单个文档详情。
     */
    @GetMapping("/documents/{knowledgeDocumentId}")
    public ResponseEntity<DocumentIngestResult> getDocument(@PathVariable Long knowledgeDocumentId) {
        DocumentIngestResult result = ragService.getDocument(knowledgeDocumentId);
        if (result == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(result);
    }

    /**
     * 停用指定文档，使其不再参与后续检索。
     */
    @DeleteMapping("/documents/{knowledgeDocumentId}")
    public ResponseEntity<String> deleteDocument(@PathVariable Long knowledgeDocumentId) {
        boolean deleted = ragService.deactivateDocument(knowledgeDocumentId);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok("文档已停用");
    }

    /**
     * 将当前激活文档重新同步到当前向量检索后端。
     */
    @PostMapping("/documents/reindex")
    public ResponseEntity<VectorStoreReindexResult> reindexDocuments() {
        return ResponseEntity.ok(ragService.reindexActiveDocuments());
    }

    /**
     * 健康检查接口，用于本地联调和部署探活。
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("AI Agent is running");
    }

    /**
     * 执行指定技能，并返回技能执行结果。
     */
    @PostMapping("/skill")
    public ResponseEntity<String> executeSkill(@RequestBody SkillRequest request) {
        String result = skillService.executeSkill(request.getSkillName(), request.getInput());
        return ResponseEntity.ok(result);
    }

    /**
     * 返回全部技能定义，供前端展示和选择。
     */
    @GetMapping("/skills")
    public ResponseEntity<?> listSkills() {
        return ResponseEntity.ok(skillService.getAllSkillInfos());
    }

    /**
     * 获取指定会话的完整历史消息。
     */
    @GetMapping("/history/{sessionId}")
    public ResponseEntity<?> getHistory(@PathVariable String sessionId) {
        return ResponseEntity.ok(conversationService.getHistory(sessionId));
    }

    /**
     * 清空指定会话的历史记录，同时清理内存和持久化数据。
     */
    @DeleteMapping("/history/{sessionId}")
    public ResponseEntity<?> clearHistory(@PathVariable String sessionId) {
        conversationService.clearHistory(sessionId);
        return ResponseEntity.ok("对话历史已清空");
    }

    // =================== 会话管理 API ===================

    /**
     * 获取全部会话及其元数据。
     */
    @GetMapping("/sessions")
    public ResponseEntity<List<SessionMetadata>> listSessions() {
        List<SessionMetadata> sessions = conversationService.getAllSessionMetadata();
        return ResponseEntity.ok(sessions);
    }

    /**
     * 获取单个会话的元数据信息。
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
     * 创建新会话；如果请求未提供 sessionId，则自动生成一个。
     */
    @PostMapping("/sessions")
    public ResponseEntity<SessionMetadata> createSession(@RequestBody CreateSessionRequest request) {
        if (!request.isValid()) {
            return ResponseEntity.badRequest().build();
        }

        String sessionId = request.getSessionId();
        if (sessionId == null || sessionId.trim().isEmpty()) {
            sessionId = generateSessionId();
        }

        if (conversationService.sessionExists(sessionId)) {
            return ResponseEntity.status(409).build();
        }

        SessionMetadata metadata = conversationService.createSession(sessionId, request.getCleanTitle());
        return ResponseEntity.ok(metadata);
    }

    /**
     * 更新会话标题，并返回更新后的最新元数据。
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
     * 删除指定会话，并一并清理其消息和元数据。
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
     * 手动触发超限会话清理，释放超过配置上限的内存会话。
     */
    @PostMapping("/sessions/cleanup")
    public ResponseEntity<?> cleanupSessions() {
        conversationService.cleanupExcessSessions();
        return ResponseEntity.ok("过期会话已清理");
    }

    /**
     * 当调用方未传入 sessionId 时，生成一个简短的兜底会话 ID。
     */
    private String generateSessionId() {
        return "session-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
