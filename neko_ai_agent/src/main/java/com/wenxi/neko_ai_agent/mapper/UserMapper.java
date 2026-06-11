package com.wenxi.neko_ai_agent.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenxi.neko_ai_agent.model.entity.User;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/**
* @author kk
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2026-05-03 18:26:25
* @Entity generator.domain.User
*/
public interface UserMapper extends BaseMapper<User> {

    /**
     * 按日期重置单个用户每日免费积分。
     *
     * @param userId 用户 ID
     * @param today 当前日期
     * @param dailyQuota 每日免费积分
     * @return 更新行数
     */
    int resetDailyQuotaIfNeeded(@Param("userId") Long userId,
                                @Param("today") LocalDate today,
                                @Param("dailyQuota") int dailyQuota);

    /**
     * 重置所有用户每日免费积分。
     *
     * @param today 当前日期
     * @param dailyQuota 每日免费积分
     * @return 更新行数
     */
    int resetAllDailyQuota(@Param("today") LocalDate today,
                           @Param("dailyQuota") int dailyQuota);

    /**
     * 原子扣减用户积分，优先扣每日积分，不足部分扣额外积分。
     *
     * @param userId 用户 ID
     * @param cost 扣减积分
     * @return 更新行数
     */
    int deductQuota(@Param("userId") Long userId, @Param("cost") int cost);

    /**
     * 增加用户额外积分。
     *
     * @param userId 用户 ID
     * @param amount 增加积分
     * @return 更新行数
     */
    int addBonusQuota(@Param("userId") Long userId, @Param("amount") int amount);
}




