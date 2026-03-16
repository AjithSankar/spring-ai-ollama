package dev.ak.ai.service;

import dev.ak.ai.dto.RagResponse;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public RagService(VectorStore vectorStore, ChatClient.Builder builder) {
        this.vectorStore = vectorStore;
        this.chatClient = builder.build();
    }

    public Mono<RagResponse> ask(String question) {

        return Mono.fromCallable(() -> vectorStore.similaritySearch(question))
                .subscribeOn(Schedulers.boundedElastic())
                .map(docs -> {

                    String context = docs.stream()
                            .map(Document::getText)
                            .collect(Collectors.joining("\n"));

                    List<String> sources = docs.stream()
                            .map(d -> (String) d.getMetadata().get("source"))
                            .filter(Objects::nonNull)
                            .distinct()
                            .toList();

                    System.out.println("Sources: " + sources);

                    Flux<String> answerStream = chatClient.prompt()
                            .system("""
                        You are an AI assistant that answers questions using the provided context.

                        Always prefer calling available tools if they can answer the question.
                        If the answer is found in the context, use it.
                        If the question is unrelated, say you don't know.
                        """)
                            .user("""
                        Context:
                        %s

                        Question:
                        %s
                        """.formatted(context, question))
                            .stream()
                            .content();
                    return new RagResponse(answerStream, sources);
                });
    }
}