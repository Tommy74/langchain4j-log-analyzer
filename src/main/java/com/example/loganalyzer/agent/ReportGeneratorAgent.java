package com.example.loganalyzer.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agent that generates a well-structured Markdown report from the log analysis output.
 */
public interface ReportGeneratorAgent {

    /**
     * Generates a Markdown report from the provided analysis.
     *
     * @param analysis the structured analysis produced by {@link LogAnalyzerAgent}
     * @return a Markdown-formatted report
     */
    @SystemMessage("""
            You are a technical quality engineer.
            Generate a well-structured Markdown report from the provided log analysis.
            The report must include these sections:
            - # Log Analysis Report
            - ## Executive Summary
            - ## Errors (use a table if applicable)
            - ## Warnings
            - ## Stack Traces (use code blocks)
            - ## Recurring Patterns
            - ## Recommendations
            Use proper Markdown formatting with headers, tables, code blocks, and bullet points.
            The analysis is {{analysis}}.
            """)
    @UserMessage("Generate a Markdown report from the following analysis:\n\n{{analysis}}")
    @Agent(description = "Expert report generator for logs", outputKey = "report")
    String generateReport(@V("analysis") String analysis);
}
