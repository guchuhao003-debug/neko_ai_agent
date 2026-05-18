package com.wenxi.neko_ai_agent.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * AI 控制器单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AiControllerTest {

    private AiController aiController;

    @Mock
    private ChatModel dashscopeChatModel;

    @Mock
    private ChatModel configuredQwenPlusModel;

    @Mock
    private ChatModel deepSeekChatModel;

    /**
     * 初始化控制器与模型映射。
     */
    @BeforeEach
    void setUp() {
        aiController = new AiController();
        ReflectionTestUtils.setField(aiController, "dashscopeChatModel", dashscopeChatModel);
        ReflectionTestUtils.setField(aiController, "chatModelMap", Map.of(
                "qwen-plus", configuredQwenPlusModel,
                "deepseek-chat", deepSeekChatModel
        ));
    }

    /**
     * 前端传入默认 qwen-plus 时应复用自动配置的 DashScope 模型。
     */
    @Test
    void resolveChatModelShouldUseAutoConfiguredDashScopeForDefaultQwenPlus() {
        ChatModel model = ReflectionTestUtils.invokeMethod(aiController,
                "resolveChatModel", "qwen-plus");

        assertSame(dashscopeChatModel, model);
    }

    /**
     * 非默认模型仍应从多模型配置中获取。
     */
    @Test
    void resolveChatModelShouldUseConfiguredModelForNonDefaultModel() {
        ChatModel model = ReflectionTestUtils.invokeMethod(aiController,
                "resolveChatModel", "deepseek-chat");

        assertSame(deepSeekChatModel, model);
    }

    /**
     * 模型接口异常应优先展示响应体详情。
     */
    @Test
    void resolveModelErrorMessageShouldUseResponseBody() {
        WebClientResponseException exception = WebClientResponseException.create(
                400,
                "Bad Request",
                HttpHeaders.EMPTY,
                "{\"code\":\"InvalidParameter\",\"message\":\"bad request\"}"
                        .getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8
        );

        String message = ReflectionTestUtils.invokeMethod(aiController,
                "resolveModelErrorMessage", exception);

        assertEquals("{\"code\":\"InvalidParameter\",\"message\":\"bad request\"}",
                message);
    }
}
