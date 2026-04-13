package com.example.agent.rag.enums;

/**
 * 文档入库生命周期状态。
 */
public enum RagDocumentStatus {
    /** 已创建主记录，但还没开始真正处理。 */
    PENDING,
    /** 正在分块、向量化和写入分块表。 */
    PROCESSING,
    /** 入库成功，可参与检索。 */
    READY,
    /** 入库失败，通常会在 metadata 中记录错误信息。 */
    FAILED
}
