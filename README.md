# 🚀 AI-Powered Chat Application (Spring Boot + MCP + RAG)

This project demonstrates a production-grade AI chat system built using **Spring Boot**, **Spring AI**, **MCP (Model Context Protocol)**, and **RAG (Retrieval-Augmented Generation)**.

The system evolves from a basic LLM chat into a **multi-tool AI agent with orchestration, observability, and external integrations**.

---

## 🧠 Core Capabilities

### ✅ 1. LLM Integration
- Integrated LLM using Spring AI `ChatClient`
- Streaming responses using `Flux<String>`
- System prompts for controlled behavior

---

### ✅ 2. Chat Memory
- Implemented conversation memory using:
    - `MessageChatMemoryAdvisor`
- Maintains context across conversations
- Supports conversation-based interactions

---

### ✅ 3. RAG (Retrieval-Augmented Generation)
- Built custom `RagTool`
- Enables querying:
    - Uploaded documents
    - Internal knowledge base
- Used vector-based retrieval (embeddings)

---

### ✅ 4. MCP (Model Context Protocol) Integration
- Integrated MCP client into chat application
- Connected to external MCP server
- Dynamically fetched tool definitions from MCP server

---

### ✅ 5. Tool Calling (Hybrid System)

#### Local Tools (`@Tool`)
- RAG Tool (document search)

#### MCP Tools (`ToolCallback`)
- Database Tool
- HTTP API Tool
- External Knowledge Tool (optional)

---

### ✅ 6. Tool Separation (Important Design)
- RAG → Internal documents only
- MCP → External systems (DB, APIs)
- Removed overlapping responsibilities (e.g., document search from MCP)

---

### ✅ 7. Database Tool (PostgreSQL)
- Designed structured DB tool (safe queries)
- Tables:
    - `invoice`
    - `vendor`
- Supported operations:
    - Fetch invoice details
    - List pending invoices

---

### ✅ 8. HTTP Tool
- External API integration via MCP
- Example use cases:
    - Weather API
    - Exchange rate API

---

### ✅ 9. Tool Logging & Observability

#### MCP Tools
- Implemented `LoggingToolCallback` (Decorator Pattern)

#### Local Tools
- Implemented Spring AOP (`@Aspect`) for logging

#### Features:
- Tool name
- Input arguments
- Output response
- Execution time

---

### ✅ 10. Conversation Tracking
- Added correlation using `conversationId`
- Improved debugging and traceability

---

### ✅ 11. Graceful Degradation
- MCP server failure does not break chat app
- Falls back to local tools

---

### ✅ 12. Reactive Execution Model
- Used Reactor (`Flux`)
- Offloaded blocking operations using:
    - `Schedulers.boundedElastic()`
- Prevented Netty thread blocking

---

### ✅ 13. Tool Routing Layer
- Introduced deterministic routing logic
- Reduced dependency on LLM decision-making

#### Example:
- Document queries → RAG
- Invoice queries → DB Tool
- External queries → HTTP Tool

---

### ✅ 14. Agent Orchestrator (🔥 Key Feature)
- Built custom orchestration layer
- Controls:
    - Tool selection
    - Execution flow
    - Multi-step reasoning

#### Example Flow:
1. Fetch data from DB tool
2. Pass result to LLM
3. Generate final response

---


