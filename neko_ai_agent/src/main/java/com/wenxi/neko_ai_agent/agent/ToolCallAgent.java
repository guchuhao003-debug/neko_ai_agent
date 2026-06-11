package com.wenxi.neko_ai_agent.agent;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.wenxi.neko_ai_agent.agent.model.AgentState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 处理工具调用的基础代理类，具体实现了 think 和 act 方法，可以用于创作实例的父类
 */
@EqualsAndHashCode(callSuper = true)
@Data
@Slf4j
public class ToolCallAgent extends ReActAgent {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final int RECENT_CONTEXT_MESSAGE_LIMIT = 8;

    private static final int SINGLE_MESSAGE_TEXT_LIMIT = 3000;

    private static final int TOOL_ARGUMENT_STRING_LIMIT = 1000;

    private static final int SUMMARY_TEXT_LIMIT = 6000;

    private static final int FINAL_STEP_RESULT_LIMIT = 1600;

    private static final int FINAL_STEP_RESULT_COUNT = 8;

    private static final Pattern GENERATED_FILE_LINK_PATTERN =
            Pattern.compile("\\[([^\\]]+)]\\(([^)]+/files/[^)]+)\\)");

    private static final String TOOL_ARGUMENT_TRUNCATED_MARKER = "\n...[内容已压缩截断]";

    private static final String HISTORY_SUMMARY_PREFIX = "【已压缩的历史执行摘要】";

    private static final String NEXT_STEP_PROMPT_PREFIX = "【本轮规划指令】";

    // 可用的工具
    private final ToolCallback[] availableTools;

    // 保存工具调用信息的响应结果（需调用哪些工具）
    private ChatResponse toolCallChatResponse;

    // 保存本轮无需工具时可以直接给用户展示的回答
    private String directFinalAnswer = "";

    // 工具调用管理者
    private final ToolCallingManager toolCallingManager;

    // 禁用 Spring AI 内置的工具调用机制，自己维护选项和消息上下文
    private final ChatOptions chatOptions;

