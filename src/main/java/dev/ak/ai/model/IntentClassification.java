package dev.ak.ai.model;

public record IntentClassification(
        String reasoning, // Chain of Thought: Why did I choose this intent?
        UserIntent intent // The final decision
) {}