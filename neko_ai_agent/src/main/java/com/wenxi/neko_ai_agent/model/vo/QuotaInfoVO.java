package com.wenxi.neko_ai_agent.model.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 用户积分配额视图。
 */
@Data
public class QuotaInfoVO {

    /**
     * 每日免费积分余额。
     */
    private Integer dailyQuota;

    /**
     * 额外积分余额。
     */
    private Integer bonusQuota;

    /**
     * 可用总积分。
     */
    private Integer totalQuota;

    /**
     * 每日积分重置日期。
     */
    private LocalDate quotaResetDate;

    /**
     * 单次对话扣减积分。
     */
    private Integer chatCost;
}
