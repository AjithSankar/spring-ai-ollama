package dev.ak.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Service
public class AiChatService {

    private final ChatClient chatClient;

    public AiChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String askAI(String question) {

        return chatClient.prompt()
                .system("You are a senior backend engineer mentor.")
                .user(question)
                .call()
                .content();
    }

    public Flux<String> streamResponse(String question) {

        return chatClient.prompt()
                .user(question)
                .stream()
                .content()
                .bufferTimeout(50, Duration.ofMillis(500))
                .map(tokens -> String.join("", tokens));
    }
}
