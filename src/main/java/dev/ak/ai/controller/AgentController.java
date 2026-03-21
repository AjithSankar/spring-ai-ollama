package dev.ak.ai.controller;

import dev.ak.ai.service.AgentMcpService;
import dev.ak.ai.service.AgentOrchestrator;
import dev.ak.ai.service.AgentService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/agent")
public class AgentController {

    private final AgentOrchestrator agentOrchestrator;

    public AgentController(AgentOrchestrator agentOrchestrator) {
        this.agentOrchestrator = agentOrchestrator;
    }

    @GetMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> ask(@RequestParam String question, @RequestParam String conversationId) {

        return agentOrchestrator.ask(question, conversationId);
    }
}