package com.example.loganalyzer;

import com.example.loganalyzer.agent.LogAnalyzerAgent;
import com.example.loganalyzer.agent.LogCollectorAgent;
import com.example.loganalyzer.agent.ReportGeneratorAgent;
import com.example.loganalyzer.tool.LogDownloaderTool;
import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.ollama.OllamaContainer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
class LogAnalyzerPipelineIT {

    private static final String MODEL_NAME = "qwen3:0.6b";

    private static final String SAMPLE_LOG = """
            2024-01-15 10:00:01,234 INFO  [org.jboss.as.server] WFLYSRV0010: Deployed "app.war"
            2024-01-15 10:05:12,567 WARN  [org.hibernate.engine] HHH000444: Connection pool exhaustion detected
            2024-01-15 10:05:13,890 ERROR [com.example.service.UserService] Failed to process user request
            java.lang.NullPointerException: Cannot invoke method on null object
                at com.example.service.UserService.getUser(UserService.java:42)
                at com.example.controller.UserController.handleRequest(UserController.java:28)
                at org.jboss.as.ee.component.interceptors.InvocationContextInterceptor.lambda$0(InvocationContextInterceptor.java:76)
            2024-01-15 10:10:45,123 INFO  [org.jboss.as.server] WFLYSRV0025: Running periodic health check
            2024-01-15 10:15:22,456 ERROR [com.example.service.OrderService] Database connection timeout
            java.sql.SQLTimeoutException: Connection timed out after 30000ms
                at com.zaxxer.hikari.pool.HikariPool.getConnection(HikariPool.java:195)
                at com.example.service.OrderService.placeOrder(OrderService.java:67)
            2024-01-15 10:15:23,789 WARN  [org.hibernate.engine] HHH000444: Connection pool exhaustion detected
            2024-01-15 10:20:00,012 ERROR [com.example.service.UserService] Failed to process user request
            java.lang.NullPointerException: Cannot invoke method on null object
                at com.example.service.UserService.getUser(UserService.java:42)
                at com.example.controller.UserController.handleRequest(UserController.java:28)
            2024-01-15 10:25:33,345 INFO  [org.jboss.as.server] WFLYSRV0010: Redeploying "app.war"
            """;

    @Container
    static OllamaContainer ollama = new OllamaContainer("ollama/ollama:latest");

    static HttpServer logServer;
    static int logServerPort;

    @BeforeAll
    static void setUp() throws IOException, InterruptedException {
        ollama.execInContainer("ollama", "pull", MODEL_NAME);

        logServer = HttpServer.create(new InetSocketAddress(0), 0);
        logServerPort = logServer.getAddress().getPort();
        logServer.createContext("/test.log", exchange -> {
            byte[] body = SAMPLE_LOG.getBytes();
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        logServer.start();
    }

    @AfterAll
    static void tearDown() {
        if (logServer != null) {
            logServer.stop(0);
        }
    }

    @Test
    void fullPipelineProducesMarkdownReport() {
        ChatModel model = OllamaChatModel.builder()
                .baseUrl(ollama.getEndpoint())
                .modelName(MODEL_NAME)
                .timeout(Duration.ofMinutes(5))
                .build();

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

        String logUrl = "http://host.testcontainers.internal:" + logServerPort + "/test.log";

        String rawLogs = logCollector.collectLogs(logUrl);
        assertNotNull(rawLogs);
        assertFalse(rawLogs.isBlank(), "Raw logs should not be blank");

        String analysis = logAnalyzer.analyzeLogs(rawLogs);
        assertNotNull(analysis);
        assertFalse(analysis.isBlank(), "Analysis should not be blank");

        String report = reportGenerator.generateReport(analysis);
        assertNotNull(report);
        assertFalse(report.isBlank(), "Report should not be blank");
        System.out.println("=== Generated Report ===\n" + report);
    }
}
