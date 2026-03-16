package dev.ak.ai.controller;

import dev.ak.ai.entity.Conversation;
import dev.ak.ai.repository.ConversationRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/conversations")
public class ConversationController {

    private final ConversationRepository repository;

    public ConversationController(ConversationRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public UUID createConversation(@RequestParam String title) {
        return repository.createConversation(title);
    }

    @GetMapping
    public List<Conversation> listConversations() {
        return repository.findAll();
    }

    @DeleteMapping("/{id}")
    public void deleteConversation(@PathVariable UUID id){

        repository.deleteConversation(id);

    }
}