package com.wenxi.neko_ai_agent.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库结构轻量迁移器，用于补齐旧库缺失的新功能字段。
 */
@Component
@Slf4j
public class DatabaseSchemaMigrationRunner implements ApplicationRunner {

    private static final String USER_TABLE = "user";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 创建数据库结构迁移器。
     *
     * @param jdbcTemplate JDBC 操作模板
     */
    public DatabaseSchemaMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 应用启动时执行幂等结构迁移。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        ensureUserQuotaColumns();
        ensureQuotaRedeemCodeTable();
    }

    /**
     * 补齐用户积分字段，避免旧库登录查询 dailyQuota 等字段时报错。
     */
    private void ensureUserQuotaColumns() {
        if (!tableExists(USER_TABLE)) {
            log.warn("用户表不存在，跳过积分字段迁移");
            return;
        }
        addColumnIfMissing(USER_TABLE, "dailyQuota",
                "ALTER TABLE user ADD COLUMN dailyQuota int default 100 not null "
                        + "comment '每日免费积分余额' AFTER userRole");
        addColumnIfMissing(USER_TABLE, "bonusQuota",
                "ALTER TABLE user ADD COLUMN bonusQuota int default 0 not null "
                        + "comment '额外积分余额' AFTER dailyQuota");
        addColumnIfMissing(USER_TABLE, "quotaResetDate",
                "ALTER TABLE user ADD COLUMN quotaResetDate date null "
                        + "comment '每日积分重置日期' AFTER bonusQuota");
        jdbcTemplate.update("""
                UPDATE user
                SET dailyQuota = COALESCE(dailyQuota, 100),
                    bonusQuota = COALESCE(bonusQuota, 0),
                    quotaResetDate = COALESCE(quotaResetDate, CURDATE())
                WHERE quotaResetDate IS NULL
                   OR dailyQuota IS NULL
                   OR bonusQuota IS NULL
                """);
    }

    /**
     * 创建积分兑换码表。
     */
    private void ensureQuotaRedeemCodeTable() {
        jdbcTemplate.execute("""
                CREATE TABLE IF NOT EXISTS quota_redeem_code (
                    id           bigint auto_increment comment 'id' primary key,
                    code         varchar(64)                        not null comment '兑换码',
                    quotaAmount  int                                not null comment '兑换积分额度',
                    status       varchar(32) default 'UNUSED'       not null
                        comment '状态：UNUSED/USED',
                    expireTime   datetime                           not null comment '过期时间',
                    usedUserId   bigint                             null comment '使用用户id',
                    usedTime     datetime                           null comment '使用时间',
                    createUserId bigint                             not null comment '创建管理员id',
                    createTime   datetime default CURRENT_TIMESTAMP not null comment '创建时间',
                    updateTime   datetime default CURRENT_TIMESTAMP not null
                        on update CURRENT_TIMESTAMP comment '更新时间',
                    isDelete     tinyint  default 0                 not null comment '是否删除',
                    UNIQUE KEY uk_code (code),
                    INDEX idx_status_expire (status, expireTime),
                    INDEX idx_create_time (createTime)
                ) comment '积分兑换码表' collate = utf8mb4_unicode_ci
                """);
    }

    /**
     * 缺少字段时执行添加字段 SQL。
     *
     * @param tableName 表名
     * @param columnName 字段名
     * @param alterSql 添加字段 SQL
     */
    private void addColumnIfMissing(String tableName, String columnName, String alterSql) {
        if (columnExists(tableName, columnName)) {
            return;
        }
        jdbcTemplate.execute(alterSql);
        log.info("数据库字段迁移完成：{}.{}", tableName, columnName);
    }

    /**
     * 判断当前数据库中表是否存在。
     *
     * @param tableName 表名
     * @return 表是否存在
     */
    private boolean tableExists(String tableName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                """, Integer.class, tableName);
        return count != null && count > 0;
    }

    /**
     * 判断当前数据库中字段是否存在。
     *
     * @param tableName 表名
     * @param columnName 字段名
     * @return 字段是否存在
     */
    private boolean columnExists(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = DATABASE()
                  AND table_name = ?
                  AND column_name = ?
                """, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }
}
