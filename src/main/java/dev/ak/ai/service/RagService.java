package dev.ak.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagService {

    private final VectorStore vectorStore;
    private final ChatClient chatClient;

    public RagService(VectorStore vectorStore, ChatClient.Builder builder) {
        this.vectorStore = vectorStore;
        this.chatClient = builder.build();
    }

    public Flux<String> ask(String question) {

        return Mono.fromCallable(() -> vectorStore.similaritySearch(question))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMapMany(docs -> {

                    String context = docs.stream()
                            .map(Document::getText)
                            .collect(Collectors.joining("\n"));

                    String prompt = """
                        Use the following context to answer the question.

                        Context:
                        %s

                        Question:
                        %s
                        """.formatted(context, question);

                    return chatClient.prompt()
                            .user(prompt)
                            .stream()
                            .content();
                });
    }
}