package com.example.loganalyzer.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Top-level orchestrator agent that composes the collect, analyze, and report agents
 * into a single sequential pipeline via {@code AgenticServices.sequenceBuilder()}.
 */
public interface LogAgent {

    /**
     * Runs the full log analysis pipeline: download, analyze, and generate a report.
     *
     * @param url the URL of the log file to process
     * @return a Markdown-formatted analysis report
     */
    @SystemMessage("""
            You are an agent that download, analyzes and produces a report from some log file.
            """)
    @UserMessage("Download, analyze and produce a report for the log file from this URL: {{url}}")
    @Agent(description = "Download, analyzes and produces a report from some log file", outputKey = "report")
    String processLogs(@V("url") String url);
}
