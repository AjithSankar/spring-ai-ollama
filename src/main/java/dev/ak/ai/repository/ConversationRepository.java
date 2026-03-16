package dev.ak.ai.repository;

import dev.ak.ai.entity.Conversation;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class ConversationRepository {

    private final JdbcTemplate jdbcTemplate;

    public ConversationRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public UUID createConversation(String title) {

        UUID id = UUID.randomUUID();

        jdbcTemplate.update("""
            INSERT INTO conversations(id, title, title_generated)
            VALUES (?, ?, FALSE)
        """, id, title);

        return id;
    }

    public Conversation findById(UUID id) {

        return jdbcTemplate.queryForObject("""
            SELECT id, title, created_at, title_generated
            FROM conversations
            WHERE id = ?
        """, (rs, rowNum) ->
            new Conversation(
                UUID.fromString(rs.getString("id")),
                rs.getString("title"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getBoolean("title_generated")
            ), id);
    }

    public List<Conversation> findAll() {

        return jdbcTemplate.query("""
            SELECT id, title, created_at, title_generated
            FROM conversations
            ORDER BY created_at DESC
        """, (rs, rowNum) ->
            new Conversation(
                UUID.fromString(rs.getString("id")),
                rs.getString("title"),
                rs.getTimestamp("created_at").toLocalDateTime(),
                rs.getBoolean("title_generated")
            )
        );
    }

    public void updateTitle(UUID id, String title){

        jdbcTemplate.update("""
        UPDATE conversations
        SET title = ?, title_generated = TRUE
        WHERE id = ?
    """, title, id);

    }

    public void deleteConversation(UUID id){

        jdbcTemplate.update("""
        DELETE FROM SPRING_AI_CHAT_MEMORY
        WHERE conversation_id = ?
    """, id.toString());

        jdbcTemplate.update("""
        DELETE FROM conversations
        WHERE id = ?
    """, id);
    }
}