package com.wenxi.neko_ai_agent;

import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
public class NekoAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(NekoAiAgentApplication.class, args);
    }

}
