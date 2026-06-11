package com.wenxi.neko_ai_agent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

import java.io.File;
import java.util.List;

/**
 * 宠物应用向量库配置，使用本地 Markdown 知识库构建 SimpleVectorStore。
 */
@Configuration
@ConditionalOnExpression("'${neko.rag.pet-app.enabled:false}' == 'true' "
        + "&& '${neko.rag.vector-store.type:simple}' == 'simple'")
@Slf4j
public class PetAppVectorStoreConfig {

    @Resource
    private PetAppDocumentLoader petAppDocumentLoader;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Value("${neko.rag.pet-app.vector-store-file:tmp/vector_store/pet_app_vector_store.json}")
    private String vectorStoreFilePath;

    /**
     * 构建宠物应用专属向量库。
     *
     * @param dashscopeEmbeddingModel DashScope 向量模型
     * @return 宠物应用向量库
     */
    @Bean
    @Lazy
    VectorStore petAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel)
                .build();
        File persistFile = new File(vectorStoreFilePath);

        if (persistFile.exists()) {
            log.info("从持久化文件加载宠物知识库向量库：{}", vectorStoreFilePath);
            simpleVectorStore.load(persistFile);
            return simpleVectorStore;
        }

        log.info("宠物知识库向量库不存在，开始从本地 Markdown 文档构建。");
        List<Document> documents = petAppDocumentLoader.loadMarkdowns();
        if (documents.isEmpty()) {
            log.warn("宠物知识库文档为空，将返回空向量库。");
            return simpleVectorStore;
        }
        List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documents);
        simpleVectorStore.add(splitDocuments);
        if (persistFile.getParentFile() != null) {
            persistFile.getParentFile().mkdirs();
        }
        simpleVectorStore.save(persistFile);
        log.info("宠物知识库向量库构建完成，文档片段数：{}，文件：{}",
                splitDocuments.size(), vectorStoreFilePath);
        return simpleVectorStore;
    }
}
