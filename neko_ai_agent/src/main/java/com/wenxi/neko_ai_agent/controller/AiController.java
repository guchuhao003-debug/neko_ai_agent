package com.wenxi.neko_ai_agent.controller;

import com.wenxi.neko_ai_agent.agent.NekoManus;
import com.wenxi.neko_ai_agent.app.LoveApp;
import jakarta.annotation.Resource;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.io.IOException;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Resource
    private LoveApp loveApp;

    @Resource
    private ChatModel dashscopeChatModel;

    @Resource
    private ToolCallback[] allTools;


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
     * @return
     */
    @GetMapping(value = "/love_app/chat/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> doChatWithLoveAppSSE(String message, String chatId) {
        return loveApp.doChatByStream(message,chatId);
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
     * 流式调用 Manus AI Agent
     * @param message
     * @return
     */
    @GetMapping("/manus/chat")
    public SseEmitter doChatWithManus(String message) {
        // 单例，每次调用都需要重新创建一个实例，如果通过注入方式，那么用户调用的都是同一个实例，则容易造成阻塞
        NekoManus nekoManus = new NekoManus(allTools,dashscopeChatModel);
        return nekoManus.runStream(message);
    }

}
