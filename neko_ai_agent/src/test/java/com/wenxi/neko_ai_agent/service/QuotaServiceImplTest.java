package com.wenxi.neko_ai_agent.service;

import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.mapper.UserMapper;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.model.vo.QuotaInfoVO;
import com.wenxi.neko_ai_agent.service.impl.QuotaServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 积分配额服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class QuotaServiceImplTest {

    private QuotaServiceImpl quotaService;

    @Mock
    private UserMapper userMapper;

    /**
     * 初始化积分服务。
     */
    @BeforeEach
    void setUp() {
        quotaService = new QuotaServiceImpl();
        ReflectionTestUtils.setField(quotaService, "userMapper", userMapper);
    }

    /**
     * 对话扣费前应先按需重置每日免费积分，然后原子扣减 10 积分。
     */
    @Test
    void deductForChatShouldResetAndDeductQuota() {
        when(userMapper.deductQuota(1L, 10)).thenReturn(1);

        quotaService.deductForChat(1L);

        verify(userMapper).resetDailyQuotaIfNeeded(eq(1L), any(LocalDate.class), eq(100));
        verify(userMapper).deductQuota(1L, 10);
    }

    /**
     * 积分不足时应阻断对话调用。
     */
    @Test
    void deductForChatShouldRejectWhenQuotaInsufficient() {
        User user = new User();
        user.setId(1L);
        user.setDailyQuota(0);
        user.setBonusQuota(0);
        user.setQuotaResetDate(LocalDate.now());
        when(userMapper.deductQuota(1L, 10)).thenReturn(0);
        when(userMapper.selectById(1L)).thenReturn(user);

        assertThrows(BusinessException.class, () -> quotaService.deductForChat(1L));
    }

    /**
     * 查询积分时应返回每日积分和额外积分合计。
     */
    @Test
    void getQuotaInfoShouldReturnTotalQuota() {
        User user = new User();
        user.setId(1L);
        user.setDailyQuota(80);
        user.setBonusQuota(50);
        user.setQuotaResetDate(LocalDate.now());
        when(userMapper.selectById(1L)).thenReturn(user);

        QuotaInfoVO quotaInfo = quotaService.getQuotaInfo(1L);

        assertEquals(80, quotaInfo.getDailyQuota());
        assertEquals(50, quotaInfo.getBonusQuota());
        assertEquals(130, quotaInfo.getTotalQuota());
        assertEquals(10, quotaInfo.getChatCost());
    }
}
