package com.wenxi.neko_ai_agent.controller;

import com.wenxi.neko_ai_agent.annotation.AuthCheck;
import com.wenxi.neko_ai_agent.app.LoveApp;
import com.wenxi.neko_ai_agent.app.PetApp;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.service.QuotaService;
import com.wenxi.neko_ai_agent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.http.HttpHeaders;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private LoveApp loveApp;

    @Mock
    private PetApp petApp;

    @Mock
    private UserService userService;

    @Mock
    private QuotaService quotaService;

    @Mock
    private HttpServletRequest request;

    /**
     * 初始化控制器与模型映射。
     */
    @BeforeEach
    void setUp() {
        aiController = new AiController();
        ReflectionTestUtils.setField(aiController, "dashscopeChatModel", dashscopeChatModel);
        ReflectionTestUtils.setField(aiController, "loveApp", loveApp);
        ReflectionTestUtils.setField(aiController, "petApp", petApp);
        ReflectionTestUtils.setField(aiController, "userService", userService);
        ReflectionTestUtils.setField(aiController, "quotaService", quotaService);
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

    /**
     * 固定恋爱智能体对话前必须扣减积分。
     */
    @Test
    void loveAppChatShouldDeductQuotaBeforeCallingApp() {
        User user = new User();
        user.setId(1L);
        when(userService.getLoginUser(request)).thenReturn(user);
        when(loveApp.doChat("恋爱沟通建议", "chat-1")).thenReturn("ok");

        String result = aiController.doChatWithLoveAppSync("恋爱沟通建议", "chat-1", request);

        assertEquals("ok", result);
        verify(quotaService).deductForChat(1L);
        verify(loveApp).doChat("恋爱沟通建议", "chat-1");
    }

    /**
     * 固定宠物智能体对话前必须扣减积分。
     */
    @Test
    void petAppChatShouldDeductQuotaBeforeCallingApp() {
        User user = new User();
        user.setId(2L);
        PetApp.PetReport report = new PetApp.PetReport("猫咪护理", List.of("保持饮水"));
        when(userService.getLoginUser(request)).thenReturn(user);
        when(petApp.doChatWithReport("猫咪不爱喝水怎么办", "chat-2")).thenReturn(report);

        PetApp.PetReport result = aiController.doChatWithPetAppSync(
                "猫咪不爱喝水怎么办", "chat-2", request);

        assertSame(report, result);
        verify(quotaService).deductForChat(2L);
        verify(petApp).doChatWithReport("猫咪不爱喝水怎么办", "chat-2");
    }

    /**
     * AI 控制器公开端点必须要求登录，避免未认证消耗模型资源。
     */
    @Test
    void publicEndpointsShouldRequireLogin() {
        Method[] methods = AiController.class.getDeclaredMethods();

        Arrays.stream(methods)
                .filter(method -> method.getName().startsWith("doChat")
                        || method.getName().equals("getAvailableModels"))
                .forEach(method -> {
                    AuthCheck authCheck = method.getAnnotation(AuthCheck.class);
                    assertNotNull(authCheck, method.getName() + " 缺少 @AuthCheck");
                    assertEquals("", authCheck.mustRole());
                });
    }

    /**
     * 聊天端点参数必须显式声明 @RequestParam，避免依赖编译参数名推断。
     */
    @Test
    void chatEndpointParametersShouldUseRequestParam() {
        Arrays.stream(AiController.class.getDeclaredMethods())
                .filter(method -> method.getName().startsWith("doChat"))
                .forEach(method -> {
                    for (Parameter parameter : method.getParameters()) {
                        if (parameter.getType().equals(jakarta.servlet.http.HttpServletRequest.class)) {
                            continue;
                        }
                        assertNotNull(parameter.getAnnotation(RequestParam.class),
                                method.getName() + "." + parameter.getName()
                                        + " 缺少 @RequestParam");
                    }
                });
    }
}
