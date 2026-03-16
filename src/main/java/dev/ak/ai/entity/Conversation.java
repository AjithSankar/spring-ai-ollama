package dev.ak.ai.entity;

import java.time.LocalDateTime;
import java.util.UUID;

public record Conversation(
        UUID id,
        String title,
        LocalDateTime createdAt,
        boolean titleGenerated
) {}