package com.wenxi.neko_ai_agent.controller;

import cn.hutool.core.util.StrUtil;
import com.wenxi.neko_ai_agent.agent.NekoManus;
import com.wenxi.neko_ai_agent.agent.NekoManusFactory;
import com.wenxi.neko_ai_agent.annotation.AuthCheck;
import com.wenxi.neko_ai_agent.app.LoveApp;
import com.wenxi.neko_ai_agent.app.PetApp;
import com.wenxi.neko_ai_agent.config.MultiModelProperties;
import com.wenxi.neko_ai_agent.model.dto.chatmemory.ChatHistoryDetailDTO;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.service.ChatHistoryService;
import com.wenxi.neko_ai_agent.service.QuotaService;
import com.wenxi.neko_ai_agent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ai")
public class AiController {

    private static final String DEFAULT_DASHSCOPE_MODEL_ID = "qwen-plus";

    private static final String MANUS_APP_TYPE = "manus";

    @Resource
    private LoveApp loveApp;

    @Resource
    private PetApp petApp;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private NekoManusFactory nekoManusFactory;

    @Resource
    private UserService userService;

    @Resource
    private ChatHistoryService chatHistoryService;

    @Resource
    private QuotaService quotaService;

    @Resource
    private Map<String, ChatModel> chatModelMap;

    @Resource
    private MultiModelProperties multiModelProperties;

    /**
     * 获取可用模型列表
     */
    @GetMapping("/models")
    @AuthCheck
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
        // 默认 DashScope 模型统一使用自动配置 Bean，避免手动实例与 starter 配置不一致。
        if (modelId == null || modelId.isBlank() || DEFAULT_DASHSCOPE_MODEL_ID.equals(modelId)) {
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
    @AuthCheck
    public String doChatWithLoveAppSync(@RequestParam String message,
                                        @RequestParam String chatId,
                                        HttpServletRequest request) {
        chargeChatQuota(request);
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
    @AuthCheck
    public Flux<String> doChatWithLoveAppSSE(@RequestParam String message,
                                             @RequestParam String chatId,
                                             @RequestParam(required = false) String modelId,
                                             HttpServletRequest request) {
        chargeChatQuota(request);
        ChatModel model = resolveChatModel(modelId);
        Flux<String> flux;
        if (model == dashscopeChatModel) {
            flux = loveApp.doChatByStream(message, chatId);
        } else {
            flux = loveApp.doChatByStream(message, chatId, model);
        }
        return flux.onErrorResume(e -> {
            String errorMessage = resolveModelErrorMessage(e);
            log.error("SSE streaming error with model [{}]: {}", modelId, errorMessage, e);
            return Flux.just("[模型调用异常] " + errorMessage);
        });
    }



    /**
     * SSE 流式调用 AI 恋爱大师应用 （第二种方式： ServerSentEvent）
     * @param message
     * @param chatId
     * @return
     */
    @GetMapping(value = "/love_app/chat/server_sent_event",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @AuthCheck
    public Flux<ServerSentEvent<String>> doChatWithLoveAppServerSentEvent(
            @RequestParam String message,
            @RequestParam String chatId,
            HttpServletRequest request) {
        chargeChatQuota(request);
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
    @AuthCheck
    public SseEmitter doChatWithLoveAppSseEmitter(@RequestParam String message,
                                                  @RequestParam String chatId,
                                                  HttpServletRequest request) {
        chargeChatQuota(request);
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
    @AuthCheck
    public PetApp.PetReport doChatWithPetAppSync(@RequestParam String message,
                                                 @RequestParam String chatId,
                                                 HttpServletRequest request) {
        chargeChatQuota(request);
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
    @AuthCheck
    public Flux<String> doChatWithPetAppSSE(@RequestParam String message,
                                            @RequestParam String chatId,
                                            @RequestParam(required = false) String modelId,
                                            HttpServletRequest request) {
        chargeChatQuota(request);
        ChatModel model = resolveChatModel(modelId);
        Flux<String> flux;
        if (model == dashscopeChatModel) {
            flux = petApp.doChatStream(message, chatId);
        } else {
            flux = petApp.doChatStream(message, chatId, model);
        }
        return flux.onErrorResume(e -> {
            String errorMessage = resolveModelErrorMessage(e);
            log.error("SSE streaming error with model [{}]: {}", modelId, errorMessage, e);
            return Flux.just("[模型调用异常] " + errorMessage);
        });
    }

    /**
     * 提取模型接口异常详情。
     */
    private String resolveModelErrorMessage(Throwable e) {
        if (e instanceof WebClientResponseException webClientException) {
            String responseBody = webClientException.getResponseBodyAsString();
            if (StrUtil.isNotBlank(responseBody)) {
                return responseBody;
            }
        }
        return e.getMessage();
    }

    /**
     * 流式调用 Manus AI Agent
     * @param message
     * @param modelId
     * @return
     */
    @GetMapping("/manus/chat")
    @AuthCheck
    public SseEmitter doChatWithManus(@RequestParam String message,
                                      @RequestParam(required = false) String chatId,
                                      @RequestParam(required = false) String modelId,
                                      HttpServletRequest request) {
        chargeChatQuota(request);
        ChatModel model = resolveChatModel(modelId);
        NekoManus nekoManus = nekoManusFactory.create(model);
        seedManusHistory(nekoManus, chatId, request);
        return nekoManus.runStream(message);
    }

    /**
     * 智能体对话前扣减用户积分。
     *
     * @param request HTTP 请求
     */
    private void chargeChatQuota(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        quotaService.deductForChat(loginUser.getId());
    }

    /**
     * 为 Manus 注入同一会话的历史消息，避免后续任务丢失上文已生成文件信息。
     *
     * @param nekoManus Manus 智能体实例
     * @param chatId 会话 ID
     * @param request HTTP 请求
     */
    private void seedManusHistory(NekoManus nekoManus, String chatId, HttpServletRequest request) {
        if (StrUtil.isBlank(chatId)) {
            return;
        }
        try {
            User loginUser = userService.getLoginUser(request);
            ChatHistoryDetailDTO history = chatHistoryService.getChatHistoryDetail(
                    loginUser.getId(), chatId, MANUS_APP_TYPE);
            if (history == null || history.getMessages() == null) {
                return;
            }
            List<Message> messages = history.getMessages().stream()
                    .map(this::toSpringAiMessage)
                    .filter(message -> message != null && StrUtil.isNotBlank(message.getText()))
                    .toList();
            nekoManus.setMessageList(new ArrayList<>(messages));
        } catch (Exception e) {
            log.warn("加载 Manus 会话历史失败，chatId：{}", chatId, e);
        }
    }

    /**
     * 将前端持久化的历史消息转换为 Spring AI 消息对象。
     *
     * @param message 历史消息
     * @return Spring AI 消息
     */
    private Message toSpringAiMessage(ChatHistoryDetailDTO.ChatMessage message) {
        if (message == null || StrUtil.isBlank(message.getContent())) {
            return null;
        }
        if ("user".equalsIgnoreCase(message.getRole())) {
            return new UserMessage(message.getContent());
        }
        if ("ai".equalsIgnoreCase(message.getRole())
                || "assistant".equalsIgnoreCase(message.getRole())) {
            return new AssistantMessage(message.getContent());
        }
        return null;
    }


}
