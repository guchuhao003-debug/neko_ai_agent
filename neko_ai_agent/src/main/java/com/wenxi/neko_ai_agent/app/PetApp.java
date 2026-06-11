package com.wenxi.neko_ai_agent.app;

import com.wenxi.neko_ai_agent.constant.PromptConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.vectorstore
        .VectorStoreChatMemoryAdvisor.TOP_K;

/**
 * AI 宠物大师应用
 */
@Component
@Slf4j
public class PetApp extends BaseApp {

    @Autowired(required = false)
    @Qualifier("petAppVectorStore")
    @Lazy
    private VectorStore petAppVectorStore;

    @Value("${neko.rag.pet-app.enabled:false}")
    private boolean petAppRagEnabled;

    public PetApp(ChatModel dashscopeChatModel, ChatMemory chatMemory) {
        super(dashscopeChatModel, chatMemory);
    }

    @Override
    protected String getSystemPrompt() {
        return PromptConstant.PET_SYSTEM_PROMPT;
    }

    /**
     * AI 基础对话（支持多轮对话记忆）
     */
    public String doChat(String message, String chatId) {
        if (!shouldUseRag()) {
            return doChatWithoutRag(message, chatId);
        }
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .advisors(new QuestionAnswerAdvisor(petAppVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（SSE 流式输出）
     */
    public Flux<String> doChatStream(String message, String chatId) {
        if (!shouldUseRag()) {
            return doChatStreamWithoutRag(message, chatId);
        }
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .advisors(new QuestionAnswerAdvisor(petAppVectorStore))
                .stream()
                .content();
    }

    /**
     * 指定模型的宠物应用流式对话，同样复用本地知识库 RAG。
     */
    @Override
    public Flux<String> doChatStream(String message, String chatId, ChatModel chatModel) {
        if (!shouldUseRag()) {
            return super.doChatStream(message, chatId, chatModel);
        }
        ChatClient dynamicClient = buildChatClient(chatModel, getSystemPrompt(), chatMemory);
        return dynamicClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .advisors(new QuestionAnswerAdvisor(petAppVectorStore))
                .stream()
                .content();
    }

    /**
     * 普通非 RAG 对话，用于 RAG 关闭或向量库不可用时降级。
     */
    private String doChatWithoutRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 普通非 RAG 流式对话。
     */
    private Flux<String> doChatStreamWithoutRag(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .stream()
                .content();
    }

    /**
     * 判断宠物应用是否启用本地知识库 RAG。
     */
    private boolean shouldUseRag() {
        return petAppRagEnabled && petAppVectorStore != null;
    }

    // ==================== 结构化报告 ====================

    public record PetReport(String title, List<String> suggestions) {
    }

    /**
     * AI 养宠详细报告功能（结构化输出）
     */
    public PetReport doChatWithReport(String message, String chatId) {
        String reportPrompt = PromptConstant.PET_SYSTEM_PROMPT
                + "每次对话后都要生成养宠详细报告，标题为 {用户名} 的养宠详情报告，内容为建议列表";
        ChatClient.ChatClientRequestSpec requestSpec = chatClient.prompt()
                .system(reportPrompt)
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20));
        if (shouldUseRag()) {
            requestSpec.advisors(new QuestionAnswerAdvisor(petAppVectorStore));
        }
        PetReport petReport = requestSpec.call()
                .entity(PetReport.class);
        log.info("petReport: {}", petReport);
        return petReport;
    }
}
