package com.wenxi.neko_ai_agent.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wenxi.neko_ai_agent.model.dto.quota.QuotaRedeemCodeGenerateRequest;
import com.wenxi.neko_ai_agent.model.dto.quota.QuotaRedeemCodeQueryRequest;
import com.wenxi.neko_ai_agent.model.entity.QuotaRedeemCode;
import com.wenxi.neko_ai_agent.model.vo.QuotaInfoVO;
import com.wenxi.neko_ai_agent.model.vo.QuotaRedeemCodeVO;

import java.util.List;

/**
 * 积分兑换码服务。
 */
public interface QuotaRedeemCodeService extends IService<QuotaRedeemCode> {

    /**
     * 管理员批量生成兑换码。
     *
     * @param request 生成请求
     * @param adminUserId 管理员用户 ID
     * @return 生成的兑换码
     */
    List<QuotaRedeemCodeVO> generateCodes(QuotaRedeemCodeGenerateRequest request,
                                          Long adminUserId);

    /**
     * 分页查询兑换码。
     *
     * @param request 查询请求
     * @return 分页兑换码
     */
    Page<QuotaRedeemCodeVO> listCodes(QuotaRedeemCodeQueryRequest request);

    /**
     * 删除兑换码。
     *
     * @param id 兑换码 ID
     * @return 是否删除成功
     */
    boolean deleteCode(Long id);

    /**
     * 用户兑换积分。
     *
     * @param code 兑换码
     * @param userId 用户 ID
     * @return 兑换后的积分信息
     */
    QuotaInfoVO redeem(String code, Long userId);
}
