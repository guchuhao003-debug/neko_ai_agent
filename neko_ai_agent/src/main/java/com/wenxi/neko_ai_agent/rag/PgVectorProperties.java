package com.wenxi.neko_ai_agent.rag;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * PGVector 向量库配置属性。
 */
@Data
@ConfigurationProperties(prefix = "neko.rag.pgvector")
public class PgVectorProperties {

    /**
     * PGVector 独立数据库连接配置。
     */
    private Datasource datasource = new Datasource();

    /**
     * PostgreSQL schema 名称。
     */
    private String schemaName = "public";

    /**
     * Embedding 向量维度，需要和当前 Embedding 模型输出维度一致。
     */
    private int dimensions = 1536;

    /**
     * 是否由 Spring AI 自动初始化 PGVector 表结构。
     */
    private boolean initializeSchema = false;

    /**
     * 批量写入文档的最大条数。
     */
    private int maxDocumentBatchSize = 10000;

    /**
     * 恋爱知识库向量表名。
     */
    private String loveTableName = "love_vector_store";

    /**
     * 宠物知识库向量表名。
     */
    private String petTableName = "pet_vector_store";

    /**
     * 校验 PGVector 必填配置。
     */
    void validate() {
        if (!StringUtils.hasText(datasource.getUrl())) {
            throw new IllegalStateException("neko.rag.pgvector.datasource.url 不能为空");
        }
        if (!StringUtils.hasText(datasource.getUsername())) {
            throw new IllegalStateException("neko.rag.pgvector.datasource.username 不能为空");
        }
        if (dimensions <= 0) {
            throw new IllegalStateException("neko.rag.pgvector.dimensions 必须大于 0");
        }
    }

    /**
     * PGVector 数据源配置。
     */
    @Data
    public static class Datasource {

        /**
         * PostgreSQL JDBC URL。
         */
        private String url;

        /**
         * PostgreSQL 用户名。
         */
        private String username;

        /**
         * PostgreSQL 密码。
         */
        private String password;

        /**
         * 连接池最大连接数。
         */
        private int maximumPoolSize = 5;

        /**
         * 连接池最小空闲连接数。
         */
        private int minimumIdle = 1;

        /**
         * 获取连接超时时间，单位毫秒。
         */
        private long connectionTimeout = 30000L;
    }
}
