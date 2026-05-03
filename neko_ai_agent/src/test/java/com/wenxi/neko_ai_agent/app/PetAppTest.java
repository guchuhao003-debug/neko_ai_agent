package com.wenxi.neko_ai_agent.app;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class PetAppTest {

    @Resource
    private PetApp petApp;

    @Test
    void doChat() {
        String userMessage = "你好，我是温习";
        String chatId = UUID.randomUUID().toString();
        String answer = petApp.doChat(userMessage, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChatStream() {
    }

    @Test
    void doChatWithReport() {
    }

    @Test
    void doChatWithTools() {
    }

    @Test
    void doChatWithMcp() {
    }
}