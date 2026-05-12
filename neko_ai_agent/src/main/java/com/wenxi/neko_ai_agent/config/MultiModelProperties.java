package com.wenxi.neko_ai_agent.config;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "spring.ai")
public class MultiModelProperties {

    private List<ModelConfig> models;

    public List<ModelConfig> getModels() {
        return models;
    }

    public void setModels(List<ModelConfig> models) {
        this.models = models;
    }

    @Data
    public static class ModelConfig {
        private String id;
        private String name;
        // dashscope / openai
        private String type;
        private String apiKey;
        private String baseUrl;
        private String model;
    }

}
