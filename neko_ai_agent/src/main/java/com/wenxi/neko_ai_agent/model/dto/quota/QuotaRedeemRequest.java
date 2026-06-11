package com.wenxi.neko_ai_agent.model.dto.quota;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户兑换积分请求。
 */
@Data
public class QuotaRedeemRequest implements Serializable {

    /**
     * 兑换码。
     */
    @NotBlank(message = "兑换码不能为空")
    private String code;

    private static final long serialVersionUID = 1L;
}
