package com.wenxi.neko_ai_agent.model.dto.quota;

import com.wenxi.neko_ai_agent.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 积分兑换码查询请求。
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class QuotaRedeemCodeQueryRequest extends PageRequest implements Serializable {

    /**
     * 兑换码。
     */
    private String code;

    /**
     * 状态：UNUSED / USED / EXPIRED。
     */
    private String status;

    private static final long serialVersionUID = 1L;
}
