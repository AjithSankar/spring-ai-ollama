package dev.ak.ai.service;

import org.springframework.stereotype.Component;

@Component
public class AiDebugLogger {

    public void log(String stage, Object data) {
        System.out.println("\n==============================");
        System.out.println("AI DEBUG STAGE: " + stage);
        System.out.println(data);
        System.out.println("==============================\n");
    }
}