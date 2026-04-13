package com.example.agent.rag.support;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RagMathUtilsTest {

    @Test
    void cosineSimilarityShouldReturnExpectedScore() {
        double score = RagMathUtils.cosineSimilarity(
                List.of(1d, 0d, 1d),
                List.of(1d, 0d, 1d));

        assertEquals(1d, score, 0.0001d);
    }

    @Test
    void toDoubleListShouldConvertEmbeddings() {
        List<Double> values = RagMathUtils.toDoubleList(new float[]{1.5f, 2.5f});

        assertEquals(2, values.size());
        assertEquals(1.5d, values.get(0), 0.0001d);
        assertEquals(2.5d, values.get(1), 0.0001d);
    }

    @Test
    void cosineSimilarityShouldHandleEmptyVectors() {
        double score = RagMathUtils.cosineSimilarity(List.of(), List.of(1d));

        assertTrue(score == 0d);
    }
}
