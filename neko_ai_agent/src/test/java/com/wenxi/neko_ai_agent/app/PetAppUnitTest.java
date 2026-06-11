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
 * 宠物应用单元测试。
 */
@ExtendWith(MockitoExtension.class)
class PetAppUnitTest {

    private PetApp petApp;

    @Mock
    private ChatModel chatModel;

    @Mock
    private ChatMemory chatMemory;

    @Mock
    private VectorStore vectorStore;

    /**
     * 初始化宠物应用。
     */
    @BeforeEach
    void setUp() {
        petApp = new PetApp(chatModel, chatMemory);
    }

    /**
     * 配置关闭时不应启用本地知识库 RAG。
     */
    @Test
    void shouldUseRagShouldReturnFalseWhenConfigDisabled() {
        ReflectionTestUtils.setField(petApp, "petAppRagEnabled", false);
        ReflectionTestUtils.setField(petApp, "petAppVectorStore", vectorStore);

        Boolean shouldUseRag = ReflectionTestUtils.invokeMethod(petApp, "shouldUseRag");

        assertFalse(Boolean.TRUE.equals(shouldUseRag));
    }

    /**
     * 配置开启且向量库存在时应启用本地知识库 RAG。
     */
    @Test
    void shouldUseRagShouldReturnTrueWhenConfigEnabledAndVectorStoreExists() {
        ReflectionTestUtils.setField(petApp, "petAppRagEnabled", true);
        ReflectionTestUtils.setField(petApp, "petAppVectorStore", vectorStore);

        Boolean shouldUseRag = ReflectionTestUtils.invokeMethod(petApp, "shouldUseRag");

        assertTrue(Boolean.TRUE.equals(shouldUseRag));
    }

    /**
     * 配置开启但向量库未创建时，应自动降级为普通对话。
     */
    @Test
    void shouldUseRagShouldReturnFalseWhenVectorStoreMissing() {
        ReflectionTestUtils.setField(petApp, "petAppRagEnabled", true);
        ReflectionTestUtils.setField(petApp, "petAppVectorStore", null);

        Boolean shouldUseRag = ReflectionTestUtils.invokeMethod(petApp, "shouldUseRag");

        assertFalse(Boolean.TRUE.equals(shouldUseRag));
    }
}
