package com.wenxi.neko_ai_agent.app;

import com.wenxi.neko_ai_agent.advisor.MyLoggerAdvisor;
import com.wenxi.neko_ai_agent.constant.PromptConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.springframework.ai.chat.client.advisor.vectorstore.VectorStoreChatMemoryAdvisor.TOP_K;

/**
 * AI 宠物大师应用
 */
@Component
@Slf4j
public class PetApp {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    // AI 配置 MCP
    @Resource
    private ToolCallbackProvider toolCallbackProvider;


    public PetApp(ChatModel dashscopeChatModel, ChatMemory chatMemory) {
        this.chatMemory = chatMemory;
        // 初始化基于文件的对话记忆
//        String fileDir = System.getProperty("user.dir") + "/tmp/chat_memory";
//        ChatMemory chatMemory = new FileBasedChatMemory(fileDir);
        // 基于内存存储
//        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
//                .chatMemoryRepository(new InMemoryChatMemoryRepository())
//                .maxMessages(20)
//                .build();
        chatClient = ChatClient.builder(dashscopeChatModel)
                .defaultSystem(PromptConstant.PET_SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        // 自定义日志 Advisor, 可按需开启
                        new MyLoggerAdvisor()
                        )
                .build();
    }

    /**
     *  AI 基础对话 （支持多轮对话记忆）
     * @param message
     * @param chatId
     * @return
     */
    public String doChat(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                // 支持 MCP 配置
                .toolCallbacks(toolCallbackProvider)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 基础对话 （支持多轮对话记忆， SSE 流式输出）
     * @param message
     * @param chatId
     * @return
     */
    public Flux<String> doChatStream(String message, String chatId) {
        return chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID,chatId)
                        .param(TOP_K,20))
                // 支持 MCP 配置
                .toolCallbacks(toolCallbackProvider)
                .stream()
                .content();
    }

    /**
     * AI 基础对话（指定模型，SSE 流式输出）
     */
    public Flux<String> doChatStream(String message, String chatId, ChatModel chatModel) {
        ChatClient dynamicClient = ChatClient.builder(chatModel)
                .defaultSystem(PromptConstant.PET_SYSTEM_PROMPT)
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        new MyLoggerAdvisor()
                )
                .build();
        return dynamicClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                .stream()
                .content();
    }

    public record PetReport(String title, List<String> suggestions) {}

    /**
     * AI 养宠详细报告功能 （结构化输出）
     * @param message
     * @param chatId
     * @return
     */
    public PetReport doChatWithReport(String message, String chatId){
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


    /**
     * AI 养宠详细报告（调用 AI 工具）
     * @param message
     * @param chatId
     * @return
     */
    public String doChatWithTools(String message, String chatId) {
        ChatResponse chatResponse = chatClient.prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId)
                        .param(TOP_K, 20))
                // 开启日志，便于观察效果
                .advisors(new MyLoggerAdvisor())
                // 调用 AI 工具
                .toolCallbacks(allTools)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }

    /**
     * AI 对话（基于配置 MCP ）
     * @param message
     * @param chatId
     * @return
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
}
