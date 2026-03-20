package dev.ak.ai.config;

import dev.ak.ai.service.AiDebugLogger;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.annotation.Autowired;

public class LoggingToolCallbackDecorator implements ToolCallback {

    private final ToolCallback delegate;
    private final AiDebugLogger debugLogger;

    @Autowired
    public LoggingToolCallbackDecorator(ToolCallback delegate, AiDebugLogger debugLogger) {
        this.delegate = delegate;
        this.debugLogger = debugLogger;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        // Return the schema fetched from the MCP server untouched
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        String toolName = delegate.getToolDefinition().name();
        debugLogger.log("MCP TOOL EXECUTING: " + toolName, "JSON Input: " + toolInput);

        long startTime = System.currentTimeMillis();
        try {
            // Forward the execution to the actual MCP server on port 8081
            String result = delegate.call(toolInput);
            
            long duration = System.currentTimeMillis() - startTime;
            String displayResult = result.length() > 500 ? result.substring(0, 500) + "... [TRUNCATED]" : result;
            
            debugLogger.log("MCP TOOL SUCCESS: " + toolName + " (" + duration + "ms)", displayResult);
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            debugLogger.log("MCP TOOL FAILED: " + toolName + " (" + duration + "ms)", e.getMessage());
            throw e;
        }
    }
}