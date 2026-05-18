package com.wenxi.neko_ai_agent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.wenxi.neko_ai_agent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用于创作实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（需调用哪些工具）
    private ChatResponse toolCallChatResponse;

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    public ToolCallAgent(ToolCallback[] availableTools) {
        super(); // 调用父组件
        this.availableTools = availableTools;
        this.toolCallingManager = ToolCallingManager.builder().build();
        // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文 （
        // 原因 ： 阿里云的 DashscopeChat API 与 Spring AI 内置的工具调用机制不兼容，而不能使用原生的手动调用机制
        // 所以需要禁用 Spring AI 内置的工具调用机制
        // ）
        this.chatOptions = DashScopeChatOptions.builder()
                // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文 （
                .withInternalToolExecutionEnabled(false)
                .build();
    }

    /**
     * 处理当前状态并决定下一步行动
     * @return 是否需要执行行动
     */
    @Override
    public boolean think() {
        try {
            // 1.校验提示词，拼接用户提示词
            if(StrUtil.isNotBlank(getNextStepPrompt())) {
                UserMessage userMessage = new UserMessage(getNextStepPrompt());
                getMessageList().add(userMessage);  // 添加至上下文列表中
            }
            // 2.调用 AI 大模型，获取工具调用列表
            List<Message> messageList = getMessageList();
            Prompt prompt = new Prompt(messageList, this.chatOptions);
            ChatResponse chatResponse = getChatClient().prompt(prompt)
                    .system(getSystemPrompt())
                    .toolCallbacks(availableTools)
                    .call()
                    .chatResponse();
            // 记录响应，用于等下 Act
            this.toolCallChatResponse = chatResponse;
            // 3.解析工具调用结果，获取需要调用的工具
            // AI 助手消息
            AssistantMessage assistantMessage = chatResponse.getResult().getOutput();
            // 获取需要调用的工具列表
            List<AssistantMessage.ToolCall> toolCallList = assistantMessage.getToolCalls();
            // 输出提示信息
            String result = assistantMessage.getText();
            log.info(getName() + "的思考: " + result);
            log.info(getName() + "选择了 " + toolCallList.size() + " 个工具来使用");
            String toolCallInfo = toolCallList.stream()
                    .map(toolCall ->
                            String.format("工具名称: %s, 参数: %s", toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            // 如果不需要调用工具，返回 false
            if(toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                getMessageList().add(assistantMessage);
                return false;
            } else {
                // 需要调用工具时，等思考完成后会执行行动，行动后返回的结果会包含助手消息，
                // 所以此处需要调用工具时不需要手动添加助手消息，避免重复
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考发生异常: " + e.getMessage());
            getMessageList().add(new AssistantMessage("处理时遇到了错误: " + e.getMessage()));
            return false;
        }
    }

    /**
     * 执行工具调用并处理结果
     * @return 执行结果
     */
    @Override
    public String act() {
        if(!toolCallChatResponse.hasToolCalls()){
            return "无工具需要调用";
        }
        // 调用工具
        Prompt prompt = new Prompt(getMessageList(), this.chatOptions);
        ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        setMessageList(toolExecutionResult.conversationHistory());
        // 获取工具调用返回的最后一条结果
        ToolResponseMessage toolResponseMessage = (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
        // 判断是否调用了终止工具
        boolean terminateToolCalled = toolResponseMessage.getResponses().stream()
                .anyMatch(response -> isTerminateTool(response.name()));
        // 如果调用了终止任务工具，则需要修改代理状态 -> FINISHED
        if(terminateToolCalled) {
            // 任务终止，修改状态
            setState(AgentState.FINISHED);
            log.info("{} 调用了终止工具，停止后续步骤。", getName());
            return "任务结束";
        }
        // 获取每个工具的响应信息
        String results = toolResponseMessage.getResponses().stream()
                .map(response -> "工具" + response.name() + "返回的结果: " + response.responseData())
                .collect(Collectors.joining("\n"));

        log.info(results);
        return results;
    }

    /**
     * 构建面向用户的 Markdown 最终答复。
     *
     * @param userPrompt 用户原始任务
     * @param stepResults 步骤执行结果
     * @return Markdown 最终答复
     */
    @Override
    protected String buildFinalAnswer(String userPrompt, List<String> stepResults) {
        if (stepResults == null || stepResults.isEmpty()) {
            return "";
        }
        String finalPrompt = """
                请基于用户任务和工具执行结果，生成给用户看的最终答复。

                要求：
                1. 只输出 Markdown 纯文本；
                2. 不要输出 JSON、工具名称、函数名、Step 编号或原始工具返回字段；
                3. 如果工具结果包含搜索结果，请综合为自然语言说明，并保留可点击链接；
                4. 如果信息不足，请明确说明无法确认，不要编造。

                用户任务：
                %s

                工具执行结果：
                %s
                """.formatted(userPrompt, String.join("\n\n", stepResults));
        try {
            ChatResponse chatResponse = getChatClient().prompt()
                    .system(getSystemPrompt())
                    .user(finalPrompt)
                    .call()
                    .chatResponse();
            return chatResponse.getResult().getOutput().getText();
        } catch (Exception e) {
            log.warn("{} 生成最终答复失败: {}", getName(), e.getMessage());
            return "## 处理结果\n\n已完成工具调用，但最终总结生成失败，请稍后重试。";
        }
    }

    /**
     * 判断工具名是否为终止工具。
     *
     * @param toolName 工具名
     * @return 是否终止工具
     */
    private boolean isTerminateTool(String toolName) {
        return StrUtil.isNotBlank(toolName)
                && toolName.toLowerCase().contains("terminate");
    }
}
