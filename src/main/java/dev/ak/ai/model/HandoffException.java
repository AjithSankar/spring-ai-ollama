package dev.ak.ai.model;

public class HandoffException extends RuntimeException {
    public HandoffException() {
        super("Agent requested handoff to fallback system.");
    }
}