package com.example.agent.rag.config;

import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import org.apache.commons.lang3.StringUtils;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(RagProperties.class)
public class AiConfigRag {

    @Bean(destroyMethod = "close")
    @ConditionalOnProperty(prefix = "app.rag.vector-store", name = "provider", havingValue = "QDRANT")
    public QdrantClient qdrantClient(RagProperties ragProperties) {
        RagProperties.QdrantProperties qdrant = ragProperties.getVectorStore().getQdrant();
        QdrantGrpcClient.Builder builder = QdrantGrpcClient.newBuilder(
                qdrant.getHost(),
                qdrant.getGrpcPort(),
                qdrant.isUseTls());

        if (StringUtils.isNotBlank(qdrant.getApiKey())) {
            builder.withApiKey(qdrant.getApiKey());
        }
        if (qdrant.getTimeoutSeconds() > 0) {
            builder.withTimeout(Duration.ofSeconds(qdrant.getTimeoutSeconds()));
        }

        return new QdrantClient(builder.build());
    }

    @Bean("qdrantVectorStore")
    @ConditionalOnProperty(prefix = "app.rag.vector-store", name = "provider", havingValue = "QDRANT")
    public VectorStore qdrantVectorStore(@Qualifier("qdrantClient") QdrantClient qdrantClient,
                                         EmbeddingModel embeddingModel,
                                         RagProperties ragProperties) {
        RagProperties.QdrantProperties qdrant = ragProperties.getVectorStore().getQdrant();
        return QdrantVectorStore.builder(qdrantClient, embeddingModel)
                .collectionName(qdrant.getCollectionName())
                .initializeSchema(false)
                .build();
    }
}
