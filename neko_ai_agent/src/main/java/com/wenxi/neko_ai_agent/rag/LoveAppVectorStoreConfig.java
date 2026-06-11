package com.wenxi.neko_ai_agent.rag;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.io.File;
import java.util.List;

/**
 * 恋爱大师向量数据库配置（基于文件的 SimpleVectorStore，重启秒级加载）。
 * <p>
 * 首次启动：加载文档 → 分词 → 关键词提取 → 写入向量库 → 持久化到文件。
 * 后续启动：直接从持久化文件加载，无需重复 embedding。
 * 删除持久化文件即可在下次启动时强制重建。
 */
@Configuration
@ConditionalOnProperty(prefix = "neko.rag.vector-store", name = "type",
        havingValue = "simple", matchIfMissing = true)
@Slf4j
public class LoveAppVectorStoreConfig {

    @Resource
    private LoveAppDocumentLoader loveAppDocumentLoader;

    @Resource
    private MyKeywordEnricher myKeywordEnricher;

    @Resource
    private MyTokenTextSplitter myTokenTextSplitter;

    @Value("${neko.rag.vector-store-file:tmp/vector_store/love_app_vector_store.json}")
    private String vectorStoreFilePath;

    @Primary
    @Bean
    VectorStore loveAppVectorStore(EmbeddingModel dashscopeEmbeddingModel) {
        SimpleVectorStore simpleVectorStore = SimpleVectorStore.builder(dashscopeEmbeddingModel).build();
        File persistFile = new File(vectorStoreFilePath);

        if (persistFile.exists()) {
            log.info("从持久化文件加载向量库 ({}), 跳过文档加载和分词。删除该文件可强制重建。", vectorStoreFilePath);
            simpleVectorStore.load(persistFile);
        } else {
            log.info("向量库持久化文件不存在，开始从文档构建...");
            List<Document> documentList = loveAppDocumentLoader.loadMarkdowns();
            List<Document> splitDocuments = myTokenTextSplitter.splitCustomized(documentList);
            List<Document> enrichDocuments = myKeywordEnricher.enrichDocuments(splitDocuments);
            simpleVectorStore.add(enrichDocuments);
            // 持久化到文件，避免下次重启重复计算
            persistFile.getParentFile().mkdirs();
            simpleVectorStore.save(persistFile);
            log.info("向量库构建并持久化完成 ({} 篇文档) → {}", enrichDocuments.size(), vectorStoreFilePath);
        }

        return simpleVectorStore;
    }
}
