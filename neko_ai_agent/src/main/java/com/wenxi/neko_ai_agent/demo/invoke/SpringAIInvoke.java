package com.wenxi.neko_ai_agent.demo.invoke;

import jakarta.annotation.Resource;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Spring AI 框架调用 AI 大模型
 */
@Component
// 实现 CommandLineRunner 接口，Spring Boot 启动时会自动调用 run 方法
public class SpringAIInvoke implements CommandLineRunner {

    @Resource
    private ChatModel dashscopeChatModel;


    @Override
    public void run(String... args) throws Exception {
        AssistantMessage assistantMessage = dashscopeChatModel.call(new Prompt("你好，你能帮我完成什么事情呢？"))
                .getResult()
                .getOutput();
        System.out.println(assistantMessage.getText());
    }
}
