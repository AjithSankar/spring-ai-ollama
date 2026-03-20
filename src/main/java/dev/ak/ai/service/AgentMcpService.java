package dev.ak.ai.service;

import dev.ak.ai.config.LoggingToolCallbackDecorator;
import dev.ak.ai.entity.Conversation;
import dev.ak.ai.repository.ConversationRepository;
import dev.ak.ai.tools.DatabaseTool;
import dev.ak.ai.tools.HttpClientTool;
import dev.ak.ai.tools.RagTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class AgentMcpService {

    private final ChatClient chatClient;
    private final ConversationRepository conversationRepository;
    private final TitleService titleService;

    // 1. Dedicated AI Thread Pool (Bulkhead Pattern)
    // Adjust thread counts based on your production load
    private final Scheduler aiScheduler = Schedulers.newBoundedElastic(
            50, 10000, "ai-executor-pool"
    );

    private AiDebugLogger debugLogger;

    public AgentMcpService(ChatClient.Builder builder,
                           DatabaseTool databaseTool,
                            HttpClientTool httpClientTool,
                           RagTool ragTool,
                           ToolCallbackProvider mcpToolProvider, // <--- Inject the MCP Tool Provider
                           ChatMemory chatMemory,
                           ConversationRepository conversationRepository,
                           TitleService titleService, AiDebugLogger debugLogger) {

        this.conversationRepository = conversationRepository;
        this.titleService = titleService;
        this.debugLogger = debugLogger;

        Object[] localTools = { ragTool, databaseTool, httpClientTool }; // You can add databaseTool, httpClientTool here too

        // Array to hold our cached MCP tools
        ToolCallback[] cachedMcpTools = new ToolCallback[0];

        // 2. Eager Initialization & Caching
        // We fetch the tools NOW during application startup, not during the user request.
        try {
            if (mcpToolProvider != null && mcpToolProvider.getToolCallbacks() != null) {
                // Fetch the raw tools from port 8081
                ToolCallback[] rawMcpTools = mcpToolProvider.getToolCallbacks();

                // Wrap every MCP tool in our Logging Decorator
                cachedMcpTools = Arrays.stream(rawMcpTools)
                        .map(tool -> new LoggingToolCallbackDecorator(tool, debugLogger))
                        .toArray(ToolCallback[]::new);

                log.info("Successfully loaded, wrapped, and cached MCP tools from remote server.");
            }
        } catch (Exception e) {
            // 3. Graceful Degradation
            // If port 8081 is down at startup, we log it but still start the chat app
            // so users can at least use the Database and HTTP tools.
            log.warn("Failed to connect to MCP server at startup. Running with local tools only.", e);
        }

        assert mcpToolProvider != null;
        this.chatClient = builder
                // 1. Register ONLY local beans annotated with @Tool here
                .defaultTools(localTools)
                // 2. Register ONLY the pre-built MCP callbacks here
                .defaultToolCallbacks(cachedMcpTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public Flux<String> ask(String question, String conversationId) {

        debugLogger.log("USER QUESTION", question);
        debugLogger.log("CONVERSATION ID", conversationId);

        Conversation convo = conversationRepository.findById(UUID.fromString(conversationId));

        if (!convo.titleGenerated()) {

            titleService.generateTitle(question)
                    .subscribe(title ->
                            conversationRepository.updateTitle(
                                    UUID.fromString(conversationId),
                                    title
                            )
                    );
        }

        return Flux.defer( () ->
                chatClient.prompt()
                            .system("""
                                    You are a helpful AI assistant.
                                    
                                    If the user asks about information that might exist in uploaded documents,
                                    use the mcp registered tool to retrieve the answer.
                                    
                                    Only call tools if they are required to retrieve external data.
                                    
                                    You will receive the conversation history between you and the user.
                                    Treat information previously provided by the user as factual context.
                                    
                                    If the user asks a question that can be answered using the conversation
                                    history, you must use that information.
                                    
                                    The conversation history contains facts shared by the user.
                                    If the user previously stated a fact about themselves,
                                    you must use that exact information when answering.
                                    
                                    Do not claim you do not know something if it was previously stated
                                    in the conversation.
                                    
                                    If the answer exists in the conversation history, retrieve it and answer
                                    the question directly.
                                    
                                    Only call tools if they are required to retrieve external data.
                                    
                                    Do NOT call tools unnecessarily.
                                    """)
                            .user(question)
                            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                            .stream()
                            .content()
                // Shift the execution of this stream off the Netty event loop
                // and onto a thread pool that safely allows blocking tool calls.
        ).subscribeOn(this.aiScheduler);
    }
}