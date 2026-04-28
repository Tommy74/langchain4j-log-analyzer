package com.example.loganalyzer.tool;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * LangChain4j tool that downloads log file content from a remote URL via HTTP.
 * Used by {@link com.example.loganalyzer.agent.LogCollectorAgent}.
 */
public class LogDownloaderTool {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /**
     * Downloads the log file at the given URL and returns its content as text.
     *
     * @param url the URL of the log file to download
     * @return the log file content, or an error message if the download fails
     */
    @Tool("Downloads the content of a log file from the given URL and returns it as text")
    public String downloadLog(@P("The URL of the log file to download") String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(60))
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                return response.body();
            }
            return "Error: HTTP status " + response.statusCode();
        } catch (Exception e) {
            return "Error downloading log: " + e.getMessage();
        }
    }
}
