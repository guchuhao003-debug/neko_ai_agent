package com.wenxi.neko_ai_agent.rag;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * PGVector 配置属性测试。
 */
class PgVectorPropertiesTest {

    /**
     * 默认配置应适合生产环境手动建表策略。
     */
    @Test
    void shouldUseSafeDefaults() {
        PgVectorProperties properties = new PgVectorProperties();

        assertThat(properties.getSchemaName()).isEqualTo("public");
        assertThat(properties.getDimensions()).isEqualTo(1536);
        assertThat(properties.isInitializeSchema()).isFalse();
        assertThat(properties.getLoveTableName()).isEqualTo("love_vector_store");
        assertThat(properties.getPetTableName()).isEqualTo("pet_vector_store");
        assertThat(properties.getDatasource().getMaximumPoolSize()).isEqualTo(5);
    }

    /**
     * 启用 PGVector 时必须提供独立 PostgreSQL 连接信息。
     */
    @Test
    void shouldValidateRequiredDatasourceProperties() {
        PgVectorProperties properties = new PgVectorProperties();

        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("datasource.url");

        properties.getDatasource().setUrl("jdbc:postgresql://localhost:5432/neko_vector");
        assertThatThrownBy(properties::validate)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("datasource.username");

        properties.getDatasource().setUsername("postgres");
        properties.validate();
    }
}
