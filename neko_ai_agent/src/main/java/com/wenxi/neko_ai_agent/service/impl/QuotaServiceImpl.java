package com.wenxi.neko_ai_agent.service.impl;

import com.wenxi.neko_ai_agent.constant.QuotaConstant;
import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import com.wenxi.neko_ai_agent.mapper.UserMapper;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.model.vo.QuotaInfoVO;
import com.wenxi.neko_ai_agent.service.QuotaService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * 用户积分配额服务实现。
 */
@Service
@Slf4j
public class QuotaServiceImpl implements QuotaService {

    @Resource
    private UserMapper userMapper;

    /**
     * 获取用户积分信息。
     *
     * @param userId 用户 ID
     * @return 积分信息
     */
    @Override
    public QuotaInfoVO getQuotaInfo(Long userId) {
        validateUserId(userId);
        resetDailyQuotaIfNeeded(userId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        return buildQuotaInfo(user);
    }

    /**
     * 智能体对话前扣减积分。
     *
     * @param userId 用户 ID
     */
    @Override
    public void deductForChat(Long userId) {
        validateUserId(userId);
        resetDailyQuotaIfNeeded(userId);
        int updated = userMapper.deductQuota(userId, QuotaConstant.CHAT_COST);
        if (updated <= 0) {
            QuotaInfoVO quotaInfo = getQuotaInfo(userId);
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR,
                    "积分不足，当前可用积分 " + quotaInfo.getTotalQuota()
                            + "，每次对话需要 " + QuotaConstant.CHAT_COST + " 积分");
        }
    }

    /**
     * 增加额外积分。
     *
     * @param userId 用户 ID
     * @param amount 增加积分
     */
    @Override
    public void addBonusQuota(Long userId, int amount) {
        validateUserId(userId);
        if (amount <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "积分额度必须大于 0");
        }
        int updated = userMapper.addBonusQuota(userId, amount);
        if (updated <= 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
    }

    /**
     * 每天凌晨重置所有用户每日免费积分。
     *
     * @return 更新用户数
     */
    @Override
    @Scheduled(cron = "0 0 0 * * ?")
    public int resetAllDailyQuota() {
        int updated = userMapper.resetAllDailyQuota(LocalDate.now(),
                QuotaConstant.DAILY_FREE_QUOTA);
        log.info("每日免费积分重置完成，更新用户数：{}", updated);
        return updated;
    }

    /**
     * 按需重置单个用户每日免费积分。
     *
     * @param userId 用户 ID
     */
    private void resetDailyQuotaIfNeeded(Long userId) {
        userMapper.resetDailyQuotaIfNeeded(userId, LocalDate.now(),
                QuotaConstant.DAILY_FREE_QUOTA);
    }

    /**
     * 构建积分视图。
     *
     * @param user 用户
     * @return 积分信息
     */
    private QuotaInfoVO buildQuotaInfo(User user) {
        int dailyQuota = normalizeQuota(user.getDailyQuota());
        int bonusQuota = normalizeQuota(user.getBonusQuota());
        QuotaInfoVO quotaInfo = new QuotaInfoVO();
        quotaInfo.setDailyQuota(dailyQuota);
        quotaInfo.setBonusQuota(bonusQuota);
        quotaInfo.setTotalQuota(dailyQuota + bonusQuota);
        quotaInfo.setQuotaResetDate(user.getQuotaResetDate());
        quotaInfo.setChatCost(QuotaConstant.CHAT_COST);
        return quotaInfo;
    }

    /**
     * 校验用户 ID。
     *
     * @param userId 用户 ID
     */
    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不合法");
        }
    }

    /**
     * 规整积分空值。
     *
     * @param quota 积分
     * @return 非空积分
     */
    private int normalizeQuota(Integer quota) {
        return quota == null ? 0 : quota;
    }
}
