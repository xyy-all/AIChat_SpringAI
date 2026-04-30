package com.example.agent.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 模块的可调参数。
 *
 * <p>除了分块和召回参数外，这里也集中管理向量库后端的选择和 Qdrant 连接配置。
 */
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    private int chunkSize = 700;
    private int chunkOverlap = 100;
    private int defaultTopK = 3;
    private double similarityThreshold = 0.35d;
    private int maxCandidates = 2000;
    private VectorStoreProperties vectorStore = new VectorStoreProperties();

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public int getDefaultTopK() {
        return defaultTopK;
    }

    public void setDefaultTopK(int defaultTopK) {
        this.defaultTopK = defaultTopK;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public int getMaxCandidates() {
        return maxCandidates;
    }

    public void setMaxCandidates(int maxCandidates) {
        this.maxCandidates = maxCandidates;
    }

    public VectorStoreProperties getVectorStore() {
        return vectorStore;
    }

    public void setVectorStore(VectorStoreProperties vectorStore) {
        this.vectorStore = vectorStore;
    }

    public static class VectorStoreProperties {
        private Provider provider = Provider.DATABASE;
        private boolean localFallbackEnabled = true;
        private QdrantProperties qdrant = new QdrantProperties();

        public Provider getProvider() {
            return provider;
        }

        public void setProvider(Provider provider) {
            this.provider = provider;
        }

        public boolean isLocalFallbackEnabled() {
            return localFallbackEnabled;
        }

        public void setLocalFallbackEnabled(boolean localFallbackEnabled) {
            this.localFallbackEnabled = localFallbackEnabled;
        }

        public QdrantProperties getQdrant() {
            return qdrant;
        }

        public void setQdrant(QdrantProperties qdrant) {
            this.qdrant = qdrant;
        }
    }

    public static class QdrantProperties {
        private String host = "localhost";
        private int grpcPort = 6334;
        private boolean useTls = false;
        private String apiKey;
        private String collectionName = "ai_document_chunks";
        private boolean initializeSchema = false;
        private int vectorSize;
        private int timeoutSeconds = 10;

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getGrpcPort() {
            return grpcPort;
        }

        public void setGrpcPort(int grpcPort) {
            this.grpcPort = grpcPort;
        }

        public boolean isUseTls() {
            return useTls;
        }

        public void setUseTls(boolean useTls) {
            this.useTls = useTls;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getCollectionName() {
            return collectionName;
        }

        public void setCollectionName(String collectionName) {
            this.collectionName = collectionName;
        }

        public boolean isInitializeSchema() {
            return initializeSchema;
        }

        public void setInitializeSchema(boolean initializeSchema) {
            this.initializeSchema = initializeSchema;
        }

        public int getVectorSize() {
            return vectorSize;
        }

        public void setVectorSize(int vectorSize) {
            this.vectorSize = vectorSize;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public enum Provider {
        DATABASE,
        QDRANT
    }
}
