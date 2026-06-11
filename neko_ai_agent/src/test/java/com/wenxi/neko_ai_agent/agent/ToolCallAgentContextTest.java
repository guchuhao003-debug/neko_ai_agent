package com.wenxi.neko_ai_agent.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.tool.ToolCallback;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工具调用智能体上下文压缩单元测试。
 */
class ToolCallAgentContextTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private TestToolCallAgent agent;

    /**
     * 初始化测试智能体。
     */
    @BeforeEach
    void setUp() {
        agent = new TestToolCallAgent();
        agent.setNextStepPrompt("请规划下一步工具调用。");
    }

    /**
     * 历史压缩应保留原始任务、摘要和最近消息。
     */
    @Test
    void compactConversationHistoryShouldKeepBoundedContext() {
        List<Message> history = new ArrayList<>();
        history.add(new UserMessage("什么是 Spring AI"));
        for (int i = 0; i < 14; i++) {
            history.add(new AssistantMessage("第 " + i + " 轮工具规划"));
            history.add(new ToolResponseMessage(List.of(new ToolResponseMessage.ToolResponse(
                    "tool-call-" + i,
                    "webSearch",
                    "搜索结果".repeat(1000)
            ))));
        }

        List<Message> compactedHistory = agent.compact(history);

        assertTrue(compactedHistory.size() <= 10);
        assertEquals("什么是 Spring AI", compactedHistory.get(0).getText());
        assertTrue(compactedHistory.stream()
                .anyMatch(message -> message.getText().contains("已压缩的历史执行摘要")));
        assertFalse(compactedHistory.get(1).getMessageType() == MessageType.TOOL);
    }

    /**
     * 本轮规划提示只应进入 Prompt，不应污染长期历史。
     */
    @Test
    void buildCurrentPromptMessagesShouldNotPersistNextStepPrompt() {
        agent.setMessageList(new ArrayList<>(List.of(new UserMessage("查询 Spring AI"))));

        List<Message> promptMessages = agent.buildPrompt();

        assertTrue(promptMessages.get(promptMessages.size() - 1).getText()
                .contains("本轮规划指令"));
        assertEquals(1, agent.getMessageList().size());
        assertFalse(agent.getMessageList().get(0).getText().contains("本轮规划指令"));
    }

    /**
     * 工具参数历史压缩必须保持 JSON 合法，避免 DashScope 拒绝 function.arguments。
     */
    @Test
    void normalizeToolArgumentsShouldKeepValidJson() throws Exception {
        String arguments = """
                {
                    "fileName": "spring-ai-learning-plan.pdf",
                    "content": "%s"
                }
                """.formatted("Spring AI 学习计划".repeat(500));

        String normalized = agent.normalizeArguments(arguments);

        OBJECT_MAPPER.readTree(normalized);
        assertTrue(normalized.contains("spring-ai-learning-plan.pdf"));
        assertTrue(normalized.length() <= 3000);
    }

    /**
     * 非 JSON 工具参数也必须被包装为合法 JSON 对象。
     */
    @Test
    void normalizeToolArgumentsShouldWrapInvalidJson() throws Exception {
        String normalized = agent.normalizeArguments("not json arguments");

        assertTrue(OBJECT_MAPPER.readTree(normalized).has("_rawArguments"));
    }

    /**
     * 测试用工具调用智能体。
     */
    private static class TestToolCallAgent extends ToolCallAgent {

        TestToolCallAgent() {
            super(new ToolCallback[0]);
        }

        List<Message> compact(List<Message> history) {
            return compactConversationHistory(history);
        }

        List<Message> buildPrompt() {
            return buildCurrentPromptMessages();
        }

        String normalizeArguments(String arguments) {
            return normalizeToolArgumentsForHistory(arguments);
        }
    }
}
