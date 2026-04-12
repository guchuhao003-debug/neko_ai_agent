package com.wenxi.neko_ai_agent.demo.invoke;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.model.chat.ChatLanguageModel;

public class LangChainAiInvoke {
    private static final String API_KEY = "********";

    public static void main(String[] args) {
        ChatLanguageModel qwenModel = QwenChatModel.builder()
                .apiKey(API_KEY)
                .modelName("qwen-max")
                .build();
        String answer = qwenModel.chat("你好，你有什么技能呢");
        System.out.println(answer);
    }
}
