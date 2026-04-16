package com.example.loganalyzer;

import com.example.loganalyzer.agent.LogAnalyzerAgent;
import com.example.loganalyzer.agent.LogCollectorAgent;
import com.example.loganalyzer.agent.ReportGeneratorAgent;
import com.example.loganalyzer.tool.LogDownloaderTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

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

        String logUrl = args[0];
        ChatModel model = createChatModel();

        LogCollectorAgent logCollector = AiServices.builder(LogCollectorAgent.class)
                .chatModel(model)
                .tools(new LogDownloaderTool())
                .build();

        LogAnalyzerAgent logAnalyzer = AiServices.builder(LogAnalyzerAgent.class)
                .chatModel(model)
                .build();

        ReportGeneratorAgent reportGenerator = AiServices.builder(ReportGeneratorAgent.class)
                .chatModel(model)
                .build();

        System.out.println("=== Agent 1: Collecting logs from " + logUrl + " ===");
        String rawLogs = logCollector.collectLogs(logUrl);

        System.out.println("=== Agent 2: Analyzing log content ===");
        String analysis = logAnalyzer.analyzeLogs(rawLogs);

        System.out.println("=== Agent 3: Generating Markdown report ===");
        String report = reportGenerator.generateReport(analysis);

        Path reportPath = Path.of("report.md");
        Files.writeString(reportPath, report);
        System.out.println("\nReport written to " + reportPath.toAbsolutePath());
        System.out.println("\n" + report);
    }

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
