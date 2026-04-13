package com.example.agent.rag.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextChunkerTest {

    @Test
    void splitShouldRespectParagraphBoundariesWhenPossible() {
        TextChunker chunker = new TextChunker(80, 10);
        String text = "第一段内容，包含一些信息，并且补充一段较长的上下文，确保单段已经比较接近上限。"
                + "\n\n第二段内容，继续补充背景，也保持足够长度，让组合后超过分块大小。"
                + "\n\n第三段内容，总结并补充额外说明。";

        List<String> chunks = chunker.split(text);

        assertEquals(2, chunks.size());
        assertTrue(chunks.get(0).contains("第一段"));
        assertTrue(chunks.get(1).contains("第三段"));
    }

    @Test
    void splitShouldBreakLongParagraphs() {
        TextChunker chunker = new TextChunker(120, 20);
        String text = "这是一个很长的段落。".repeat(40);

        List<String> chunks = chunker.split(text);

        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() > 1);
    }

    @Test
    void splitShouldReturnEmptyForBlankText() {
        TextChunker chunker = new TextChunker(120, 20);

        List<String> chunks = chunker.split("   \n\n   ");

        assertTrue(chunks.isEmpty());
    }
}
