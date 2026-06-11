package com.wenxi.neko_ai_agent.agent;

import com.wenxi.neko_ai_agent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

import java.util.concurrent.Executor;

/**
 * Neko AI Agent 超级智能体（拥有自主规划能力，可直接使用）
 */
//@Component
public class NekoManus extends ToolCallAgent {

    public NekoManus(ToolCallback[] allTools, ChatModel dashscopeChatModel, Executor executor) {
        super(allTools);
        this.setExecutor(executor);
        this.setName("NekoManus");
        String SYSTEM_PROMPT = """
                You are NekoManus, an all-capable AI assistant, aimed at solving any task presented by the user. \s
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                Please note that for every response, you must integrate the results from queries or tool calls before replying to the user.
                The final answer shown to the user must be clean Markdown text, never raw JSON or raw tool output.
                When the user refers to a file generated earlier in the same conversation, reuse the
                filename or /api/files/... link from the conversation history instead of regenerating it.
                If the user asks to send an existing generated file by email, call the email attachment
                tool with the known file reference; do not use text-only email for file delivery.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools. \s
                For complex tasks, you can break down the problem and use different tools step by step to solve it. \s
                After using each tool, clearly explain the execution results and suggest the next steps. \s
                For follow-up requests like "send the previous PDF to email", first inspect the existing
                conversation context for a generated file link or filename, then use the attachment email tool.
                Do not search, download, or regenerate the file unless no existing generated file can be found.
                If you want to stop the interaction at any point, use the `terminate` tool/function call. \s
                Once the terminate tool is called, do not continue planning or calling any other tool. \s
                """;
        this.setNextStepPrompt(NEXT_STEP_PROMPT);
        // 自定义设置最大步数限制
        this.setMaxSteps(20);
        // 初始化 AI 对话客户端
        ChatClient chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultAdvisors(
                        new MyLoggerAdvisor()
                )
                .build();
        this.setChatClient(chatClient);
    }



}
