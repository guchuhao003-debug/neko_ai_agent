package com.wenxi.neko_ai_agent.service;

import com.wenxi.neko_ai_agent.model.vo.QuotaInfoVO;

/**
 * 用户积分配额服务。
 */
public interface QuotaService {

    /**
     * 获取用户积分信息，会先按需重置每日免费积分。
     *
     * @param userId 用户 ID
     * @return 积分信息
     */
    QuotaInfoVO getQuotaInfo(Long userId);

    /**
     * 智能体对话前扣减积分。
     *
     * @param userId 用户 ID
     */
    void deductForChat(Long userId);

    /**
     * 增加额外积分。
     *
     * @param userId 用户 ID
     * @param amount 增加积分
     */
    void addBonusQuota(Long userId, int amount);

    /**
     * 重置所有用户每日免费积分。
     *
     * @return 更新用户数
     */
    int resetAllDailyQuota();
}
