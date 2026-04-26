package com.wenxi.neko_ai_agent.tools;

import org.springframework.ai.tool.annotation.Tool;

/**
 * 终止任务工具（作用是让自主规划智能体能够合理停止中断任务）
 */
public class TerminateTool {

    @Tool(description = """
            Terminate the interaction when request is met OR if the assistant cannot proceed further with the task.
            when you have finished all the tasks, call this tool to end the work
            """)
    public String doTerminate() {
        return "任务结束";
    }
}
