package com.example.loganalyzer.agent;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

/**
 * Agent that analyzes raw log content to extract errors, warnings, stack traces,
 * recurring patterns, and anomalies.
 */
public interface LogAnalyzerAgent {

    /**
     * Analyzes the provided raw log content and returns a structured analysis.
     *
     * @param rawLogs the raw log content to analyze
     * @return a structured text analysis of the log content
     */
    @SystemMessage("""
            You are an expert log analyzer.
            Analyze the provided log content and extract:
            1. All ERROR level entries with their timestamps and messages
            2. All WARNING level entries with their timestamps and messages
            3. Stack traces and their root causes
            4. Recurring error patterns and their frequency
            5. Any notable anomalies or trends
            Provide your analysis in a structured text format.
            """)
    @UserMessage("Analyze the following log content:\n\n{{rawLogs}}")
    @Agent(description = "Expert log analyzer", outputKey = "analysis")
    String analyzeLogs(@V("rawLogs") String rawLogs);
}
