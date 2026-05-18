package com.wenxi.neko_ai_agent.app;

import com.wenxi.neko_ai_agent.advisor.MyLoggerAdvisor;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import reactor.core.publisher.Flux;

import static org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor.TOP_K;

/**
 * AI 应用基类 — 封装 ChatClient 初始化、工具调用、MCP 集成等通用逻辑。
 * 子类只需提供系统提示词和应用特定的对话增强（如 RAG Advisor）。
 */
@Slf4j
public abstract class BaseApp {

    protected final ChatClient chatClient;
    protected final ChatMemory chatMemory;

    @Resource
    protected ToolCallback[] allTools;

    @Resource
    protected ToolCallbackProvider toolCallbackProvider;

    protected BaseApp(ChatModel chatModel, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        this.chatClient = buildChatClient(chatModel, getSystemPrompt(), chatMemory);
    }

    /**
     * 子类提供系统提示词。
     */
    protected abstract String getSystemPrompt();

    /**
     * 构建统一配置的 ChatClient（MessageChatMemoryAdvisor + MyLoggerAdvisor）。
     * 用于消除多模型切换时的重复构建逻辑。
     */
    public static ChatClient buildChatClient(ChatModel model, String systemPrompt, ChatMemory chatMemory) {
        return ChatClient.builder(model)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
    }

    // ==================== 通用对话方法 ====================

    /**
     * 调用工具对话。
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .advisors(new MyLoggerAdvisor())
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        return chatResponse.getResult().getOutput().getText();
    }

    /**
     * MCP 工具对话。
     */
    public String doChatWithMcp(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * 指定模型流式对话（运行时动态 ChatClient）。
     */
    public Flux<String> doChatStream(String message, String chatId, ChatModel chatModel) {
        ChatClient dynamicClient = buildChatClient(chatModel, getSystemPrompt(), chatMemory);
        return dynamicClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .stream()
                .content();
    }
}
