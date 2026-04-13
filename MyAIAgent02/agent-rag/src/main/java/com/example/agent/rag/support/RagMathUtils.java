package com.example.agent.rag.support;

import java.util.ArrayList;
import java.util.List;

/**
 * RAG 相关的基础数学工具。
 */
public final class RagMathUtils {

    private RagMathUtils() {
    }

    /**
     * Spring AI embedding 返回 float[]，这里转成便于 JSON 落库的 List<Double>。
     */
    public static List<Double> toDoubleList(float[] values) {
        List<Double> result = new ArrayList<>();
        if (values == null) {
            return result;
        }
        for (float value : values) {
            result.add((double) value);
        }
        return result;
    }

    /**
     * 计算两个向量的余弦相似度。
     *
     * <p>返回值越接近 1，代表语义越接近。
     */
    public static double cosineSimilarity(List<Double> left, List<Double> right) {
        if (left == null || right == null || left.isEmpty() || right.isEmpty()) {
            return 0d;
        }

        int size = Math.min(left.size(), right.size());
        double dot = 0d;
        double leftNorm = 0d;
        double rightNorm = 0d;
        for (int i = 0; i < size; i++) {
            double l = left.get(i);
            double r = right.get(i);
            dot += l * r;
            leftNorm += l * l;
            rightNorm += r * r;
        }

        if (leftNorm == 0d || rightNorm == 0d) {
            return 0d;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
