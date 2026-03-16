package dev.ak.ai.tools;

import dev.ak.ai.service.HybridSearchService;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class RagTool {

    private final VectorStore vectorStore;
    private final HybridSearchService hybridSearchService;

    public RagTool(VectorStore vectorStore, HybridSearchService hybridSearchService) {
        this.vectorStore = vectorStore;
        this.hybridSearchService = hybridSearchService;
    }

    @Tool(description = "Search uploaded documents")
    public String search(String question) {

        //List<Document> docs = vectorStore.similaritySearch(question); // Vector similarity search

        List<Document> docs = hybridSearchService.search(question); // Hybrid search (vector + keyword)

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        List<String> sources = docs.stream()
                .map(d -> (String) d.getMetadata().get("source"))
                .distinct()
                .toList();

        System.out.println("Sources: " + sources);

        return """
            Retrieved context:
            %s
            
            Sources:
            %s
            """.formatted(context, String.join(", ", sources));
    }
}