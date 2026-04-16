package com.example.loganalyzer.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface ReportGeneratorAgent {

    @SystemMessage("""
            You are a technical report writer.
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
            """)
    @UserMessage("Generate a Markdown report from the following analysis:\n\n{{analysis}}")
    String generateReport(String analysis);
}
