# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

MyAIAgent02 is a Spring AI + Vue.js AI agent demonstration project featuring:
- **Backend**: Spring Boot 3.5.0 with Spring AI 1.1.2 and Spring AI Alibaba 1.1.2.0
- **Frontend**: Vue 3 single-page application with Vite
- **AI Model**: Dashscope/Qwen via Alibaba Cloud (qwen3-max model)
- **Core Features**:
  - Conversation memory with session management
  - RAG (Retrieval-Augmented Generation) with in-memory vector store
  - Extensible skills system (calculator, weather, time)
  - Server-Sent Events (SSE) for streaming responses
  - Multi-language support (English/Chinese) with vue-i18n
  - Session persistence via localStorage

## Project Structure

```
MyAIAgent/                          # Project root
├── MyAIAgent02/                    # Backend module (Maven multi-module parent)
│   ├── backend/                    # Spring Boot application
│   │   ├── src/main/java/com/example/aiagent/
│   │   └── pom.xml
│   └── pom.xml                     # Parent POM (defines modules)
├── MyAIAgent02 - frontend/         # Frontend directory
│   └── frontend/                   # Vue application
│       ├── src/
│       └── package.json
└── CLAUDE.md                       # This file
```

**Note**: The parent POM uses Spring Boot 3.5.0 and manages Spring AI dependencies. Repository configurations are commented out in pom.xml due to potential SSL certificate issues with Aliyun repositories.

## Common Development Commands

### Fixed Local Toolchain
Use this machine-specific toolchain by default instead of probing older system defaults:
- Maven home: `D:\DevelopmentTools\Maven\apache-maven-3.9.14`
- Maven local repository: `D:\DevelopmentTools\Maven\ngRepository`
- JDK home: `D:\DevelopmentEnvironment\jdk\jdk-17_windows-x64_bin\jdk-17.0.6`

Recommended environment setup before backend builds:
```powershell
$env:JAVA_HOME='D:\DevelopmentEnvironment\jdk\jdk-17_windows-x64_bin\jdk-17.0.6'
$env:PATH='D:\DevelopmentEnvironment\jdk\jdk-17_windows-x64_bin\jdk-17.0.6\bin;D:\DevelopmentTools\Maven\apache-maven-3.9.14\bin;' + $env:PATH
```

Recommended Maven invocation:
```powershell
mvn --% -Dmaven.repo.local=D:\DevelopmentTools\Maven\ngRepository -pl agent-api -am test
```

### Backend (Spring Boot)
```bash
# Navigate to backend module
cd MyAIAgent02/backend

# Build and run the application
mvn spring-boot:run

# Build without running
mvn clean package

# Run tests (if any)
mvn test
```

### Frontend (Vue 3 + Vite)
```bash
# Navigate to frontend directory
cd "MyAIAgent02 - frontend/frontend"

# Install dependencies
npm install

# Start development server (runs on http://localhost:10000)
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

### Running the Full Application
1. Start backend: `mvn spring-boot:run` from `MyAIAgent02/backend`
2. Start frontend: `npm run dev` from `MyAIAgent02 - frontend/frontend`
3. Access application at http://localhost:10000

## High-Level Architecture

### Backend Structure
- **`com.example.aiagent.controller.ChatController`**: REST API endpoints for chat, document upload, skills, and session management
- **`com.example.aiagent.service.ChatService`**: Core AI chat logic using Spring AI ChatClient and ChatMemory
- **`com.example.aiagent.service.ConversationService`**: In-memory session storage with metadata management (ConcurrentHashMap)
- **`com.example.aiagent.service.ChatMemoryFactory`**: Adapter implementing Spring AI `ChatMemory` interface for conversation history
- **`com.example.aiagent.service.RagService`**: Simple in-memory vector store for document retrieval
- **`com.example.aiagent.service.SkillService`**: Extensible skill execution system
- **`com.example.aiagent.config.AiConfig`**: Spring AI configuration and bean definitions
- **DTOs**: `ChatRequest`, `ChatResponse`, `SessionMetadata`, etc. in `com.example.aiagent.dto`

Key architectural decisions:
- Uses Spring AI's `ChatMemory` interface for conversation history abstraction
- Session data stored in memory with automatic cleanup (max 100 sessions, 20 messages per session)
- Frontend manages session IDs and persists them in localStorage
- RAG uses simple cosine similarity on in-memory embeddings (not persistent across restarts)

### Frontend Structure
- **`src/App.vue`**: Main application component with chat interface and session sidebar
- **`src/components/`**: Reusable Vue components (none currently)
- **`src/locales/`**: i18n translation files (en.json, zh.json)
- **`vite.config.js`**: Vite configuration with proxy to backend (localhost:8081 → /api)
- **Session Management**: Implemented directly in App.vue with localStorage persistence

### API Endpoints
- `POST /api/chat` - Send message, get AI response (with session memory)
- `POST /api/chat/stream` - SSE streaming endpoint for real-time responses
- `POST /api/upload` - Upload document text to vector store
- `POST /api/skill` - Execute a skill by name with input
- `GET /api/skills` - List available skills
- `GET /api/health` - Health check
- `GET /api/sessions` - List all sessions with metadata
- `POST /api/sessions` - Create new session
- `PUT /api/sessions/{sessionId}` - Update session (rename)
- `DELETE /api/sessions/{sessionId}` - Delete session and history
- `GET /api/history/{sessionId}` - Get conversation history
- `DELETE /api/history/{sessionId}` - Clear history for session

## Configuration

### Backend Configuration (`application.yml`)
- Server port: 8081
- Dashscope API key: `spring.ai.dashscope.api-key` (required)
- AI model: `qwen3-max` with temperature 0.7
- Embedding model: `text-embedding-v2`
- Logging: DEBUG for `com.example.aiagent`, INFO for Spring AI

### Frontend Configuration (`vite.config.js`)
- Development server port: 10000
- Proxy: `/api` → `http://localhost:8081`
- Vue 3 with TypeScript support

### Environment Variables
- `OPENAI_API_KEY` - Not used (project uses Dashscope)
- `DASHSCOPE_API_KEY` - Set via `application.yml` or environment variable

## Skills System
The project includes an extensible skill interface. Current skills:
1. **calculator**: Basic arithmetic (`/skill calculator "2+2"`)
2. **weather**: Simulated weather for any location
3. **time**: Current date and time

Skills can be added by implementing `Skill` interface and registering in `SkillService`.

## Session Memory Implementation
- **Session IDs**: Generated as `session-` + UUID prefix, managed by frontend
- **Metadata**: Each session has title, creation time, last active time, message count
- **Auto-title**: First user message truncated to 30 characters as default title
- **Storage**: `ConversationService` uses `ConcurrentHashMap` for thread-safe in-memory storage
- **Limits**: Maximum 100 sessions, 20 messages per session (oldest removed)
- **Persistence**: Frontend stores session list and active session ID in localStorage

## Important Notes
- The vector store is in-memory only; documents are lost on backend restart
- MCP (Model Context Protocol) integration is simulated through the skills system
- CORS is configured for localhost:10000 to communicate with backend on port 8081
- Session memory uses Spring AI's `ChatMemory` interface but with custom implementation
- Frontend uses vue-i18n for translations; update both en.json and zh.json for new strings
- Maven repository configurations are commented out due to SSL certificate issues; uncomment in pom.xml if needed
- Backend uses Spring Boot 3.5.0 with Java 17 requirement
