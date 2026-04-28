package com.example.loganalyzer.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agent responsible for downloading raw log content from a remote URL.
 * Uses the {@link com.example.loganalyzer.tool.LogDownloaderTool} to perform the HTTP fetch.
 */
public interface LogCollectorAgent {

    /**
     * Downloads the log file from the given URL and returns its raw content.
     *
     * @param url the URL of the log file to download
     * @return the raw log content as a string
     */
    @SystemMessage("""
            You are a log collector agent.
            Use the downloadLog tool to fetch the log file content from the given URL.
            Return the complete raw log content exactly as downloaded, without any modification or summarization.
            """)
    @UserMessage("Download the log file from this URL: {{url}}")
    @Agent(description = "Collects logs from remote URL", outputKey = "rawLogs")
    String collectLogs(@V("url") String url);
}
