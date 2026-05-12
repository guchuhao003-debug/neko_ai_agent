package com.wenxi.neko_ai_agent.agent;

import com.wenxi.neko_ai_agent.advisor.MyLoggerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * Neko AI Agent 超级智能体（拥有自主规划能力，可直接使用）
 */
//@Component
public class NekoManus extends ToolCallAgent{

    public NekoManus(ToolCallback[] allTools, ChatModel dashscopeChatModel) {
        super(allTools);
        this.setName("NekoManus");
        String SYSTEM_PROMPT = """
                You are NekoManus, an all-capable AI assistant, aimed at solving any task presented by the user. \s
                You have various tools at your disposal that you can call upon to efficiently complete complex requests.
                Please note that for every response, you must integrate the results from queries or tool calls before replying to the user.
                """;
        this.setSystemPrompt(SYSTEM_PROMPT);
        String NEXT_STEP_PROMPT = """
                Based on user needs, proactively select the most appropriate tool or combination of tools. \s
                For complex tasks, you can break down the problem and use different tools step by step to solve it. \s
                After using each tool, clearly explain the execution results and suggest the next steps. \s
                If you want to stop the interaction at any point, use the `terminate` tool/function call. \s
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
