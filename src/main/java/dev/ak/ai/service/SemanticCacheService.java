package dev.ak.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;

@Service
public class SemanticCacheService {

    private final VectorStore vectorStore;

    public SemanticCacheService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    // 1. Check if we already answered this question
    public Mono<String> checkCache(String question) {
        return Mono.fromCallable(() -> {
            // Search with a very high similarity threshold (e.g., 95% match)
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query(question)
                            .topK(1)
                            .similarityThreshold(0.95)
                            // Optional: Filter by a metadata tag so we don't confuse cache docs with RAG docs
                            .filterExpression("type == 'CACHE'") 
                            .build()
            );

            if (!results.isEmpty() && results.get(0).getMetadata().containsKey("cached_answer")) {
                return (String) results.get(0).getMetadata().get("cached_answer");
            }
            return null; // Cache miss
        }).subscribeOn(Schedulers.boundedElastic());
    }

    // 2. Save new answers to the cache asynchronously
    public void saveToCache(String question, String generatedAnswer) {
        Mono.fromRunnable(() -> {
            Document cacheDoc = new Document(question, Map.of(
                    "type", "CACHE",
                    "cached_answer", generatedAnswer
            ));
            vectorStore.add(List.of(cacheDoc));
        }).subscribeOn(Schedulers.boundedElastic()).subscribe();
    }
}