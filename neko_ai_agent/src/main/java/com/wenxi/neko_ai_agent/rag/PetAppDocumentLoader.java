package com.wenxi.neko_ai_agent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.markdown.MarkdownDocumentReader;
import org.springframework.ai.reader.markdown.config.MarkdownDocumentReaderConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 宠物应用本地知识库 Markdown 文档加载器。
 */
@Component
@Slf4j
public class PetAppDocumentLoader {

    private final ResourcePatternResolver resourcePatternResolver;

    @Value("${neko.rag.pet-app.document-pattern:file:resource/documents/pet_app/*.md}")
    private String documentPattern;

    /**
     * 创建宠物知识库文档加载器。
     *
     * @param resourcePatternResolver Spring 资源解析器
     */
    public PetAppDocumentLoader(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    /**
     * 加载宠物应用本地 Markdown 知识库。
     *
     * @return 文档列表
     */
    public List<Document> loadMarkdowns() {
        List<Document> documents = loadByPattern(documentPattern);
        if (!documents.isEmpty()) {
            return documents;
        }
        log.warn("宠物知识库路径 {} 未加载到文档，尝试读取 classpath 备用路径。", documentPattern);
        return loadByPattern("classpath:document/pet_app/*.md");
    }

    /**
     * 按资源表达式加载 Markdown 文档。
     *
     * @param pattern 资源表达式
     * @return 文档列表
     */
    private List<Document> loadByPattern(String pattern) {
        List<Document> allDocuments = new ArrayList<>();
        try {
            Resource[] resources = resourcePatternResolver.getResources(pattern);
            for (Resource resource : resources) {
                if (!resource.exists()) {
                    continue;
                }
                String filename = resource.getFilename();
                MarkdownDocumentReaderConfig config = MarkdownDocumentReaderConfig.builder()
                        .withHorizontalRuleCreateDocument(true)
                        .withIncludeCodeBlock(false)
                        .withIncludeBlockquote(false)
                        .withAdditionalMetadata("filename", filename)
                        .withAdditionalMetadata("species", resolveSpecies(filename))
                        .build();
                MarkdownDocumentReader markdownDocumentReader =
                        new MarkdownDocumentReader(resource, config);
                allDocuments.addAll(markdownDocumentReader.get());
            }
            log.info("宠物知识库加载完成，路径：{}，文档片段数：{}", pattern, allDocuments.size());
        } catch (IOException e) {
            log.error("宠物知识库 Markdown 文档加载失败，路径：{}", pattern, e);
        }
        return allDocuments;
    }

    /**
     * 根据文件名提取宠物类别元信息。
     *
     * @param filename 文件名
     * @return 宠物类别
     */
    private String resolveSpecies(String filename) {
        if (filename == null) {
            return "unknown";
        }
        if (filename.contains("猫")) {
            return "cat";
        }
        if (filename.contains("狗")) {
            return "dog";
        }
        return "general";
    }
}
