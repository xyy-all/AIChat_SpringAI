package com.example.agent.rag.support;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TextChunker {

    private final int chunkSize;

    private final int chunkOverlap;

    public TextChunker(int chunkSize, int chunkOverlap) {
        this.chunkSize = Math.max(1, chunkSize);
        this.chunkOverlap = Math.max(0, Math.min(chunkOverlap, this.chunkSize / 2));
    }

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
