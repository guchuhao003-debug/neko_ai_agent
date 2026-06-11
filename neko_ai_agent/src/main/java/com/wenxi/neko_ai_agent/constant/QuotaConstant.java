package com.wenxi.neko_ai_agent.constant;

/**
 * 积分配额常量。
 */
public interface QuotaConstant {

    /**
     * 用户每日免费积分。
     */
    int DAILY_FREE_QUOTA = 100;

    /**
     * 每次智能体对话扣减积分。
     */
    int CHAT_COST = 10;

    /**
     * 兑换码有效小时数。
     */
    int REDEEM_CODE_EXPIRE_HOURS = 24;
}
