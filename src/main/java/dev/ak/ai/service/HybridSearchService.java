package dev.ak.ai.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class HybridSearchService {

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingModel embeddingModel;

    public HybridSearchService(JdbcTemplate jdbcTemplate,
                               EmbeddingModel embeddingModel) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingModel = embeddingModel;
    }

    public List<Document> search(String query) {

        float[] embedding = embeddingModel.embed(query);

        return jdbcTemplate.query("""
                SELECT content, metadata
                FROM vector_store
                ORDER BY
                    (embedding <=> ?::vector) ASC
                LIMIT 5
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
    }
}