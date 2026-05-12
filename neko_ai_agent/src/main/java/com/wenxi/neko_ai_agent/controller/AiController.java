package com.wenxi.neko_ai_agent.controller;

import com.wenxi.neko_ai_agent.agent.NekoManus;
import com.wenxi.neko_ai_agent.app.LoveApp;
import com.wenxi.neko_ai_agent.app.PetApp;
import com.wenxi.neko_ai_agent.config.MultiModelProperties;
import com.wenxi.neko_ai_agent.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private PetApp petApp;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private ToolCallback[] allTools;

    @Resource
    private ToolCallbackProvider toolCallbackProvider;

    @Resource
    private UserService userService;

    @Resource
    private Map<String, ChatModel> chatModelMap;

    @Resource
    private MultiModelProperties multiModelProperties;

    /**
     * 获取可用模型列表
     */
    @GetMapping("/models")
    public List<Map<String, String>> getAvailableModels() {
        List<Map<String, String>> result = new ArrayList<>();
        for (MultiModelProperties.ModelConfig config : multiModelProperties.getModels()) {
            Map<String, String> item = new HashMap<>();
            item.put("id", config.getId());
            item.put("name", config.getName());
            result.add(item);
        }
        return result;
    }

    /**
     * 根据 modelId 获取对应的 ChatModel，不存在则返回默认模型
     */
    private ChatModel resolveChatModel(String modelId) {
        if (modelId == null || modelId.isBlank()) {
            return dashscopeChatModel;
        }
        return chatModelMap.getOrDefault(modelId, dashscopeChatModel);
    }


    /**
     * 同步调用 AI 恋爱大师应用
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/love_app/chat/sync")
    public String doChatWithLoveAppSync(String message, String chatId) {
        return loveApp.doChat(message,chatId);
    }

    /**
     * SSE 流式调用 AI 恋爱大师应用 （第一种方式，需要添加响应头 ：produces = MediaType.TEXT_EVENT_STREAM_VALUE ）
     * @param message
     * @param chatId
     * @param modelId
     * @return
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSEOld(String message, String chatId,
                                                @RequestParam(required = false) String modelId) {
        ChatModel model = resolveChatModel(modelId);
        Flux<String> flux;
        if (model == dashscopeChatModel) {
            flux = loveApp.doChatByStream(message, chatId);
        } else {
            flux = loveApp.doChatByStream(message, chatId, model);
        }
        return flux.onErrorResume(e -> {
            log.error("SSE streaming error with model [{}]: {}", modelId, e.getMessage(), e);
            return Flux.just("[模型调用异常] " + e.getMessage());
        });
    }



    /**
     * SSE 流式调用 AI 恋爱大师应用 （第二种方式： ServerSentEvent）
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/love_app/chat/server_sent_event", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(String message, String chatId) {
        return loveApp.doChatByStream(message, chatId)
                .map(chunk -> ServerSentEvent.<String>builder()
                        .data(chunk)
                        .build());
    }

    /**
     * SSE 流式调用 AI 恋爱大师应用 （第三种方式： SseEmitter）
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/love_app/chat/sse_emitter", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter doChatWithLoveAppSseEmitter(String message, String chatId) {
        // 创建一个超时时间较长的 SseEmitter
        SseEmitter sseEmitter = new SseEmitter(180000L); // 3分钟超时
        // 获取 Flux 响应式数据流并且直接通过订阅推送给 SseEmitter
        loveApp.doChatByStream(message,chatId)
                .subscribe(chunk -> {
                    try {
                        sseEmitter.send(chunk);
                    } catch (IOException e) {
                        sseEmitter.completeWithError(e);
                    }
                }, sseEmitter::completeWithError, sseEmitter::complete);
        return sseEmitter;
    }


    /**
     * 同步调用 AI 养宠大师应用 （输出养宠详细报告）
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping("/pet_app/chat/sync")
    public PetApp.PetReport doChatWithPetAppSync(String message, String chatId) {
        return petApp.doChatWithReport(message, chatId);
    }

    /**
     * 异步调用 养宠大师应用 （基础对话  + SSE）
     * @param message
     * @param chatId
     * @param modelId
     * @return
     */
    @GetMapping(value = "/pet_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithPetAppSSE(String message, String chatId,
                                            @RequestParam(required = false) String modelId) {
        ChatModel model = resolveChatModel(modelId);
        Flux<String> flux;
        if (model == dashscopeChatModel) {
            flux = petApp.doChatStream(message, chatId);
        } else {
            flux = petApp.doChatStream(message, chatId, model);
        }
        return flux.onErrorResume(e -> {
            log.error("SSE streaming error with model [{}]: {}", modelId, e.getMessage(), e);
            return Flux.just("[模型调用异常] " + e.getMessage());
        });
    }

    /**
     * 流式调用 Manus AI Agent
     * @param message
     * @param modelId
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManusOld(String message, String chatId,
                                         @RequestParam(required = false) String modelId) {
        // 合并本地工具和 MCP 工具
        List<ToolCallback> combinedTools = new ArrayList<>(Arrays.asList(allTools));
        ToolCallback[] mcpTools = (ToolCallback[]) toolCallbackProvider.getToolCallbacks();
        combinedTools.addAll(Arrays.asList(mcpTools));
        ToolCallback[] allCombinedTools = combinedTools.toArray(new ToolCallback[0]);
        // 根据 modelId 选择模型
        ChatModel model = resolveChatModel(modelId);
        // 单例，每次调用都需要重新创建一个实例，如果通过注入方式，那么用户调用的都是同一个实例，则容易造成阻塞
        NekoManus nekoManus = new NekoManus(allCombinedTools, model);
        return nekoManus.runStream(message);
    }



}
