package com.wenxi.neko_ai_agent.app;

import com.wenxi.neko_ai_agent.advisor.MyLoggerAdvisor;
import com.wenxi.neko_ai_agent.rag.QueryRewriter;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor.TOP_K;

/**
 * AI 恋爱大师应用
 */
@Component
@Slf4j
public class LoveApp extends BaseApp {

    private static final String SYSTEM_PROMPT = "扮演深耕恋爱心理领域的专家。开场向用户表明身份，告知用户可倾诉恋爱难题。" +
            "围绕单身、恋爱、已婚三种状态提问：单身状态询问社交圈拓展及追求心仪对象的困扰；" +
            "恋爱状态询问沟通、习惯差异引发的矛盾；已婚状态询问家庭责任与亲属关系处理的问题。" +
            "引导用户详述事情经过、对方反应及自身想法，以便给出专属解决方案。";

    public LoveApp(ChatModel dashscopeChatModel, ChatMemory chatMemory) {
        super(dashscopeChatModel, chatMemory);
    }

    @Override
    protected String getSystemPrompt() {
        return SYSTEM_PROMPT;
    }

    // ==================== Love 特有的 RAG 依赖 ====================

    @Resource
    private VectorStore loveAppVectorStore;

    @Resource
    private Advisor loveAppRagCloudAdvisor;

    @Resource
    private VectorStore pgVectorStore;

    @Resource
    private QueryRewriter loveAppQueryRewriter;

    @Value("${neko.rag.love-app.enabled:false}")
    private boolean loveAppRagEnabled;

    // ==================== 基础对话 ====================

    /**
     * AI 基础对话（支持多轮对话记忆）
     */
    public String doChat(String message, String chatId) {
        if (!shouldUseRag()) {
            return doChatWithoutRag(message, chatId);
        }
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话（SSE 流式输出）
     */
    public Flux<String> doChatByStream(String message, String chatId) {
        if (!shouldUseRag()) {
            return doChatByStreamWithoutRag(message, chatId);
        }
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                .stream()
                .content();
    }

    /**
     * 指定模型流式对话（委托基类，保持 doChatByStream 命名兼容）
     */
    public Flux<String> doChatByStream(String message, String chatId, ChatModel chatModel) {
        return doChatStream(message, chatId, chatModel);
    }

    /**
     * 普通非 RAG 对话，避免向量检索不可用时影响基础聊天。
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
    private Flux<String> doChatByStreamWithoutRag(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .stream()
                .content();
    }

    /**
     * 判断恋爱应用基础对话是否启用 RAG。
     */
    private boolean shouldUseRag() {
        return loveAppRagEnabled && loveAppVectorStore != null;
    }

    // ==================== 结构化报告 ====================

    record LoveReport(String title, List<String> suggestions) {
    }

    /**
     * AI 恋爱报告功能（结构化输出）
     */
    public LoveReport doChatWithReport(String message, String chatId) {
        LoveReport loveReport = chatClient.prompt()
                .system(SYSTEM_PROMPT + "每次对话后都要生成恋爱结果，标题为{用户名}的恋爱报告，内容为建议列表")
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .call()
                .entity(LoveReport.class);
        log.info("loveReport: {}", loveReport);
        return loveReport;
    }

    // ==================== RAG 知识库对话 ====================

    /**
     * 和 RAG 知识库进行对话
     */
    public String doChatWithRag(String message, String chatId) {
        if (!shouldUseRag()) {
            log.warn("恋爱应用 RAG 未启用，已降级为普通对话。");
            return doChatWithoutRag(message, chatId);
        }
        String rewrittenMessage = loveAppQueryRewriter.doQueryRewrite(message);
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(rewrittenMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .advisors(new MyLoggerAdvisor())
                .advisors(new QuestionAnswerAdvisor(loveAppVectorStore))
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
