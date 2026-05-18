package com.wenxi.neko_ai_agent.app;

import com.wenxi.neko_ai_agent.constant.PromptConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor.TOP_K;

/**
 * AI 宠物大师应用
 */
@Component
@Slf4j
public class PetApp extends BaseApp {

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
     * AI 基础对话（SSE 流式输出）
     */
    public Flux<String> doChatStream(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .stream()
                .content();
    }

    // ==================== 结构化报告 ====================

    public record PetReport(String title, List<String> suggestions) {
    }

    /**
     * AI 养宠详细报告功能（结构化输出）
     */
    public PetReport doChatWithReport(String message, String chatId) {
        PetReport petReport = chatClient.prompt()
                .system(PromptConstant.PET_SYSTEM_PROMPT + "每次对话后都要生成养宠详细报告，标题为 {用户名} 的养宠详情报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .call()
                .entity(PetReport.class);
        log.info("petReport: {}", petReport);
        return petReport;
    }
}
