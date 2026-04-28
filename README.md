# LangChain4j Log Analyzer

A 3-agent sequential workflow built with [LangChain4j](https://github.com/langchain4j/langchain4j) and the [langchain4j-agentic](https://github.com/langchain4j/langchain4j/blob/main/docs/docs/tutorials/agents.md) framework that downloads log files from a URL, analyzes them for errors, and generates a structured Markdown report.

## Agents

The pipeline is composed as a sequential workflow using `AgenticServices.sequenceBuilder()`:

1. **LogCollectorAgent** - Downloads log file content from a user-supplied URL using an HTTP tool
2. **LogAnalyzerAgent** - Analyzes raw logs to extract errors, warnings, stack traces, and recurring patterns
3. **ReportGeneratorAgent** - Produces a well-formatted Markdown report from the analysis

A top-level **LogAgent** interface orchestrates the sequence, and each agent is wired with an `AgentListener` that logs invocation start/end for observability.

## Prerequisites

- Java 21+
- Maven 3.9+
- Docker (for integration tests)

## Configuration

The application supports two LLM providers via environment variables:

### OpenAI (default)

```bash
export OPENAI_API_KEY=sk-...
export OPENAI_MODEL=gpt-4o-mini    # optional, default: gpt-4o-mini
```

If `OPENAI_API_KEY` is not set, the app falls back to a rate-limited langchain4j demo key.

### Ollama (local)

```bash
export LLM_PROVIDER=ollama
export OLLAMA_BASE_URL=http://localhost:11434   # optional
export OLLAMA_MODEL=llama3.1                    # optional
```

## Build

```bash
mvn clean compile
```

## Run

```bash
mvn exec:java -Dexec.args="https://example.com/path/to/logfile.log"
```

The generated report is printed to stdout and saved to `report.md`.

## Programmatic Usage

The pipeline logic is available as a static method for embedding in other code or tests:

```java
ChatModel model = OllamaChatModel.builder()
        .baseUrl("http://localhost:11434")
        .modelName("llama3.1")
        .build();

String report = LogAnalyzerApp.analyzeLogs("https://example.com/logfile.log", model);
```

## Integration Test

The integration test uses [Testcontainers](https://testcontainers.com/) to spin up an Ollama container, pull the `qwen3:0.6b` model, and run the full pipeline against a mock log server. Requires Docker.

```bash
systemctl --user daemon-reload
systemctl --user start podman.socket
export DOCKER_HOST=unix:///run/user/$UID/podman/podman.sock
#We're not running podman privileged
export TESTCONTAINERS_RYUK_DISABLED=true
mvn verify
```

This runs `LogAnalyzerPipelineIT` which:
1. Starts an Ollama container and pulls a small model
2. Starts an embedded HTTP server serving sample log content
3. Calls `LogAnalyzerApp.analyzeLogs()` with the Ollama model
4. Asserts the pipeline produces a non-empty Markdown report
