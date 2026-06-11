package com.wenxi.neko_ai_agent.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 数据库结构迁移器单元测试。
 */
@ExtendWith(MockitoExtension.class)
class DatabaseSchemaMigrationRunnerTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private ApplicationArguments applicationArguments;

    /**
     * 旧库缺少积分字段时应自动补齐字段和兑换码表。
     */
    @Test
    void runShouldAddMissingQuotaColumnsAndRedeemCodeTable() {
        DatabaseSchemaMigrationRunner runner = new DatabaseSchemaMigrationRunner(jdbcTemplate);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Integer.class), any()))
                .thenReturn(1, 0, 0, 0);

        runner.run(applicationArguments);

        verify(jdbcTemplate).execute(contains("ADD COLUMN dailyQuota"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN bonusQuota"));
        verify(jdbcTemplate).execute(contains("ADD COLUMN quotaResetDate"));
        verify(jdbcTemplate).update(contains("UPDATE user"));
        verify(jdbcTemplate).execute(contains("CREATE TABLE IF NOT EXISTS quota_redeem_code"));
    }
}
