package dev.ak.ai.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class QueryRewriteService {

    private final ChatClient chatClient;

    public QueryRewriteService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String rewrite(String question) {

        return chatClient.prompt()
                .system("""
                        You improve search queries for document retrieval.

                        Rewrite the user's question to be clearer and more detailed
                        so a search engine can retrieve the most relevant documents.

                        Return ONLY the rewritten query.
                        """)
                .user(question)
                .call()
                .content();
    }
}