package com.wenxi.neko_ai_agent.controller;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 对话历史控制器测试。
 */
class ChatHistoryControllerTest {

    private final ChatHistoryController chatHistoryController = new ChatHistoryController();

    /**
     * 自定义智能体对话历史应支持 agent-{agentId} 应用类型。
     */
    @Test
    void isValidAppTypeShouldAllowCustomAgentType() {
        Boolean valid = ReflectionTestUtils.invokeMethod(chatHistoryController,
                "isValidAppType", "agent-123456");

        assertTrue(Boolean.TRUE.equals(valid));
    }

    /**
     * 自定义智能体应用类型必须带数字智能体 ID。
     */
    @Test
    void isValidAppTypeShouldRejectInvalidCustomAgentType() {
        Boolean missingId = ReflectionTestUtils.invokeMethod(chatHistoryController,
                "isValidAppType", "agent-");
        Boolean invalidId = ReflectionTestUtils.invokeMethod(chatHistoryController,
                "isValidAppType", "agent-demo");

        assertFalse(Boolean.TRUE.equals(missingId));
        assertFalse(Boolean.TRUE.equals(invalidId));
    }
}
