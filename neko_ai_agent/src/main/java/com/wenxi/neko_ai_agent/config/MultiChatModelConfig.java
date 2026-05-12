package com.wenxi.neko_ai_agent.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class MultiChatModelConfig {

    @Bean
    public Map<String, ChatModel> chatModelMap(MultiModelProperties properties) {
        Map<String, ChatModel> modelMap = new HashMap<>();

        for (MultiModelProperties.ModelConfig config : properties.getModels()) {
            ChatModel chatModel = createChatModel(config);
            modelMap.put(config.getId(), chatModel);
        }

        return modelMap;
    }

    private ChatModel createChatModel(MultiModelProperties.ModelConfig config) {
        return switch (config.getType().toLowerCase()) {
            case "dashscope" -> {
                DashScopeApi dashScopeApi = DashScopeApi.builder()
                        .apiKey(config.getApiKey())
                        .build();
                DashScopeChatOptions options = DashScopeChatOptions.builder()
                        .withModel(config.getModel())
                        .build();
                yield DashScopeChatModel.builder()
                        .dashScopeApi(dashScopeApi)
                        .defaultOptions(options)
                        .build();
            }
            case "openai" -> {
                OpenAiApi openAiApi = OpenAiApi.builder()
                        .apiKey(config.getApiKey())
                        .baseUrl(config.getBaseUrl())
                        .build();
                OpenAiChatOptions options = OpenAiChatOptions.builder()
                        .model(config.getModel())
                        .build();
                yield OpenAiChatModel.builder()
                        .openAiApi(openAiApi)
                        .defaultOptions(options)
                        .build();
            }
            default -> throw new IllegalArgumentException("Unsupported model type: " + config.getType());
        };
    }

}
