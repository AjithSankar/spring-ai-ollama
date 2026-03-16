package dev.ak.ai.tools;

import dev.ak.ai.service.AiDebugLogger;
import dev.ak.ai.service.HybridSearchService;
import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RagTool {

    private final HybridSearchService hybridSearchService;
    @Autowired
    private AiDebugLogger debugLogger;

    public RagTool(HybridSearchService hybridSearchService) {
        this.hybridSearchService = hybridSearchService;
    }

    @Tool(description = """
            Search the uploaded knowledge base and documents
            Use this tool when the question may be answered from uploaded files
            or the vector database.""")
    public String search(String question) {

        //List<Document> docs = vectorStore.similaritySearch(question); // Vector similarity search
        debugLogger.log("RAG TOOL INVOKED", question);
        List<Document> docs = hybridSearchService.search(question); // Hybrid search (vector + keyword)
        debugLogger.log("DOCUMENTS RETRIEVED", docs.size());
        System.out.println("Documents used for answer:");

        docs.forEach(d ->
                System.out.println(
                        d.getMetadata().get("source") +
                                " -> " +
                                d.getText().substring(0,60)
                )
        );

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n"));

        List<String> sources = docs.stream()
                .map(d -> (String) d.getMetadata().get("source"))
                .distinct()
                .toList();

        System.out.println("Sources: " + sources);

        debugLogger.log("FINAL CONTEXT SENT TO LLM", context);

        return """
            Retrieved context:
            %s
            
            Sources:
            %s
            """.formatted(context, String.join(", ", sources));
    }
}