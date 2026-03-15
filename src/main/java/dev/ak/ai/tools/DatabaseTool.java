package dev.ak.ai.tools;

import org.jspecify.annotations.Nullable;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DatabaseTool {

    private final JdbcTemplate jdbcTemplate;

    public DatabaseTool(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Tool(description = "Count the number of stored documents in the vector store")
    public int countDocuments() {
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM vector_store", Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return -1; // Indicate an error
        }
    }

    @Tool(description = "Get the top N documents from the vector store")
    public String getTopDocuments(int limit) {
        try {
            List<Map<String, @Nullable Object>> result = jdbcTemplate.queryForList("SELECT * FROM vector_store LIMIT 5", limit);
            return result.toString();
        } catch (Exception e) {
            return "Error fetching documents: " + e.getMessage();
        }
    }

    @Tool(description = "Run a SQL query on the application database")
    public String runQuery(String query) {
        try {
            List<Map<String, @Nullable Object>> result = jdbcTemplate.queryForList(query);
            return result.toString();
        } catch (Exception e) {
            return "Error executing query: " + e.getMessage();
        }
    }
}
