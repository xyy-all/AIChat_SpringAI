# AI Agent with Spring AI, RAG, MCP, and Skills

A simple AI agent demonstration using modern AI technologies with a Vue-based web interface.

## Features

- **RAG (Retrieval-Augmented Generation)**: Store documents in a vector store and retrieve relevant context for answering questions
- **Skills System**: Extensible skills that the AI can execute (calculator, weather, time, etc.)
- **MCP Integration**: Model Context Protocol support for connecting to external tools (simulated)
- **Vue Frontend**: Modern web interface for chatting, uploading documents, and executing skills
- **Spring AI Backend**: Leverages OpenAI models and in-memory vector store

## Project Structure

```
backend/           # Spring Boot backend
  src/main/java/com/example/aiagent/
    config/        # Configuration classes
    controller/    # REST API endpoints
    dto/           # Data transfer objects
    service/       # Business logic (RAG, Chat, Skills)
    skill/         # Skill implementations
  pom.xml          # Maven dependencies

frontend/          # Vue 3 frontend
  src/
    App.vue        # Main Vue component
    main.js        # Vue application entry
    style.css      # Global styles
  index.html
  package.json
  vite.config.js
```

## Prerequisites

- Java 17 or later
- Maven 3.6+
- Node.js 18+ and npm
- OpenAI API key (for chat and embeddings)

## Setup

### Backend

1. Navigate to the `backend` directory:
   ```bash
   cd backend
   ```

2. Set your OpenAI API key as an environment variable:
   ```bash
   export OPENAI_API_KEY=your-api-key-here
   # On Windows:
   # set OPENAI_API_KEY=your-api-key-here
   ```

3. Build and run the Spring Boot application:
   ```bash
   mvn spring-boot:run
   ```
   The backend will start on `http://localhost:8081`

### Frontend

1. Navigate to the `frontend` directory:
   ```bash
   cd frontend
   ```

2. Install dependencies:
   ```bash
   npm install
   ```

3. Start the development server:
   ```bash
   npm run dev
   ```
   The frontend will start on `http://localhost:8080`

## Usage

1. Open `http://localhost:8080` in your browser
2. Use the chat interface to ask questions
3. Upload documents via the sidebar to populate the RAG vector store
4. Execute skills like calculator, weather, and time from the skills panel
5. Use `/skill <name> <input>` in chat to invoke skills directly

## API Endpoints

- `POST /api/chat` - Send a message to the AI
- `POST /api/upload` - Upload a document to the vector store
- `POST /api/skill` - Execute a skill
- `GET /api/skills` - List available skills
- `GET /api/health` - Health check

## Skills

The system includes the following built-in skills:

- **calculator**: Performs basic arithmetic (`2 + 3`, `10 * 5`, etc.)
- **weather**: Returns simulated weather for a location
- **time**: Returns current date and time

### Adding New Skills

1. Create a new class in the `skill` package implementing the `Skill` interface
2. Annotate with `@Component`
3. The skill will be automatically registered

## MCP Integration

The project includes a simulated MCP (Model Context Protocol) layer through the skills system. In a production environment, you could integrate with a full MCP server to connect with external tools and data sources.

## Configuration

- Backend port: `8081` (configure in `backend/src/main/resources/application.yml`)
- Frontend port: `8080` (configure in `frontend/vite.config.js`)
- OpenAI model: `gpt-3.5-turbo` (configure in `application.yml`)

## Future Enhancements

- Persistent vector store (Redis, PostgreSQL)
- File upload support (PDF, Word, etc.)
- Real MCP server integration
- User authentication
- Chat history persistence
- More sophisticated skill detection

## License

MIT