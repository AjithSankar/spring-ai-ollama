package dev.ak.ai.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final JdbcTemplate jdbcTemplate;

    public MessageController(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/{conversationId}")
    public List<Map<String,Object>> getMessages(@PathVariable String conversationId){

        return jdbcTemplate.queryForList("""
            SELECT type, content
            FROM SPRING_AI_CHAT_MEMORY
            WHERE conversation_id = ?
            ORDER BY "timestamp"
        """, conversationId);

    }
}