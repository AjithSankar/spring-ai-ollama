package dev.ak.ai.service;

import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class PromptGuardService {

    private static final List<String> BLOCKED_PHRASES = List.of(
            "ignore all previous",
            "system prompt",
            "drop table",
            "you are no longer",
            "bypass instructions"
    );

    public Mono<String> validate(String prompt) {
        return Mono.fromCallable(() -> {
            String normalizedPrompt = prompt.toLowerCase().trim();
            for (String phrase : BLOCKED_PHRASES) {
                if (normalizedPrompt.contains(phrase)) {
                    throw new SecurityViolationException("Prompt blocked by security guardrails.");
                }
            }
            return prompt;
        });
    }

    public static class SecurityViolationException extends RuntimeException {
        public SecurityViolationException(String message) {
            super(message);
        }
    }
}