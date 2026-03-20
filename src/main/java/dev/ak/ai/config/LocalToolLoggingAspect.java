package dev.ak.ai.config;

import dev.ak.ai.service.AiDebugLogger;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Aspect
@Component
public class LocalToolLoggingAspect {

    private final AiDebugLogger debugLogger;

    @Autowired
    public LocalToolLoggingAspect(AiDebugLogger debugLogger) {
        this.debugLogger = debugLogger;
    }

    // Intercepts any method annotated with Spring AI's @Tool
    @Around("@annotation(org.springframework.ai.tool.annotation.Tool)")
    public Object logToolExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        String toolName = joinPoint.getSignature().getName();
        String inputs = Arrays.toString(joinPoint.getArgs());
        
        debugLogger.log("LOCAL TOOL EXECUTING: " + toolName, "Arguments: " + inputs);
        
        long startTime = System.currentTimeMillis();
        try {
            // Execute the actual tool
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            // Trim the result if it's massive (like a huge RAG context) so we don't blow up the logs
            String resultStr = String.valueOf(result);
            String displayResult = resultStr.length() > 500 ? resultStr.substring(0, 500) + "... [TRUNCATED]" : resultStr;
            
            debugLogger.log("LOCAL TOOL SUCCESS: " + toolName + " (" + duration + "ms)", displayResult);
            return result;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            debugLogger.log("LOCAL TOOL FAILED: " + toolName + " (" + duration + "ms)", e.getMessage());
            throw e; // Rethrow to let Ollama know the tool failed
        }
    }
}