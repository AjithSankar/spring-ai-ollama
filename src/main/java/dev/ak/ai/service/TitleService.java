package dev.ak.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
public class TitleService {

    private final ChatClient chatClient;

    public TitleService(ChatClient.Builder builder){

        this.chatClient = builder.build();
    }

    public Mono<String> generateTitle(String question) {

        return Mono.fromCallable(() ->
                chatClient.prompt()
                        .system("Generate a short title (max 5 words) for this conversation.")
                        .user(question)
                        .call()
                        .content()
        ).subscribeOn(Schedulers.boundedElastic());

    }
}