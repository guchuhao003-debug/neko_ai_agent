package com.wenxi.neko_ai_agent.model.dto.agent;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 更新智能体请求。
 */
@Data
public class AgentUpdateRequest implements Serializable {

    /**
     * 智能体 ID。
     */
    @NotNull(message = "智能体ID不能为空")
    private Long id;

    /**
     * 智能体名称。
     */
    @Size(max = 50, message = "智能体名称不能超过50个字符")
    private String name;

    /**
     * 智能体头像 URL。
     */
    private String avatar;

    /**
     * 系统提示词。
     */
    @Size(max = 4000, message = "系统提示词不能超过4000个字符")
    private String systemPrompt;

    /**
     * 模型 ID。
     */
    private String modelId;

    /**
     * 温度参数，取值范围 0 到 2。
     */
    @DecimalMin(value = "0", message = "温度参数不能小于0")
    @DecimalMax(value = "2", message = "温度参数不能大于2")
    private BigDecimal temperature;

    /**
     * 最大输出 token 数。
     */
    @Min(value = 256, message = "最大Token数不能小于256")
    @Max(value = 8192, message = "最大Token数不能大于8192")
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

    /**
     * 智能体状态。
     */
    private Boolean status;

    private static final long serialVersionUID = 8248563433648316258L;
}
