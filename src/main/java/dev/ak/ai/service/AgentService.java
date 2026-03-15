package dev.ak.ai.service;

import dev.ak.ai.tools.DatabaseTool;
import dev.ak.ai.tools.HttpClientTool;
import dev.ak.ai.tools.RagTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class AgentService {

    private final ChatClient chatClient;

    public AgentService(ChatClient.Builder builder, DatabaseTool databaseTool,
                        HttpClientTool httpClientTool, RagTool ragTool,
                        ChatMemory chatMemory) {
        this.chatClient = builder
                .defaultTools(databaseTool, httpClientTool, ragTool)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    public Flux<String> ask(String question, String conversationId) {

        return chatClient.prompt()
                .system("""
                You are a helpful AI assistant.
                
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
                .content();
    }
}
