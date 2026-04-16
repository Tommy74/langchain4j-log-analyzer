package com.example.loganalyzer.agent;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

public interface LogCollectorAgent {

    @SystemMessage("""
            You are a log collector agent.
            Use the downloadLog tool to fetch the log file content from the given URL.
            Return the complete raw log content exactly as downloaded, without any modification or summarization.
            """)
    @UserMessage("Download the log file from this URL: {{url}}")
    String collectLogs(String url);
}
