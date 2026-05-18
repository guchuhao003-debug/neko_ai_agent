package com.wenxi.neko_ai_agent.app;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 恋爱应用单元测试。
 */
@ExtendWith(MockitoExtension.class)
class LoveAppUnitTest {

    private LoveApp loveApp;

    @Mock
    private ChatModel chatModel;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private VectorStore vectorStore;

    /**
     * 初始化恋爱应用。
     */
    @BeforeEach
    void setUp() {
        loveApp = new LoveApp(chatModel, chatMemory);
    }

    /**
     * 默认关闭 RAG 时基础对话不应触发向量检索。
     */
    @Test
    void shouldUseRagShouldReturnFalseWhenConfigDisabled() {
        ReflectionTestUtils.setField(loveApp, "loveAppRagEnabled", false);
        ReflectionTestUtils.setField(loveApp, "loveAppVectorStore", vectorStore);

        Boolean shouldUseRag = ReflectionTestUtils.invokeMethod(loveApp, "shouldUseRag");

        assertFalse(Boolean.TRUE.equals(shouldUseRag));
    }

    /**
     * 配置开启且向量库存在时才启用 RAG。
     */
    @Test
    void shouldUseRagShouldReturnTrueWhenConfigEnabledAndVectorStoreExists() {
        ReflectionTestUtils.setField(loveApp, "loveAppRagEnabled", true);
        ReflectionTestUtils.setField(loveApp, "loveAppVectorStore", vectorStore);

        Boolean shouldUseRag = ReflectionTestUtils.invokeMethod(loveApp, "shouldUseRag");

        assertTrue(Boolean.TRUE.equals(shouldUseRag));
    }
}
