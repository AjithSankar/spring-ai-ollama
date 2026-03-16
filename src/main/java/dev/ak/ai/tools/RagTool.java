package dev.ak.ai.tools;

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

    public RagTool(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Tool(description = "Search uploaded documents")
    public String search(String question) {

        List<Document> docs = vectorStore.similaritySearch(question);

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