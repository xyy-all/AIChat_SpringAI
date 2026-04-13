package com.example.aiagent.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final VectorStore vectorStore;

    public RagService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void addDocument(String text, String documentId) {
        Document document = new Document(text);
        document.getMetadata().put("documentId", documentId);
        vectorStore.add(List.of(document));
    }

    public List<String> searchSimilar(String query, int k) {
        SearchRequest searchRequest = SearchRequest
                .builder()
                .query(query)
                .topK(k)
                .build();
        List<Document> documents = vectorStore.similaritySearch(searchRequest);

        return documents.stream()
                .map(Document::getText)
                .collect(Collectors.toList());
    }
}