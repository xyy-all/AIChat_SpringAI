package com.example.agent.rag.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 文本分块器。
 *
 * <p>策略是“尽量按自然边界切分”：
 * 1. 先按空行拆段
 * 2. 能拼进当前 chunk 的段落尽量拼进去
 * 3. 单段过长时，再按句号/换行/空白等边界回退切分
 * 4. 通过 overlap 保留少量上下文，降低跨 chunk 信息断裂
 */
public class TextChunker {

    private final int chunkSize;

    private final int chunkOverlap;

    public TextChunker(int chunkSize, int chunkOverlap) {
        this.chunkSize = Math.max(1, chunkSize);
        this.chunkOverlap = Math.max(0, Math.min(chunkOverlap, this.chunkSize / 2));
    }

    /**
     * 对外暴露的分块入口。
     */
    public List<String> split(String text) {
        String normalized = normalize(text);
        if (normalized.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> paragraphs = splitParagraphs(normalized);
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (paragraph.length() >= chunkSize) {
                flush(chunks, current);
                chunks.addAll(splitLongParagraph(paragraph));
                continue;
            }

            if (current.length() == 0) {
                current.append(paragraph);
                continue;
            }

            if (current.length() + 2 + paragraph.length() <= chunkSize) {
                current.append("\n\n").append(paragraph);
            } else {
                flush(chunks, current);
                current.append(paragraph);
            }
        }

        flush(chunks, current);
        return chunks;
    }

    /**
     * 双换行视为段落边界，优先保留原始段落结构。
     */
    private List<String> splitParagraphs(String text) {
        List<String> result = new ArrayList<>();
        for (String paragraph : text.split("\\n\\s*\\n")) {
            String value = paragraph.trim();
            if (!value.isEmpty()) {
                result.add(value);
            }
        }
        return result.isEmpty() ? List.of(text) : result;
    }

    /**
     * 对超长段落做二次切分，并通过 overlap 回带一小段上下文。
     */
    private List<String> splitLongParagraph(String paragraph) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < paragraph.length()) {
            int end = Math.min(start + chunkSize, paragraph.length());
            int splitPoint = findSplitPoint(paragraph, start, end);
            String chunk = paragraph.substring(start, splitPoint).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (splitPoint >= paragraph.length()) {
                break;
            }
            start = Math.max(splitPoint - chunkOverlap, start + 1);
        }
        return chunks;
    }

    /**
     * 在允许范围内尽量往前找一个更自然的切点，避免粗暴截断。
     */
    private int findSplitPoint(String text, int start, int maxEnd) {
        if (maxEnd >= text.length()) {
            return text.length();
        }
        for (int i = maxEnd; i > start + (chunkSize / 2); i--) {
            char current = text.charAt(i - 1);
            if (current == '\n' || current == '。' || current == '！' || current == '？'
                    || current == '.' || current == '!' || current == '?' || current == ';'
                    || current == '；' || Character.isWhitespace(current)) {
                return i;
            }
        }
        return maxEnd;
    }

    private void flush(List<String> chunks, StringBuilder current) {
        if (current.length() == 0) {
            return;
        }
        chunks.add(current.toString().trim());
        current.setLength(0);
    }

    /**
     * 统一换行和空白格式，减少 embedding 前的噪声。
     */
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t\\x0B\\f]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}
