package com.wenxi.neko_ai_agent.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 
 * @TableName agent
 */
@TableName(value ="agent")
@Data
public class Agent implements Serializable {
    /**
     * 
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 创建者用户ID
     */
    private String userId;

    /**
     * 智能体名称
     */
    private String name;

    /**
     * 头像URL
     */
    private String avatar;

    /**
     * 系统提示词
     */
    private String systemPrompt;

    /**
     * 默认模型ID（对应application.yml中的模型id）
     */
    private String modelId;

    /**
     * 温度参数 0-2
     */
    private BigDecimal temperature;

    /**
     * 最大输出token
     */
    private Integer maxTokens;

    /**
     * 是否公开
     */
    private Boolean isPublic;

    /**
     * 状态 0禁用 1启用
     */
    private Boolean status;

    /**
     * 使用次数统计
     */
    private Integer useCount;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 逻辑删除
     */
    @TableLogic
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}