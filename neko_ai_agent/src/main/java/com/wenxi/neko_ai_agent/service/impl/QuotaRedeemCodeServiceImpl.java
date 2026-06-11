package com.wenxi.neko_ai_agent.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wenxi.neko_ai_agent.constant.QuotaConstant;
import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import com.wenxi.neko_ai_agent.mapper.QuotaRedeemCodeMapper;
import com.wenxi.neko_ai_agent.model.dto.quota.QuotaRedeemCodeGenerateRequest;
import com.wenxi.neko_ai_agent.model.dto.quota.QuotaRedeemCodeQueryRequest;
import com.wenxi.neko_ai_agent.model.entity.QuotaRedeemCode;
import com.wenxi.neko_ai_agent.model.vo.QuotaInfoVO;
import com.wenxi.neko_ai_agent.model.vo.QuotaRedeemCodeVO;
import com.wenxi.neko_ai_agent.service.QuotaRedeemCodeService;
import com.wenxi.neko_ai_agent.service.QuotaService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 积分兑换码服务实现。
 */
@Service
public class QuotaRedeemCodeServiceImpl
        extends ServiceImpl<QuotaRedeemCodeMapper, QuotaRedeemCode>
        implements QuotaRedeemCodeService {

    private static final String STATUS_UNUSED = "UNUSED";

    private static final String STATUS_USED = "USED";

    private static final String STATUS_EXPIRED = "EXPIRED";

    @Resource
    private QuotaService quotaService;

    @Resource
    private TransactionTemplate transactionTemplate;

    /**
     * 管理员批量生成兑换码。
     *
     * @param request 生成请求
     * @param adminUserId 管理员用户 ID
     * @return 生成的兑换码
     */
    @Override
    public List<QuotaRedeemCodeVO> generateCodes(QuotaRedeemCodeGenerateRequest request,
                                                Long adminUserId) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数不能为空");
        }
        if (adminUserId == null || adminUserId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "管理员 ID 不合法");
        }
        validateGenerateRequest(request);

        Date now = new Date();
        Date expireTime = new Date(now.getTime()
                + QuotaConstant.REDEEM_CODE_EXPIRE_HOURS * 60L * 60L * 1000L);
        List<QuotaRedeemCode> codes = new ArrayList<>();
        for (int i = 0; i < request.getCount(); i++) {
            QuotaRedeemCode code = new QuotaRedeemCode();
            code.setCode(generateUniqueCode());
            code.setQuotaAmount(request.getQuotaAmount());
            code.setStatus(STATUS_UNUSED);
            code.setExpireTime(expireTime);
            code.setCreateUserId(adminUserId);
            code.setCreateTime(now);
            code.setUpdateTime(now);
            codes.add(code);
        }
        boolean saved = this.saveBatch(codes);
        if (!saved) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "兑换码生成失败");
        }
        return codes.stream().map(this::toVO).toList();
    }

    /**
     * 分页查询兑换码。
     *
     * @param request 查询请求
     * @return 分页兑换码
     */
    @Override
    public Page<QuotaRedeemCodeVO> listCodes(QuotaRedeemCodeQueryRequest request) {
        if (request == null) {
            request = new QuotaRedeemCodeQueryRequest();
        }
        Page<QuotaRedeemCode> pageRequest = new Page<>(
                Math.max(request.getCurrent(), 1),
                Math.min(Math.max(request.getPageSize(), 1), 100)
        );
        LambdaQueryWrapper<QuotaRedeemCode> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(request.getCode()), QuotaRedeemCode::getCode,
                        normalizeCode(request.getCode()))
                .orderByDesc(QuotaRedeemCode::getCreateTime);
        appendStatusCondition(queryWrapper, request.getStatus(), new Date());

        Page<QuotaRedeemCode> codePage = this.page(pageRequest, queryWrapper);
        Page<QuotaRedeemCodeVO> voPage = new Page<>(codePage.getCurrent(), codePage.getSize(),
                codePage.getTotal());
        voPage.setRecords(codePage.getRecords().stream().map(this::toVO).toList());
        return voPage;
    }

    /**
     * 删除兑换码。
     *
     * @param id 兑换码 ID
     * @return 是否删除成功
     */
    @Override
    public boolean deleteCode(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "兑换码 ID 不合法");
        }
        boolean removed = this.removeById(id);
        if (!removed) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "兑换码不存在");
        }
        return true;
    }

    /**
     * 用户兑换积分。
     *
     * @param code 兑换码
     * @param userId 用户 ID
     * @return 兑换后的积分信息
     */
    @Override
    public QuotaInfoVO redeem(String code, Long userId) {
        if (StrUtil.isBlank(code)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "兑换码不能为空");
        }
        if (userId == null || userId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户 ID 不合法");
        }
        return transactionTemplate.execute(status -> redeemInTransaction(code, userId));
    }

    /**
     * 在事务中兑换积分。
     *
     * @param rawCode 原始兑换码
     * @param userId 用户 ID
     * @return 兑换后的积分信息
     */
    private QuotaInfoVO redeemInTransaction(String rawCode, Long userId) {
        String normalizedCode = normalizeCode(rawCode);
        QuotaRedeemCode redeemCode = this.getOne(new LambdaQueryWrapper<QuotaRedeemCode>()
                .eq(QuotaRedeemCode::getCode, normalizedCode));
        if (redeemCode == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "兑换码不存在");
        }
        Date now = new Date();
        if (STATUS_USED.equals(redeemCode.getStatus())) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "兑换码已被使用");
        }
        if (redeemCode.getExpireTime() == null || !redeemCode.getExpireTime().after(now)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "兑换码已过期");
        }

        QuotaRedeemCode updateCode = new QuotaRedeemCode();
        updateCode.setStatus(STATUS_USED);
        updateCode.setUsedUserId(userId);
        updateCode.setUsedTime(now);
        updateCode.setUpdateTime(now);
        boolean updated = this.update(updateCode, new LambdaUpdateWrapper<QuotaRedeemCode>()
                .eq(QuotaRedeemCode::getId, redeemCode.getId())
                .eq(QuotaRedeemCode::getStatus, STATUS_UNUSED)
                .gt(QuotaRedeemCode::getExpireTime, now));
        if (!updated) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "兑换码状态已变化，请刷新后重试");
        }

        quotaService.addBonusQuota(userId, redeemCode.getQuotaAmount());
        return quotaService.getQuotaInfo(userId);
    }

    /**
     * 添加状态查询条件。
     *
     * @param queryWrapper 查询条件
     * @param status 状态
     * @param now 当前时间
     */
    private void appendStatusCondition(LambdaQueryWrapper<QuotaRedeemCode> queryWrapper,
                                       String status, Date now) {
        if (StrUtil.isBlank(status)) {
            return;
        }
        String normalizedStatus = status.trim().toUpperCase();
        if (STATUS_EXPIRED.equals(normalizedStatus)) {
            queryWrapper.eq(QuotaRedeemCode::getStatus, STATUS_UNUSED)
                    .le(QuotaRedeemCode::getExpireTime, now);
            return;
        }
        queryWrapper.eq(QuotaRedeemCode::getStatus, normalizedStatus);
        if (STATUS_UNUSED.equals(normalizedStatus)) {
            queryWrapper.gt(QuotaRedeemCode::getExpireTime, now);
        }
    }

    /**
     * 校验生成请求。
     *
     * @param request 生成请求
     */
    private void validateGenerateRequest(QuotaRedeemCodeGenerateRequest request) {
        if (request.getCount() == null || request.getCount() < 1 || request.getCount() > 1000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "生成数量必须在 1 到 1000 之间");
        }
        if (request.getQuotaAmount() == null || request.getQuotaAmount() <= 0
                || request.getQuotaAmount() > 100000) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "兑换积分额度必须在 1 到 100000 之间");
        }
    }

    /**
     * 生成唯一兑换码。
     *
     * @return 兑换码
     */
    private String generateUniqueCode() {
        String code;
        do {
            code = "NQ-" + UUID.randomUUID().toString().replace("-", "")
                    .substring(0, 16).toUpperCase();
        } while (this.count(new LambdaQueryWrapper<QuotaRedeemCode>()
                .eq(QuotaRedeemCode::getCode, code)) > 0);
        return code;
    }

    /**
     * 兑换码标准化。
     *
     * @param code 兑换码
     * @return 标准兑换码
     */
    private String normalizeCode(String code) {
        return StrUtil.blankToDefault(code, "").trim().toUpperCase();
    }

    /**
     * 转换为视图对象。
     *
     * @param code 兑换码实体
     * @return 兑换码视图
     */
    private QuotaRedeemCodeVO toVO(QuotaRedeemCode code) {
        QuotaRedeemCodeVO vo = new QuotaRedeemCodeVO();
        BeanUtil.copyProperties(code, vo);
        if (STATUS_UNUSED.equals(code.getStatus()) && code.getExpireTime() != null
                && !code.getExpireTime().after(new Date())) {
            vo.setStatus(STATUS_EXPIRED);
        }
        return vo;
    }
}
