package com.example.loganalyzer;

import com.example.loganalyzer.agent.LogAgent;
import com.example.loganalyzer.agent.LogAnalyzerAgent;
import com.example.loganalyzer.agent.LogCollectorAgent;
import com.example.loganalyzer.agent.ReportGeneratorAgent;
import com.example.loganalyzer.tool.LogDownloaderTool;
import dev.langchain4j.agentic.AgenticServices;
import dev.langchain4j.agentic.observability.AgentListener;
import dev.langchain4j.agentic.observability.AgentRequest;
import dev.langchain4j.agentic.observability.AgentResponse;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

/**
 * Main application that orchestrates the log analysis pipeline.
 * <p>
 * Builds a sequential agent pipeline (collect, analyze, report) using
 * {@link AgenticServices} and writes the resulting Markdown report to disk.
 */
public class LogAnalyzerApp {

    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.err.println("Usage: LogAnalyzerApp <log-file-url>");
            System.err.println();
            System.err.println("Environment variables:");
            System.err.println("  LLM_PROVIDER     - 'openai' (default) or 'ollama'");
            System.err.println("  OPENAI_API_KEY   - OpenAI API key (uses demo key if not set)");
            System.err.println("  OPENAI_MODEL     - OpenAI model name (default: gpt-4o-mini)");
            System.err.println("  OLLAMA_BASE_URL  - Ollama base URL (default: http://localhost:11434)");
            System.err.println("  OLLAMA_MODEL     - Ollama model name (default: llama3.1)");
            System.exit(1);
        }

        String report = analyzeLogs(args[0], createChatModel());

        Path reportPath = Path.of("report.md");
        Files.writeString(reportPath, report);
        System.out.println("\nReport written to " + reportPath.toAbsolutePath());
        System.out.println("\n" + report);
    }

    /**
     * Runs the full log analysis pipeline: collect, analyze, and generate a Markdown report.
     *
     * @param logUrl the URL of the log file to process
     * @param model  the chat model to use for all agents
     * @return the generated Markdown report
     * @throws IllegalArgumentException if {@code model} is null
     */
    public static String analyzeLogs(String logUrl, ChatModel model) {
        if (model == null) {
            throw new IllegalArgumentException("Chat model is null");
        }

        AgentListener listener = new AgentListener() {
            @Override
            public void beforeAgentInvocation(AgentRequest request) {
                System.out.println(">>> Invoking agent: " + request.agentName() + " with inputs: " + request.inputs());
            }

            @Override
            public void afterAgentInvocation(AgentResponse response) {
                System.out.println("<<< Agent completed: " + response.agentName());
            }
        };

        LogCollectorAgent logCollector = AgenticServices
                .agentBuilder(LogCollectorAgent.class)
                .chatModel(model)
                .tools(new LogDownloaderTool())
                .listener(listener)
                .build();

        LogAnalyzerAgent logAnalyzer = AgenticServices
                .agentBuilder(LogAnalyzerAgent.class)
                .chatModel(model)
                .listener(listener)
                .build();

        ReportGeneratorAgent reportGenerator = AgenticServices
                .agentBuilder(ReportGeneratorAgent.class)
                .chatModel(model)
                .listener(listener)
                .build();

        LogAgent logAgent = AgenticServices
                .sequenceBuilder(LogAgent.class)
                .subAgents(logCollector, logAnalyzer, reportGenerator)
                .build();

        System.out.println("=== Analyzing logs from " + logUrl + " ===");
        return logAgent.processLogs(logUrl);
    }

    /**
     * Creates a {@link ChatModel} based on environment variables.
     * Supports OpenAI (default) and Ollama providers.
     *
     * @return a configured chat model
     */
    static ChatModel createChatModel() {
        String provider = System.getenv().getOrDefault("LLM_PROVIDER", "openai");

        if ("ollama".equalsIgnoreCase(provider)) {
            String baseUrl = System.getenv().getOrDefault("OLLAMA_BASE_URL", "http://localhost:11434");
            String modelName = System.getenv().getOrDefault("OLLAMA_MODEL", "llama3.1");
            return OllamaChatModel.builder()
                    .baseUrl(baseUrl)
                    .modelName(modelName)
                    .timeout(Duration.ofMinutes(5))
                    .build();
        }

        String apiKey = System.getenv("OPENAI_API_KEY");
        String modelName = System.getenv().getOrDefault("OPENAI_MODEL", "gpt-4o-mini");

        if (apiKey == null || apiKey.isBlank()) {
            System.out.println("No OPENAI_API_KEY set, using langchain4j demo key (rate-limited).");
            return OpenAiChatModel.builder()
                    .baseUrl("https://langchain4j.dev/demo/openai/v1")
                    .apiKey("demo")
                    .modelName(modelName)
                    .build();
        }

        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
