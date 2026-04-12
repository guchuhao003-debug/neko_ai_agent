package com.wenxi.neko_ai_agent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class LoveAppTest {

    @Resource
    private LoveApp loveApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();
        // 第一轮
        String message = "你好，我是温习";
        String answer = loveApp.doChat(message,chatId);
        // 第二轮
        message = "我是一名程序员，单身，最近在相亲，但是总是找不到心仪的对象，你能帮我分析一下吗？";
        answer = loveApp.doChat(message,chatId);
        Assertions.assertNotNull(answer);
        // 第三轮
        message = "我单身吗？我刚告诉过你";
        answer = loveApp.doChat(message,chatId);
        Assertions.assertNotNull(answer);
    }

    /**
     * 测试 AI 恋爱报告功能
     */
    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我是温习，我想让另一半（鞠婧祎）更爱我，但我不知道该怎么做";
        LoveApp.LoveReport loveReport = loveApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(loveReport);
    }

    /**
     * 测试 RAG 知识库问答功能
     */
    @Test
    void doChatWithRag() {
        String chatId = UUID.randomUUID().toString();
        String message = "我已经结婚了，但是婚后关系不太亲密，怎么办？";
        String doChatWithRag = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(doChatWithRag);
    }

    /**
     * 测试 RAG 恋爱对象匹配功能
     */
    @Test
    void doChatWithRagCloud(){
        String chatId = UUID.randomUUID().toString();
        String message = "我想要一个女朋友，帮我推荐一个吧";
        String answer = loveApp.doChatWithRag(message, chatId);
        Assertions.assertNotNull(answer);

    }

}