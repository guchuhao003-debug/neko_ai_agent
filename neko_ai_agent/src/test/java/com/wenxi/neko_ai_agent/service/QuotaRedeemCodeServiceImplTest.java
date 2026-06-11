package com.wenxi.neko_ai_agent.service;

import com.wenxi.neko_ai_agent.mapper.QuotaRedeemCodeMapper;
import com.wenxi.neko_ai_agent.model.entity.QuotaRedeemCode;
import com.wenxi.neko_ai_agent.model.vo.QuotaInfoVO;
import com.wenxi.neko_ai_agent.service.impl.QuotaRedeemCodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 积分兑换码服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class QuotaRedeemCodeServiceImplTest {

    private QuotaRedeemCodeServiceImpl redeemCodeService;

    @Mock
    private QuotaRedeemCodeMapper redeemCodeMapper;

    @Mock
    private QuotaService quotaService;

    @Mock
    private TransactionTemplate transactionTemplate;

    /**
     * 初始化兑换码服务。
     */
    @BeforeEach
    void setUp() {
        redeemCodeService = new QuotaRedeemCodeServiceImpl();
        ReflectionTestUtils.setField(redeemCodeService, "baseMapper", redeemCodeMapper);
        ReflectionTestUtils.setField(redeemCodeService, "quotaService", quotaService);
        ReflectionTestUtils.setField(redeemCodeService, "transactionTemplate", transactionTemplate);
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());
    }

    /**
     * 用户兑换有效兑换码后应增加额外积分并返回最新积分。
     */
    @Test
    void redeemShouldAddBonusQuotaAndReturnQuotaInfo() {
        QuotaRedeemCode code = new QuotaRedeemCode();
        code.setId(1L);
        code.setCode("NQ-TESTCODE");
        code.setQuotaAmount(200);
        code.setStatus("UNUSED");
        code.setExpireTime(new Date(System.currentTimeMillis() + 60_000L));
        QuotaInfoVO quotaInfo = new QuotaInfoVO();
        quotaInfo.setBonusQuota(200);
        quotaInfo.setTotalQuota(300);
        when(redeemCodeMapper.selectOne(any(), anyBoolean())).thenReturn(code);
        when(redeemCodeMapper.update(any(), any())).thenReturn(1);
        when(quotaService.getQuotaInfo(1L)).thenReturn(quotaInfo);

        QuotaInfoVO result = redeemCodeService.redeem("nq-testcode", 1L);

        verify(quotaService).addBonusQuota(1L, 200);
        assertEquals(300, result.getTotalQuota());
    }
}
