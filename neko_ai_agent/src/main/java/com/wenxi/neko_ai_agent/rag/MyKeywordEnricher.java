package com.wenxi.neko_ai_agent.rag;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.KeywordMetadataEnricher;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 基于 AI 的文档关键词元信息增强器
 */
@Component
public class MyKeywordEnricher {

    @Resource
    private ChatModel dashScopeChatModel;

    public List<Document> enrichDocuments(List<Document> documents) {
        KeywordMetadataEnricher keywordMetadataEnricher = new KeywordMetadataEnricher(dashScopeChatModel, 5);
        return keywordMetadataEnricher.apply(documents);
    }
}
