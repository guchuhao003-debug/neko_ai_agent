package com.wenxi.neko_ai_agent.service;

import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.mapper.AgentMapper;
import com.wenxi.neko_ai_agent.model.dto.agent.AgentCreateRequest;
import com.wenxi.neko_ai_agent.model.dto.agent.AgentUpdateRequest;
import com.wenxi.neko_ai_agent.model.entity.Agent;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.service.impl.AgentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 智能体服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class AgentServiceImplTest {

    private AgentServiceImpl agentService;

    @Mock
    private AgentMapper agentMapper;

    @Mock
    private UserService userService;

    @Mock
    private ChatModel chatModel;

    /**
     * 初始化被测服务。
     */
    @BeforeEach
    void setUp() {
        agentService = new AgentServiceImpl();
        ReflectionTestUtils.setField(agentService, "baseMapper", agentMapper);
        ReflectionTestUtils.setField(agentService, "userService", userService);
        ReflectionTestUtils.setField(agentService, "chatModelMap", Map.of("qwen-plus", chatModel));
    }

    /**
     * 创建智能体时应填充默认参数。
     */
    @Test
    void createAgentShouldFillDefaultOptions() {
        when(agentMapper.insert(any(Agent.class))).thenAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            agent.setId(1L);
            return 1;
        });

        AgentCreateRequest request = new AgentCreateRequest();
        request.setName("代码助手");
        request.setSystemPrompt("你是一个严谨的代码助手，请给出清晰的建议。");
        request.setModelId("qwen-plus");

        Agent agent = agentService.createAgent("1001", request);

        assertEquals(1L, agent.getId());
        assertEquals("1001", agent.getUserId());
        assertEquals(BigDecimal.valueOf(0.7), agent.getTemperature());
        assertEquals(2048, agent.getMaxTokens());
        assertFalse(agent.getIsPublic());
        assertTrue(agent.getStatus());
        assertEquals(0, agent.getUseCount());
    }

    /**
     * 非创建者且非管理员不能更新智能体。
     */
    @Test
    void updateAgentShouldRejectUserWithoutManageAuth() {
        Agent oldAgent = new Agent();
        oldAgent.setId(1L);
        oldAgent.setUserId("1001");
        when(agentMapper.selectById(1L)).thenReturn(oldAgent);

        User normalUser = new User();
        normalUser.setId(1002L);
        normalUser.setUserRole("user");
        when(userService.getById("1002")).thenReturn(normalUser);
        when(userService.isAdmin(normalUser)).thenReturn(false);

        AgentUpdateRequest request = new AgentUpdateRequest();
        request.setId(1L);
        request.setName("新名称");

        assertThrows(BusinessException.class, () -> agentService.updateAgent("1002", request));
    }

    /**
     * 聊天记忆会话 ID 应稳定且不超过数据库字段长度。
     */
    @Test
    void buildConversationIdShouldFitChatMemoryColumn() {
        String longChatId = "AGENT-1-1778912220123-client-session-extra-long-value";

        String conversationId = ReflectionTestUtils.invokeMethod(agentService,
                "buildConversationId", "1001", 1L, longChatId);
        String sameConversationId = ReflectionTestUtils.invokeMethod(agentService,
                "buildConversationId", "1001", 1L, longChatId);
        String anotherConversationId = ReflectionTestUtils.invokeMethod(agentService,
                "buildConversationId", "1001", 1L, longChatId + "-next");

        assertNotNull(conversationId);
        assertTrue(conversationId.length() <= 36);
        assertEquals(conversationId, sameConversationId);
        assertNotEquals(conversationId, anotherConversationId);
    }
}
