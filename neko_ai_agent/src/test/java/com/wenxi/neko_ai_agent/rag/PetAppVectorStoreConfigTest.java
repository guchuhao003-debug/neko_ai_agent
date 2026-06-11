package com.wenxi.neko_ai_agent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 宠物应用向量库配置测试。
 */
class PetAppVectorStoreConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(PetAppVectorStoreConfig.class);

    /**
     * 配置关闭时不创建宠物向量库，避免启动阶段切分文档并调用 Embedding。
     */
    @Test
    void shouldNotCreatePetAppVectorStoreWhenRagDisabled() {
        contextRunner
                .withPropertyValues("neko.rag.pet-app.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean("petAppVectorStore"));
    }

    /**
     * 未显式配置时保持关闭，避免新环境首次启动就触发向量库构建。
     */
    @Test
    void shouldNotCreatePetAppVectorStoreWhenRagPropertyMissing() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean("petAppVectorStore"));
    }

    /**
     * 配置开启时仅注册懒加载 Bean，不应在启动阶段读取文档或调用 Embedding。
     */
    @Test
    void shouldRegisterLazyPetAppVectorStoreWhenRagEnabled() {
        PetAppDocumentLoader documentLoader = mock(PetAppDocumentLoader.class);
        MyTokenTextSplitter tokenTextSplitter = mock(MyTokenTextSplitter.class);
        EmbeddingModel embeddingModel = mock(EmbeddingModel.class);

        new ApplicationContextRunner()
                .withUserConfiguration(PetAppVectorStoreConfig.class)
                .withBean(PetAppDocumentLoader.class, () -> documentLoader)
                .withBean(MyTokenTextSplitter.class, () -> tokenTextSplitter)
                .withBean(EmbeddingModel.class, () -> embeddingModel)
                .withPropertyValues("neko.rag.pet-app.enabled=true")
                .run(context -> {
                    assertThat(context.getSourceApplicationContext()
                            .containsBeanDefinition("petAppVectorStore")).isTrue();
                    verifyNoInteractions(documentLoader, tokenTextSplitter, embeddingModel);
                });
    }

    /**
     * 切换到 PGVector 时，本地文件向量库配置不应再注册同名 Bean。
     */
    @Test
    void shouldNotRegisterSimpleVectorStoreWhenPgVectorEnabled() {
        contextRunner
                .withPropertyValues(
                        "neko.rag.pet-app.enabled=true",
                        "neko.rag.vector-store.type=pgvector")
                .run(context -> assertThat(context).doesNotHaveBean("petAppVectorStore"));
    }
}
