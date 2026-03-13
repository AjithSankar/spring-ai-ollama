package dev.ak.ai.controller;

import dev.ak.ai.service.AiChatService;
import dev.ak.ai.service.RagService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/ai")
public class AiController {

    private final AiChatService aiChatService;
    private final RagService ragService;

    public AiController(AiChatService aiChatService, RagService ragService) {
        this.aiChatService = aiChatService;
        this.ragService = ragService;
    }

    @GetMapping("/ask")
    public String ask(@RequestParam String question) {
        return aiChatService.askAI(question);
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam String question) {
        return aiChatService.streamResponse(question);
    }

    @GetMapping(value="/rag", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> rag(@RequestParam String question) {
        return ragService.ask(question);
    }
}
