package com.wenxi.neko_ai_agent.rag;

import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * 宠物知识库文档加载器测试。
 */
class PetAppDocumentLoaderTest {

    /**
     * 应能从项目本地 resource/documents/pet_app 目录加载 Markdown 文档。
     */
    @Test
    void loadMarkdownsShouldReadLocalPetDocuments() {
        PetAppDocumentLoader loader = new PetAppDocumentLoader(
                new PathMatchingResourcePatternResolver());
        ReflectionTestUtils.setField(loader, "documentPattern",
                "file:resource/documents/pet_app/*.md");

        List<Document> documents = loader.loadMarkdowns();

        assertFalse(documents.isEmpty());
    }
}
