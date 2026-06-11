package com.wenxi.neko_ai_agent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wenxi.neko_ai_agent.annotation.AuthCheck;
import com.wenxi.neko_ai_agent.common.DeleteRequest;
import com.wenxi.neko_ai_agent.common.ResultUtils;
import com.wenxi.neko_ai_agent.constant.UserConstant;
import com.wenxi.neko_ai_agent.exception.BaseResponse;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import com.wenxi.neko_ai_agent.exception.ThrowUtils;
import com.wenxi.neko_ai_agent.model.dto.quota.QuotaRedeemCodeGenerateRequest;
import com.wenxi.neko_ai_agent.model.dto.quota.QuotaRedeemCodeQueryRequest;
import com.wenxi.neko_ai_agent.model.dto.quota.QuotaRedeemRequest;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.model.vo.QuotaInfoVO;
import com.wenxi.neko_ai_agent.model.vo.QuotaRedeemCodeVO;
import com.wenxi.neko_ai_agent.service.QuotaRedeemCodeService;
import com.wenxi.neko_ai_agent.service.QuotaService;
import com.wenxi.neko_ai_agent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 积分配额接口。
 */
@RestController
@RequestMapping("/quota")
public class QuotaController {

    @Resource
    private QuotaService quotaService;

    @Resource
    private QuotaRedeemCodeService quotaRedeemCodeService;

    @Resource
    private UserService userService;

    /**
     * 查询当前用户积分配额。
     *
     * @param request HTTP 请求
     * @return 积分配额
     */
    @GetMapping("/my")
    @AuthCheck
    public BaseResponse<QuotaInfoVO> getMyQuota(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(quotaService.getQuotaInfo(loginUser.getId()));
    }

    /**
     * 用户兑换积分。
     *
     * @param redeemRequest 兑换请求
     * @param request HTTP 请求
     * @return 兑换后的积分信息
     */
    @PostMapping("/redeem")
    @AuthCheck
    public BaseResponse<QuotaInfoVO> redeem(@RequestBody @Valid QuotaRedeemRequest redeemRequest,
                                            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(quotaRedeemCodeService.redeem(
                redeemRequest.getCode(), loginUser.getId()));
    }

    /**
     * 管理员批量生成积分兑换码。
     *
     * @param generateRequest 生成请求
     * @param request HTTP 请求
     * @return 生成的兑换码列表
     */
    @PostMapping("/admin/redeem-code/generate")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<List<QuotaRedeemCodeVO>> generateCodes(
            @RequestBody @Valid QuotaRedeemCodeGenerateRequest generateRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(quotaRedeemCodeService.generateCodes(
                generateRequest, loginUser.getId()));
    }

    /**
     * 管理员分页查看兑换码列表。
     *
     * @param queryRequest 查询请求
     * @return 兑换码分页
     */
    @PostMapping("/admin/redeem-code/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<QuotaRedeemCodeVO>> listCodes(
            @RequestBody QuotaRedeemCodeQueryRequest queryRequest) {
        return ResultUtils.success(quotaRedeemCodeService.listCodes(queryRequest));
    }

    /**
     * 管理员删除兑换码。
     *
     * @param deleteRequest 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/admin/redeem-code/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteCode(@RequestBody @Valid DeleteRequest deleteRequest) {
        ThrowUtils.throwIf(deleteRequest == null || deleteRequest.getId() == null,
                ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(quotaRedeemCodeService.deleteCode(deleteRequest.getId()));
    }
}
