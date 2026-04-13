package com.example.agent.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * RAG 模块的可调参数。
 *
 * <p>这里集中管理分块大小、召回数量和相似度阈值，
 * 避免这些数字散落在业务代码里。
 */
@ConfigurationProperties(prefix = "app.rag")
public class RagProperties {

    /** 单个 chunk 的目标长度。 */
    private int chunkSize = 700;

    /** 相邻 chunk 之间保留的重叠字符数。 */
    private int chunkOverlap = 100;

    /** 未显式传入 topK 时使用的默认召回数量。 */
    private int defaultTopK = 3;

    /** 低于该阈值的候选 chunk 会被过滤掉。 */
    private double similarityThreshold = 0.35d;

    /** 从数据库里最多拉多少个候选 chunk 参与排序。 */
    private int maxCandidates = 2000;

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
}
