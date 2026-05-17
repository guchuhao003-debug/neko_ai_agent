package com.wenxi.neko_ai_agent.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 智能体视图对象。
 */
@Data
public class AgentVO implements Serializable {

    /**
     * 智能体 ID。
     */
    private Long id;

    /**
     * 创建者用户 ID。
     */
    private String userId;

    /**
     * 智能体名称。
     */
    private String name;

    /**
     * 头像 URL。
     */
    private String avatar;

    /**
     * 系统提示词。
     */
    private String systemPrompt;

    /**
     * 模型 ID。
     */
    private String modelId;

    /**
     * 温度参数。
     */
    private BigDecimal temperature;

    /**
     * 最大输出 token 数。
     */
    private Integer maxTokens;

    /**
     * 是否公开。
     */
    private Boolean isPublic;

    /**
     * 是否启用。
     */
    private Boolean status;

    /**
     * 使用次数。
     */
    private Integer useCount;

    /**
     * 创建时间。
     */
    private Date createTime;

    /**
     * 更新时间。
     */
    private Date updateTime;

    private static final long serialVersionUID = 1L;
}
