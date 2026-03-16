package dev.ak.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RerankService {

    private final ChatClient chatClient;
    @Autowired
    private AiDebugLogger debugLogger;

    public RerankService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public List<Document> rerank(String question, List<Document> docs) {

        Map<Document, Double> scores = new HashMap<>();

        for (Document doc : docs) {

            double score = score(question, doc.getText());

            scores.put(doc, score);

            System.out.println("RERANK SCORE: " +
                    doc.getMetadata().get("source") +
                    " -> " + score);
        }

        return docs.stream()
                .sorted((d1, d2) ->
                        Double.compare(scores.get(d2), scores.get(d1)))
                .limit(3)
                .toList();
    }

    private double score(String question, String text) {

        return Mono.fromCallable(() ->
                        chatClient.prompt()
                                .system("Rate how relevant this document is to the question from 0 to 10. Only return the number.")
                                .user("""
                            Question:
                            %s
                            
                            Document:
                            %s
                            """.formatted(question, text))
                                .call()
                                .content()
                )
                .subscribeOn(Schedulers.boundedElastic())
                .map(response -> {
                    try {
                        return Double.parseDouble(response.trim());
                    } catch (Exception e) {
                        return 0.0;
                    }
                })
                .blockOptional()
                .orElse(0.0);
    }
}