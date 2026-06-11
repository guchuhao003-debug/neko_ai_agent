package com.wenxi.neko_ai_agent.model.dto.quota;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * 管理员生成积分兑换码请求。
 */
@Data
public class QuotaRedeemCodeGenerateRequest implements Serializable {

    /**
     * 生成数量。
     */
    @NotNull(message = "生成数量不能为空")
    @Min(value = 1, message = "生成数量不能小于 1")
    @Max(value = 1000, message = "单次最多生成 1000 个兑换码")
    private Integer count;

    /**
     * 单个兑换码可兑换积分。
     */
    @NotNull(message = "积分额度不能为空")
    @Min(value = 1, message = "积分额度不能小于 1")
    @Max(value = 100000, message = "单个兑换码积分额度不能超过 100000")
    private Integer quotaAmount;

    private static final long serialVersionUID = 1L;
}
