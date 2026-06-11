package com.wenxi.neko_ai_agent.service;

import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.wenxi.neko_ai_agent.mapper.ChatHistoryMapper;
import com.wenxi.neko_ai_agent.model.dto.chatmemory.ChatHistoryDetailDTO;
import com.wenxi.neko_ai_agent.model.entity.ChatHistory;
import com.wenxi.neko_ai_agent.service.impl.ChatHistoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 对话历史服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class ChatHistoryServiceImplTest {

    private ChatHistoryServiceImpl chatHistoryService;

    @Mock
    private ChatHistoryMapper chatHistoryMapper;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    /**
     * 初始化被测服务。
     */
    @BeforeEach
    void setUp() {
        chatHistoryService = new ChatHistoryServiceImpl();
        ReflectionTestUtils.setField(chatHistoryService, "chatHistoryMapper", chatHistoryMapper);
        ReflectionTestUtils.setField(chatHistoryService, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(chatHistoryService, "cacheTtlMinutes", 120L);
    }

    /**
     * 更新已有会话时应同步刷新 lastMessage。
     */
    @Test
    void saveChatMessagesShouldUpdateLastMessageForExistingRecord() {
        ChatHistory existing = new ChatHistory();
        existing.setId(1L);
        when(chatHistoryMapper.selectOne(any())).thenReturn(existing);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        ChatHistoryDetailDTO.ChatMessage first = new ChatHistoryDetailDTO.ChatMessage();
        first.setRole("user");
        first.setContent("第一条问题");
        ChatHistoryDetailDTO.ChatMessage latest = new ChatHistoryDetailDTO.ChatMessage();
        latest.setRole("user");
        latest.setContent("最新一条问题");

        chatHistoryService.saveChatMessages(1L, "chat-1", "love",
                List.of(first, latest));

        ArgumentCaptor<UpdateWrapper<ChatHistory>> captor = ArgumentCaptor.forClass(
                UpdateWrapper.class);
        verify(chatHistoryMapper).update(eq(null), captor.capture());
        assertTrue(captor.getValue().getSqlSet().contains("lastMessage"));
    }

    /**
     * 删除会话应走逻辑删除，并且 Redis 失败不能影响主流程。
     */
    @Test
    void deleteChatHistoryShouldUseLogicalDeleteAndIgnoreRedisFailure() {
        when(chatHistoryMapper.update(eq(null), any())).thenReturn(1);
        when(stringRedisTemplate.delete(any(String.class))).thenThrow(new RuntimeException("redis down"));

        boolean deleted = assertDoesNotThrow(() ->
                chatHistoryService.deleteChatHistory(1L, "chat-1", "love"));

        ArgumentCaptor<UpdateWrapper<ChatHistory>> captor = ArgumentCaptor.forClass(
                UpdateWrapper.class);
        verify(chatHistoryMapper).update(eq(null), captor.capture());
        assertTrue(deleted);
        assertTrue(captor.getValue().getSqlSet().contains("isDelete"));
    }
}
