package dev.ak.ai.dto;

import reactor.core.publisher.Flux;

import java.util.List;

public record RagResponse(
        Flux<String> answer,
        List<String> sources
) {}