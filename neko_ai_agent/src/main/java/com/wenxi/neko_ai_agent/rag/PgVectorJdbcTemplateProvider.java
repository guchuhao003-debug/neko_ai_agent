package com.wenxi.neko_ai_agent.rag;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * PGVector 专用 JdbcTemplate 提供器，避免暴露额外 DataSource 干扰 MySQL 业务库。
 */
class PgVectorJdbcTemplateProvider implements AutoCloseable {

    private final HikariDataSource dataSource;

    private final JdbcTemplate jdbcTemplate;

    PgVectorJdbcTemplateProvider(PgVectorProperties properties) {
        PgVectorProperties.Datasource datasourceProperties = properties.getDatasource();
        this.dataSource = new HikariDataSource();
        this.dataSource.setPoolName("NekoPgVectorPool");
        this.dataSource.setDriverClassName("org.postgresql.Driver");
        this.dataSource.setJdbcUrl(datasourceProperties.getUrl());
        this.dataSource.setUsername(datasourceProperties.getUsername());
        this.dataSource.setPassword(datasourceProperties.getPassword());
        this.dataSource.setMaximumPoolSize(datasourceProperties.getMaximumPoolSize());
        this.dataSource.setMinimumIdle(datasourceProperties.getMinimumIdle());
        this.dataSource.setConnectionTimeout(datasourceProperties.getConnectionTimeout());
        this.jdbcTemplate = new JdbcTemplate(this.dataSource);
    }

    /**
     * 获取 PGVector 专用 JdbcTemplate。
     */
    JdbcTemplate jdbcTemplate() {
        return jdbcTemplate;
    }

    /**
     * 关闭 PGVector 专用连接池。
     */
    @Override
    public void close() {
        dataSource.close();
    }
}
