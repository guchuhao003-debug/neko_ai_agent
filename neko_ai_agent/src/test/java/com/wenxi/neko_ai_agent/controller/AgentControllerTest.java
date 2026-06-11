package com.wenxi.neko_ai_agent.controller;

import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.model.dto.agent.AgentChatRequest;
import com.wenxi.neko_ai_agent.service.AgentService;
import com.wenxi.neko_ai_agent.service.QuotaService;
import com.wenxi.neko_ai_agent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import reactor.core.publisher.Flux;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 自定义智能体控制器单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AgentControllerTest {

    private AgentController agentController;

    @Mock
    private AgentService agentService;

    @Mock
    private UserService userService;

    @Mock
    private QuotaService quotaService;

    @Mock
    private HttpServletRequest request;

    /**
     * 初始化自定义智能体控制器依赖。
     */
    @BeforeEach
    void setUp() {
        agentController = new AgentController();
        ReflectionTestUtils.setField(agentController, "agentService", agentService);
        ReflectionTestUtils.setField(agentController, "userService", userService);
        ReflectionTestUtils.setField(agentController, "quotaService", quotaService);
    }

    /**
     * 自定义智能体聊天必须使用 POST body，避免长消息放入 URL query。
     */
    @Test
    void chatWithAgentShouldUsePostBodyForLongMessage() throws NoSuchMethodException {
        Method method = AgentController.class.getDeclaredMethod("chatWithAgent",
                AgentChatRequest.class, jakarta.servlet.http.HttpServletRequest.class);

        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        RequestBody requestBody = method.getParameters()[0].getAnnotation(RequestBody.class);

        assertNotNull(postMapping);
        assertNotNull(requestBody);
        assertArrayEquals(new String[]{"/chat/sse"}, postMapping.value());
        assertArrayEquals(new String[]{MediaType.TEXT_EVENT_STREAM_VALUE},
                postMapping.produces());
    }

    /**
     * 聊天请求 DTO 应包含 POST body 所需字段。
     */
    @Test
    void agentChatRequestShouldCarryChatBodyFields() {
        AgentChatRequest request = new AgentChatRequest();
        request.setAgentId(1L);
        request.setChatId("chat-1");
        request.setMessage("hello");
        request.setModelId("deepseek-chat");

        assertEquals(1L, request.getAgentId());
        assertEquals("chat-1", request.getChatId());
        assertEquals("hello", request.getMessage());
        assertEquals("deepseek-chat", request.getModelId());
    }

    /**
     * 自定义智能体对话前必须扣减积分。
     */
    @Test
    void chatWithAgentShouldDeductQuotaBeforeStreaming() {
        User user = new User();
        user.setId(3L);
        AgentChatRequest chatRequest = new AgentChatRequest();
        chatRequest.setAgentId(10L);
        chatRequest.setChatId("chat-3");
        chatRequest.setModelId("qwen-plus");
        chatRequest.setMessage("帮我写一个学习计划");
        when(userService.getLoginUser(request)).thenReturn(user);
        when(agentService.streamChat("3", 10L, "chat-3", "qwen-plus",
                "帮我写一个学习计划")).thenReturn(Flux.just("ok"));

        Flux<String> result = agentController.chatWithAgent(chatRequest, request);

        assertNotNull(result);
        verify(quotaService).deductForChat(3L);
        verify(agentService).streamChat("3", 10L, "chat-3", "qwen-plus",
                "帮我写一个学习计划");
    }
}
