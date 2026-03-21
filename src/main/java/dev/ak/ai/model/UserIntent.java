package dev.ak.ai.model;

public enum UserIntent {
    DOCUMENT_SEARCH,  // Needs to search uploaded PDFs or company docs
    DATABASE_QUERY,   // Needs to query the SQL database or use HTTP
    GENERAL_CHAT      // Standard conversational greeting or question (No tools needed)
}