package com.example.agent.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.agent.rag.entity.DocumentVectorEntity;
import com.example.agent.rag.enums.RagDocumentStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentVectorMapper extends BaseMapper<DocumentVectorEntity> {

    int batchInsert(@Param("list") List<DocumentVectorEntity> documents);

    List<DocumentVectorEntity> selectBySessionId(String sessionId);

    List<DocumentVectorEntity> selectByDocumentType(String documentType);

    List<DocumentVectorEntity> selectActiveDocuments();

    List<DocumentVectorEntity> selectBySessionIdAndType(@Param("sessionId") String sessionId,
                                                        @Param("documentType") String documentType);

    int deactivateByDocumentId(Long documentId);

    int deactivateBySessionId(String sessionId);

    int deactivateByKnowledgeDocumentId(@Param("knowledgeDocumentId") Long knowledgeDocumentId);

    int countBySessionId(String sessionId);

    int getMaxChunkIndex(@Param("sessionId") String sessionId, @Param("documentType") String documentType);

    List<DocumentVectorEntity> selectSearchCandidates(@Param("sessionId") String sessionId,
                                                      @Param("status") RagDocumentStatus status,
                                                      @Param("limit") int limit);
}
