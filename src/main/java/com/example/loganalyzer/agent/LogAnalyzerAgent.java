package com.example.loganalyzer.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface LogAnalyzerAgent {

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
    String analyzeLogs(String rawLogs);
}
