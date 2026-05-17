package com.wenxi.neko_ai_agent.model.dto.agent;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 创建智能体请求。
 */
@Data
public class AgentCreateRequest implements Serializable {

    /**
     * 智能体名称。
     */
    private String name;

    /**
     * 智能体头像 URL。
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
     * 温度参数，取值范围 0 到 2。
     */
    private BigDecimal temperature;

    /**
     * 最大输出 token 数。
     */
    private Integer maxTokens;

    /**
     * 历史字段兼容，前端新代码使用 maxTokens。
     */
    @Deprecated
    private Integer maxToken;

    /**
     * 是否公开。
     */
    private Boolean isPublic;

    private static final long serialVersionUID = 1L;
}
