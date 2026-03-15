package dev.ak.ai.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

@Component
public class SimpleTool {

    @Tool(description = "Return a greeting message")
    public String hello() {
        return "Hello from tool";
    }
}