    // 本轮 think 使用的有界上下文，act 执行工具时必须复用同一份 Prompt。
    private List<Message> currentPromptMessages = new ArrayList<>();

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
            this.directFinalAnswer = "";
            // 1.构建本轮有界上下文，避免每轮 nextStepPrompt 永久膨胀。
            List<Message> promptMessages = buildCurrentPromptMessages();
            this.currentPromptMessages = promptMessages;
            // 2.调用 AI 大模型，获取工具调用列表
            Prompt prompt = new Prompt(promptMessages, this.chatOptions);
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
                            String.format("工具名称: %s, 参数: %s",
                                    toolCall.name(), toolCall.arguments()))
                    .collect(Collectors.joining("\n"));
            log.info(toolCallInfo);
            // 如果不需要调用工具，返回 false
            if(toolCallList.isEmpty()) {
                // 只有不调用工具时，才需要手动记录助手消息
                this.directFinalAnswer = StrUtil.blankToDefault(result, "任务已完成");
                List<Message> newHistory = new ArrayList<>(getMessageList());
                newHistory.add(normalizeMessage(assistantMessage));
                setMessageList(compactConversationHistory(newHistory));
                return false;
            } else {
                // 需要调用工具时，等思考完成后会执行行动，行动后返回的结果会包含助手消息，
                // 所以此处需要调用工具时不需要手动添加助手消息，避免重复
                return true;
            }
        } catch (Exception e) {
            log.error(getName() + "的思考发生异常: " + e.getMessage());
            this.directFinalAnswer = "处理时遇到了错误：" + e.getMessage();
            setState(AgentState.FINISHED);
            List<Message> newHistory = new ArrayList<>(getMessageList());
            newHistory.add(new AssistantMessage(this.directFinalAnswer));
            setMessageList(compactConversationHistory(newHistory));
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
        List<Message> promptMessages = currentPromptMessages.isEmpty()
                ? buildCurrentPromptMessages()
                : currentPromptMessages;
        Prompt prompt = new Prompt(promptMessages, this.chatOptions);
        ToolExecutionResult toolExecutionResult =
                toolCallingManager.executeToolCalls(prompt, toolCallChatResponse);
        // 记录消息上下文，conversationHistory 已经包含了助手消息和工具调用返回的结果
        List<Message> conversationHistory =
                removeEphemeralPrompt(toolExecutionResult.conversationHistory());
        setMessageList(compactConversationHistory(conversationHistory));
        // 获取工具调用返回的最后一条结果
        ToolResponseMessage toolResponseMessage =
                (ToolResponseMessage) CollUtil.getLast(toolExecutionResult.conversationHistory());
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
     * 获取无需工具调用时的最终结果，并结束当前智能体运行。
     *
     * @return 可以直接展示给用户的结果
     */
    @Override
    protected String getNoActionResult() {
        setState(AgentState.FINISHED);
        return StrUtil.blankToDefault(directFinalAnswer, "任务已完成");
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
        String generatedFileAnswer = buildGeneratedFileAnswer(stepResults);
        if (StrUtil.isNotBlank(generatedFileAnswer)) {
            return generatedFileAnswer;
        }
        if (StrUtil.isNotBlank(directFinalAnswer)) {
            return directFinalAnswer;
        }
        if (stepResults == null || stepResults.isEmpty()) {
            return "";
        }
        String finalPrompt = """
                请基于用户任务和工具执行结果，生成给用户看的最终答复。

                要求：
                1. 只输出 Markdown 纯文本；
                2. 不要输出 JSON、工具名称、函数名、Step 编号或原始工具返回字段；
                3. 如果工具结果包含搜索结果或文件链接，请综合为自然语言说明，并保留可点击链接；
                4. 如果信息不足，请明确说明无法确认，不要编造。

                用户任务：
                %s

                工具执行结果：
                %s
                """.formatted(userPrompt, buildFinalStepContext(stepResults));
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

    /**
     * 构建最终答复使用的有界步骤上下文。
     *
     * @param stepResults 步骤结果
     * @return 有界上下文
     */
    private String buildFinalStepContext(List<String> stepResults) {
        if (stepResults == null || stepResults.isEmpty()) {
            return "";
        }
        int recentStart = Math.max(0, stepResults.size() - FINAL_STEP_RESULT_COUNT);
        List<String> oldResults = stepResults.subList(0, recentStart);
        List<String> recentResults = stepResults.subList(recentStart, stepResults.size());
        StringBuilder contextBuilder = new StringBuilder();
        if (!oldResults.isEmpty()) {
            contextBuilder.append("早期步骤摘要：\n");
            contextBuilder.append(limitText(String.join("\n", oldResults), SUMMARY_TEXT_LIMIT));
            contextBuilder.append("\n\n最近步骤：\n");
        }
        for (String result : recentResults) {
            if (contextBuilder.length() > 0) {
                contextBuilder.append("\n\n");
            }
            contextBuilder.append(limitText(result, FINAL_STEP_RESULT_LIMIT));
        }
        return contextBuilder.toString();
    }

    /**
     * 从工具结果中提取生成文件链接，避免文件任务再等待一次模型总结。
     *
     * @param stepResults 步骤结果
     * @return 文件结果答复
     */
    private String buildGeneratedFileAnswer(List<String> stepResults) {
        if (stepResults == null || stepResults.isEmpty()) {
            return "";
        }
        List<String> fileLinks = stepResults.stream()
                .flatMap(result -> extractGeneratedFileLinks(result).stream())
                .distinct()
                .toList();
        if (fileLinks.isEmpty()) {
            return "";
        }
        return "## 文件已生成\n\n" + fileLinks.stream()
                .map(link -> "- " + link)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 提取步骤结果中的 Markdown 文件链接。
     *
     * @param result 步骤结果
     * @return 文件链接列表
     */
    private List<String> extractGeneratedFileLinks(String result) {
        if (StrUtil.isBlank(result)) {
            return List.of();
        }
        Matcher matcher = GENERATED_FILE_LINK_PATTERN.matcher(result);
        List<String> links = new ArrayList<>();
        while (matcher.find()) {
            links.add("[" + matcher.group(1) + "](" + matcher.group(2) + ")");
        }
        return links;
    }

    /**
     * 归一化工具参数历史，保证 function.arguments 始终是合法 JSON。
     *
     * @param arguments 原始工具参数
     * @return 可发送给模型的 JSON 字符串
     */
    protected String normalizeToolArgumentsForHistory(String arguments) {
        if (StrUtil.isBlank(arguments)) {
            return "{}";
        }
        try {
            JsonNode jsonNode = OBJECT_MAPPER.readTree(arguments);
            JsonNode normalizedNode = jsonNode.isObject()
                    ? compactToolArgumentJson(jsonNode)
                    : wrapToolArgumentValue(jsonNode);
            String normalized = OBJECT_MAPPER.writeValueAsString(normalizedNode);
            if (normalized.length() <= SINGLE_MESSAGE_TEXT_LIMIT) {
                return normalized;
            }
            ObjectNode omittedNode = OBJECT_MAPPER.createObjectNode();
            omittedNode.put("_compressed", true);
            omittedNode.put("_note", "Tool arguments omitted from history because they are large.");
            return OBJECT_MAPPER.writeValueAsString(omittedNode);
        } catch (JsonProcessingException e) {
            ObjectNode fallbackNode = OBJECT_MAPPER.createObjectNode();
            fallbackNode.put("_rawArguments", limitText(arguments, TOOL_ARGUMENT_STRING_LIMIT));
            return fallbackNode.toString();
        }
    }

    /**
     * 压缩工具参数中的大字符串值，同时保持 JSON 结构合法。
     *
     * @param jsonNode 原始 JSON 节点
     * @return 压缩后的 JSON 节点
     */
    private JsonNode compactToolArgumentJson(JsonNode jsonNode) {
        if (jsonNode.isTextual()) {
            String text = jsonNode.asText();
            if (text.length() <= TOOL_ARGUMENT_STRING_LIMIT) {
                return jsonNode;
            }
            return OBJECT_MAPPER.getNodeFactory().textNode(
                    text.substring(0, TOOL_ARGUMENT_STRING_LIMIT) + TOOL_ARGUMENT_TRUNCATED_MARKER);
        }
        if (jsonNode.isObject()) {
            ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
            jsonNode.fields().forEachRemaining(entry ->
                    objectNode.set(entry.getKey(), compactToolArgumentJson(entry.getValue())));
            return objectNode;
        }
        if (jsonNode.isArray()) {
            return compactToolArgumentArray(jsonNode);
        }
        return jsonNode;
    }

    /**
     * 压缩工具参数中的数组节点。
     *
     * @param jsonNode 原始数组节点
     * @return 压缩后的数组节点
     */
    private JsonNode compactToolArgumentArray(JsonNode jsonNode) {
        var arrayNode = OBJECT_MAPPER.createArrayNode();
        jsonNode.forEach(item -> arrayNode.add(compactToolArgumentJson(item)));
        return arrayNode;
    }

    /**
     * 将非对象参数包装为对象，满足函数参数的 JSON 对象格式。
     *
     * @param jsonNode 原始 JSON 节点
     * @return 包装后的 JSON 对象
     */
    private JsonNode wrapToolArgumentValue(JsonNode jsonNode) {
        ObjectNode objectNode = OBJECT_MAPPER.createObjectNode();
        objectNode.set("value", compactToolArgumentJson(jsonNode));
        return objectNode;
    }

    /**
     * 构建本轮模型调用使用的有界上下文。
     *
     * @return 本轮 Prompt 消息
     */
    protected List<Message> buildCurrentPromptMessages() {
        List<Message> compactHistory = compactConversationHistory(getMessageList());
        setMessageList(compactHistory);
        List<Message> promptMessages = new ArrayList<>(compactHistory);
        if (StrUtil.isNotBlank(getNextStepPrompt())) {
            String planningPrompt = NEXT_STEP_PROMPT_PREFIX + "\n" + getNextStepPrompt();
            promptMessages.add(new UserMessage(planningPrompt));
        }
        return promptMessages;
    }

    /**
     * 压缩对话历史，保留原始任务、滚动摘要和最近消息。
     *
     * @param history 原始历史
     * @return 有界历史
     */
    protected List<Message> compactConversationHistory(List<Message> history) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }
        List<Message> cleanedHistory = removeEphemeralPrompt(history).stream()
                .map(this::normalizeMessage)
                .toList();
        if (cleanedHistory.size() <= RECENT_CONTEXT_MESSAGE_LIMIT + 2) {
            return new ArrayList<>(cleanedHistory);
        }

        Message originalTask = cleanedHistory.get(0);
        List<Message> middleMessages = cleanedHistory.subList(1, cleanedHistory.size());
        String previousSummary = extractHistorySummary(middleMessages);
        List<Message> nonSummaryMessages = middleMessages.stream()
                .filter(message -> !isHistorySummaryMessage(message))
                .toList();

        int recentStart = Math.max(0, nonSummaryMessages.size() - RECENT_CONTEXT_MESSAGE_LIMIT);
        List<Message> oldMessages = nonSummaryMessages.subList(0, recentStart);
        List<Message> recentMessages = new ArrayList<>(nonSummaryMessages.subList(recentStart,
                nonSummaryMessages.size()));
        removeInvalidLeadingToolResponse(recentMessages);

        List<Message> compactedHistory = new ArrayList<>();
        compactedHistory.add(originalTask);
        String summary = buildHistorySummary(previousSummary, oldMessages);
        if (StrUtil.isNotBlank(summary)) {
            compactedHistory.add(new SystemMessage(summary));
        }
        compactedHistory.addAll(recentMessages);
        return compactedHistory;
    }

    /**
     * 移除临时规划提示，避免它被写入长期历史。
     *
     * @param history 原始历史
     * @return 清理后的历史
     */
    protected List<Message> removeEphemeralPrompt(List<Message> history) {
        if (history == null || history.isEmpty()) {
            return new ArrayList<>();
        }
        return history.stream()
                .filter(message -> !isEphemeralPrompt(message))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * 归一化单条消息，截断过长文本和工具结果。
     *
     * @param message 原始消息
     * @return 归一化消息
     */
    private Message normalizeMessage(Message message) {
        if (message == null) {
            return new AssistantMessage("");
        }
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            List<ToolResponseMessage.ToolResponse> responses = toolResponseMessage.getResponses()
                    .stream()
                    .map(response -> new ToolResponseMessage.ToolResponse(
                            response.id(),
                            response.name(),
                            limitText(response.responseData(), SINGLE_MESSAGE_TEXT_LIMIT)
                    ))
                    .toList();
            return new ToolResponseMessage(responses);
        }
        if (message instanceof AssistantMessage assistantMessage) {
            List<AssistantMessage.ToolCall> toolCalls = assistantMessage.getToolCalls().stream()
                    .map(toolCall -> new AssistantMessage.ToolCall(
                            toolCall.id(),
                            toolCall.type(),
                            toolCall.name(),
                            normalizeToolArgumentsForHistory(toolCall.arguments())
                    ))
                    .toList();
            return new AssistantMessage(limitText(assistantMessage.getText(),
                    SINGLE_MESSAGE_TEXT_LIMIT), Map.of(), toolCalls);
        }
        if (message.getMessageType() == MessageType.SYSTEM) {
            return new SystemMessage(limitText(message.getText(), SINGLE_MESSAGE_TEXT_LIMIT));
        }
        if (message.getMessageType() == MessageType.USER) {
            return new UserMessage(limitText(message.getText(), SINGLE_MESSAGE_TEXT_LIMIT));
        }
        return message;
    }

    /**
     * 构造滚动摘要。
     *
     * @param previousSummary 既有摘要
     * @param oldMessages 本次需要压缩的旧消息
     * @return 摘要文本
     */
    private String buildHistorySummary(String previousSummary, List<Message> oldMessages) {
        StringBuilder summaryBuilder = new StringBuilder();
        if (StrUtil.isNotBlank(previousSummary)) {
            summaryBuilder.append(previousSummary.replace(HISTORY_SUMMARY_PREFIX, "").trim());
        }
        for (Message message : oldMessages) {
            if (summaryBuilder.length() > 0) {
                summaryBuilder.append("\n");
            }
            summaryBuilder.append(summarizeMessage(message));
        }
        String summary = limitText(summaryBuilder.toString(), SUMMARY_TEXT_LIMIT);
        if (StrUtil.isBlank(summary)) {
            return "";
        }
        return HISTORY_SUMMARY_PREFIX + "\n" + summary;
    }

    /**
     * 压缩单条消息为摘要行。
     *
     * @param message 消息
     * @return 摘要行
     */
    private String summarizeMessage(Message message) {
        if (message instanceof ToolResponseMessage toolResponseMessage) {
            return toolResponseMessage.getResponses().stream()
                    .map(response -> "工具 " + response.name() + " 返回: "
                            + limitText(response.responseData(), 800))
                    .collect(Collectors.joining("\n"));
        }
        String role = message.getMessageType() == null
                ? "UNKNOWN"
                : message.getMessageType().getValue();
        return role + ": " + limitText(message.getText(), 800);
    }

    /**
     * 提取已有历史摘要。
     *
     * @param messages 消息列表
     * @return 已有摘要
     */
    private String extractHistorySummary(List<Message> messages) {
        return messages.stream()
                .filter(this::isHistorySummaryMessage)
                .map(Message::getText)
                .collect(Collectors.joining("\n"));
    }

    /**
     * 判断是否是历史摘要消息。
     *
     * @param message 消息
     * @return 是否摘要
     */
    private boolean isHistorySummaryMessage(Message message) {
        return message != null
                && message.getMessageType() == MessageType.SYSTEM
                && StrUtil.startWith(message.getText(), HISTORY_SUMMARY_PREFIX);
    }

    /**
     * 判断是否是本轮临时规划提示。
     *
     * @param message 消息
     * @return 是否临时提示
     */
    private boolean isEphemeralPrompt(Message message) {
        return message != null
                && message.getMessageType() == MessageType.USER
                && StrUtil.startWith(message.getText(), NEXT_STEP_PROMPT_PREFIX);
    }

    /**
     * 去除开头孤立的工具响应，避免模型收到不合法的工具消息序列。
     *
     * @param messages 最近消息
     */
    private void removeInvalidLeadingToolResponse(List<Message> messages) {
        while (!messages.isEmpty() && messages.get(0).getMessageType() == MessageType.TOOL) {
            messages.remove(0);
        }
    }

    /**
     * 截断过长文本。
     *
     * @param text 原文本
     * @param maxLength 最大长度
     * @return 截断后的文本
     */
    private String limitText(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "\n...[内容已压缩截断]";
    }
}
