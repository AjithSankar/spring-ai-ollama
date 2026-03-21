package dev.ak.ai.service;

import dev.ak.ai.config.LoggingToolCallbackDecorator;
import dev.ak.ai.entity.Conversation;
import dev.ak.ai.model.IntentClassification;
import dev.ak.ai.repository.ConversationRepository;
import dev.ak.ai.tools.RagTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.Arrays;
import java.util.UUID;

@Service
@Slf4j
public class AgentOrchestrator {

    private static final String SHARED_MEMORY_RULES = """
            
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
            """;

    // 1. Specialized Agents
    private final ChatClient triageRouter;
    private final ChatClient documentAgent;
    private final ChatClient databaseAgent;
    private final ChatClient generalAgent;

    private final ConversationRepository conversationRepository;
    private final TitleService titleService;
    private final AiDebugLogger debugLogger;

    // Dedicated Thread Pool to protect the WebFlux Netty threads
    private final Scheduler aiScheduler = Schedulers.newBoundedElastic(
            50, 10000, "ai-executor-pool"
    );

    public AgentOrchestrator(ChatClient.Builder builder,
                             RagTool localRagTool,
                             ToolCallbackProvider mcpToolProvider,
                             ChatMemory chatMemory,
                             ConversationRepository conversationRepository,
                             TitleService titleService,
                             AiDebugLogger debugLogger) {

        this.conversationRepository = conversationRepository;
        this.titleService = titleService;
        this.debugLogger = debugLogger;

        ToolCallback[] allMcpTools = new ToolCallback[0];

        // Eagerly fetch and wrap all MCP tools at startup
        try {
            if (mcpToolProvider != null && mcpToolProvider.getToolCallbacks() != null) {
                ToolCallback[] rawMcpTools = mcpToolProvider.getToolCallbacks();
                allMcpTools = Arrays.stream(rawMcpTools)
                        .map(tool -> new LoggingToolCallbackDecorator(tool, debugLogger))
                        .toArray(ToolCallback[]::new);

                log.info("Successfully loaded and wrapped MCP tools: {}",
                        Arrays.toString(Arrays.stream(allMcpTools).map(t -> t.getToolDefinition().name()).toArray()));
            }
        } catch (Exception e) {
            log.warn("Failed to connect to MCP server at startup. Running with local tools only.", e);
        }

        // --- OPTIONAL BUT RECOMMENDED: Tool Siloing ---
        // For maximum safety, you can filter the allMcpTools array by name
        // so the DB agent doesn't get the RAG tools, and vice versa.
        // Example:
         ToolCallback[] mcpDbTools = Arrays.stream(allMcpTools).filter(t -> t.getToolDefinition().name().contains("database")).toArray(ToolCallback[]::new);

        // --- SPAWN THE AGENTS ---

        // 1. Router: Fast, NO tools, strictly outputs JSON
        this.triageRouter = builder.build();

        // 2. Document Agent: Gets local RagTool + all MCP tools (or filtered MCP RAG tools)
        this.documentAgent = builder
                .defaultSystem("You are a Document Specialist. Answer questions based ONLY on the provided RAG tools. Be precise." + SHARED_MEMORY_RULES)
                .defaultTools(localRagTool)
                .defaultToolCallbacks(allMcpTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        // 3. Database Agent: Gets ONLY the MCP tools (since DB and HTTP moved to the server)
        this.databaseAgent = builder
                .defaultSystem("You are a Data Engineer. You query databases and APIs to answer user questions." + SHARED_MEMORY_RULES)
                .defaultToolCallbacks(mcpDbTools)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();

        // 4. General Agent: Conversational, NO tools
        this.generalAgent = builder
                .defaultSystem("You are a helpful assistant. Have a polite conversation. You do not have access to external data." + SHARED_MEMORY_RULES)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public Flux<String> ask(String question, String conversationId) {
        debugLogger.log("USER QUESTION", question);
        debugLogger.log("CONVERSATION ID", conversationId);

        // We wrap the synchronous database lookup AND the synchronous router call
        // inside Mono.fromCallable. This guarantees both run on our aiScheduler
        // and safely keeps the WebFlux event loop unblocked.
        return Mono.fromCallable(() -> {

                    // 1. Handle Conversation Logic (Blocking JPA/JDBC call)
                    Conversation convo = conversationRepository.findById(UUID.fromString(conversationId));
                    if (!convo.titleGenerated()) {
                        titleService.generateTitle(question)
                                .subscribe(title -> conversationRepository.updateTitle(UUID.fromString(conversationId), title));
                    }

                    // 2. Execute Triage Router (Blocking LLM call)
                    return triageRouter.prompt()
                            .system("You are a traffic router. Analyze the user's prompt and classify it. " +
                                    "If they ask about policies, documents, or texts, choose DOCUMENT_SEARCH. " +
                                    "If they ask for metrics, user data, or system data, choose DATABASE_QUERY. " +
                                    "If it is a greeting or general knowledge, choose GENERAL_CHAT.")
                            .user(question)
                            .call()
                            .entity(IntentClassification.class);

                })
                .subscribeOn(this.aiScheduler)
                .flatMapMany(classification -> {

                    debugLogger.log("ROUTER DECISION", "Intent: " + classification.intent() + " | Reason: " + classification.reasoning());

                    // 3. Route to the specialized reactive stream
                    return switch (classification.intent()) {
                        case DOCUMENT_SEARCH -> documentAgent.prompt()
                                .user(question)
                                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                                .stream().content();

                        case DATABASE_QUERY -> databaseAgent.prompt()
                                .user(question)
                                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                                .stream().content();

                        case GENERAL_CHAT -> generalAgent.prompt()
                                .user(question)
                                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                                .stream().content();
                    };
                });
    }
}