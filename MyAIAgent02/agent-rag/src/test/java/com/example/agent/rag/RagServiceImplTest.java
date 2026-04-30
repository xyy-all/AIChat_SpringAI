package com.example.agent.rag;

import com.example.agent.rag.config.RagProperties;
import com.example.agent.rag.dto.VectorStoreReindexResult;
import com.example.agent.rag.mapper.DocumentMapper;
import com.example.agent.rag.mapper.DocumentVectorMapper;
import com.example.agent.rag.store.RagVectorStoreBackend;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class RagServiceImplTest {

    @Test
    void reindexShouldReturnDatabaseBackendWhenProviderIsDatabase() {
        RagProperties properties = new RagProperties();
        properties.getVectorStore().setProvider(RagProperties.Provider.DATABASE);

        DocumentMapper documentMapper = mock(DocumentMapper.class);
        DocumentVectorMapper documentVectorMapper = mock(DocumentVectorMapper.class);
        RagVectorStoreBackend databaseBackend = mock(RagVectorStoreBackend.class);
        when(databaseBackend.getBackendId()).thenReturn("database");
        when(databaseBackend.isAvailable()).thenReturn(true);

        RagServiceImpl service = new RagServiceImpl(
                mock(EmbeddingModel.class),
                documentMapper,
                documentVectorMapper,
                properties,
                List.of(databaseBackend));

        VectorStoreReindexResult result = service.reindexActiveDocuments();

        assertEquals("database", result.getBackend());
        verifyNoInteractions(documentMapper, documentVectorMapper);
    }

    @Test
    void reindexShouldFallbackToDatabaseWhenQdrantUnavailable() {
        RagProperties properties = new RagProperties();
        properties.getVectorStore().setProvider(RagProperties.Provider.QDRANT);
        properties.getVectorStore().setLocalFallbackEnabled(true);

        DocumentMapper documentMapper = mock(DocumentMapper.class);
        DocumentVectorMapper documentVectorMapper = mock(DocumentVectorMapper.class);
        RagVectorStoreBackend databaseBackend = mock(RagVectorStoreBackend.class);
        when(databaseBackend.getBackendId()).thenReturn("database");
        when(databaseBackend.isAvailable()).thenReturn(true);

        RagServiceImpl service = new RagServiceImpl(
                mock(EmbeddingModel.class),
                documentMapper,
                documentVectorMapper,
                properties,
                List.of(databaseBackend));

        VectorStoreReindexResult result = service.reindexActiveDocuments();

        assertEquals("database", result.getBackend());
        verifyNoInteractions(documentMapper, documentVectorMapper);
    }
}
