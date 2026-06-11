package com.wenxi.neko_ai_agent.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 积分兑换码。
 */
@TableName(value = "quota_redeem_code")
@Data
public class QuotaRedeemCode implements Serializable {

    /**
     * id。
     */
    @TableId(type = IdType.ASSIGN_ID)
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
     * 状态：UNUSED / USED。
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

    /**
     * 更新时间。
     */
    private Date updateTime;

    /**
     * 是否删除。
     */
    @TableLogic
    private Integer isDelete;

    private static final long serialVersionUID = 1L;
}
