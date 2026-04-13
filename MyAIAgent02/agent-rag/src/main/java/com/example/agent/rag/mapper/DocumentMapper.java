package com.example.agent.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.agent.rag.entity.DocumentEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentMapper extends BaseMapper<DocumentEntity> {

    DocumentEntity selectActiveByHashAndSessionId(@Param("sessionId") String sessionId,
                                                  @Param("contentHash") String contentHash);

    List<DocumentEntity> selectBySessionIdOrGlobal(@Param("sessionId") String sessionId);

    List<DocumentEntity> selectActiveByIds(@Param("ids") List<Long> ids);

    int deactivateByKnowledgeDocumentId(@Param("knowledgeDocumentId") Long knowledgeDocumentId);
}
