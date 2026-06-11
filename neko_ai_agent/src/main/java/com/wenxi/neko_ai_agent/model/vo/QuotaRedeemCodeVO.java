package com.wenxi.neko_ai_agent.model.vo;

import lombok.Data;

import java.util.Date;

/**
 * 积分兑换码视图。
 */
@Data
public class QuotaRedeemCodeVO {

    /**
     * id。
     */
    private Long id;

    /**
     * 兑换码。
     */
    private String code;

    /**
     * 可兑换积分。
     */
    private Integer quotaAmount;

    /**
     * 状态：UNUSED / USED / EXPIRED。
     */
    private String status;

    /**
     * 过期时间。
     */
    private Date expireTime;

    /**
     * 使用用户 ID。
     */
    private Long usedUserId;

    /**
     * 使用时间。
     */
    private Date usedTime;

    /**
     * 创建管理员 ID。
     */
    private Long createUserId;

    /**
     * 创建时间。
     */
    private Date createTime;
}
