package dev.ak.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class HybridSearchService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;
    private final RerankService rerankService;
    @Autowired
    private AiDebugLogger debugLogger;

    public HybridSearchService(JdbcTemplate jdbcTemplate,
                               EmbeddingModel embeddingModel, RerankService rerankService) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
        this.rerankService = rerankService;
    }

    public List<Document> search(String query) {
        debugLogger.log("HYBRID SEARCH QUERY", query);
        float[] embedding = embeddingModel.embed(query);

        List<Document> docs = jdbcTemplate.query("""
                SELECT content, metadata
                FROM vector_store
                ORDER BY embedding <=> ?::vector
                LIMIT 3
                """,
                (rs, rowNum) -> {

                    String content = rs.getString("content");

                    Map<String,Object> metadata =
                            new ObjectMapper().readValue(
                                    rs.getString("metadata"),
                                    Map.class
                            );

                    return new Document(content, metadata);
                },
                embedding
        );

        docs.forEach(doc ->
                debugLogger.log("RETRIEVED DOC",
                        doc.getMetadata().get("source") + " -> " +
                                doc.getText().substring(0,100)
                )
        );

        return rerankService.rerank(query, docs);
    }
}