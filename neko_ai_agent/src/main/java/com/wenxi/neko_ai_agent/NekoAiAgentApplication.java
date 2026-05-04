package com.wenxi.neko_ai_agent;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.autoconfigure.vectorstore.pgvector.PgVectorStoreAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication(exclude = PgVectorStoreAutoConfiguration.class)
@MapperScan("com.wenxi.neko_ai_agent.mapper")
@EnableAsync
public class NekoAiAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(NekoAiAgentApplication.class, args);
    }

}
