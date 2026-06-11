package com.wenxi.neko_ai_agent.rag;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;

import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

/**
 * PGVector 向量库配置，使用独立 PostgreSQL 连接，不影响 MySQL 业务库。
 */
@Configuration
@Slf4j
@EnableConfigurationProperties(PgVectorProperties.class)
@ConditionalOnProperty(prefix = "neko.rag.vector-store", name = "type", havingValue = "pgvector")
public class PgVectorVectorStoreConfig {

    /**
     * 创建 PGVector 专用 JdbcTemplate 提供器。
     *
     * @param properties PGVector 配置
     * @return PGVector JdbcTemplate 提供器
     */
    @Bean(destroyMethod = "close")
    PgVectorJdbcTemplateProvider pgVectorJdbcTemplateProvider(
            PgVectorProperties properties) {
        properties.validate();
        log.info("启用 PGVector 向量库，schema：{}，love 表：{}，pet 表：{}",
                properties.getSchemaName(),
                properties.getLoveTableName(),
                properties.getPetTableName());
        return new PgVectorJdbcTemplateProvider(properties);
    }

    /**
     * 创建恋爱应用 PGVector 向量库。
     *
     * @param provider PGVector JdbcTemplate 提供器
     * @param embeddingModel Embedding 模型
     * @param properties PGVector 配置
     * @return 恋爱应用向量库
     */
    @Bean("loveAppVectorStore")
    @Primary
    VectorStore loveAppVectorStore(
            PgVectorJdbcTemplateProvider provider,
            @Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel,
            PgVectorProperties properties) {
        return buildVectorStore(provider, embeddingModel, properties,
                properties.getLoveTableName());
    }

    /**
     * 创建宠物应用 PGVector 向量库。
     *
     * @param provider PGVector JdbcTemplate 提供器
     * @param embeddingModel Embedding 模型
     * @param properties PGVector 配置
     * @return 宠物应用向量库
     */
    @Bean("petAppVectorStore")
    @Lazy
    @ConditionalOnProperty(prefix = "neko.rag.pet-app", name = "enabled",
            havingValue = "true")
    VectorStore petAppVectorStore(
            PgVectorJdbcTemplateProvider provider,
            @Qualifier("dashscopeEmbeddingModel") EmbeddingModel embeddingModel,
            PgVectorProperties properties) {
        return buildVectorStore(provider, embeddingModel, properties,
                properties.getPetTableName());
    }

    /**
     * 构建指定表名的 PGVector VectorStore。
     *
     * @param provider PGVector JdbcTemplate 提供器
     * @param embeddingModel Embedding 模型
     * @param properties PGVector 配置
     * @param tableName 向量表名
     * @return PGVector VectorStore
     */
    private VectorStore buildVectorStore(
            PgVectorJdbcTemplateProvider provider,
            EmbeddingModel embeddingModel,
            PgVectorProperties properties,
            String tableName) {
        return PgVectorStore.builder(provider.jdbcTemplate(), embeddingModel)
                .dimensions(properties.getDimensions())
                .distanceType(COSINE_DISTANCE)
                .indexType(HNSW)
                .initializeSchema(properties.isInitializeSchema())
                .schemaName(properties.getSchemaName())
                .vectorTableName(tableName)
                .maxDocumentBatchSize(properties.getMaxDocumentBatchSize())
                .build();
    }
}
