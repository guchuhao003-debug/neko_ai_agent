package com.wenxi.neko_ai_agent.service;

import com.wenxi.neko_ai_agent.mapper.UserMapper;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static com.wenxi.neko_ai_agent.constant.UserConstant.USER_LOGIN_STATE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    private UserServiceImpl userService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpSession session;

    /**
     * 初始化被测服务。
     */
    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl();
        ReflectionTestUtils.setField(userService, "baseMapper", userMapper);
    }

    /**
     * 登录成功后 Session 中只保存脱敏用户，不能包含密码哈希。
     */
    @Test
    void userLoginShouldStoreSanitizedUserInSession() {
        User user = new User();
        user.setId(1L);
        user.setUserAccount("testuser");
        user.setUserPassword("hashed-password");
        user.setUserRole("user");
        when(userMapper.selectOne(any())).thenReturn(user);
        when(request.getSession()).thenReturn(session);

        userService.userLogin("testuser", "12345678", request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(session).setAttribute(eq(USER_LOGIN_STATE), captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals("user", captor.getValue().getUserRole());
        assertNull(captor.getValue().getUserPassword());
    }

    /**
     * 注册新用户时应初始化每日免费积分。
     */
    @Test
    void userRegisterShouldInitializeQuota() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.insert(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return 1;
        });

        long userId = userService.userRegister("tester", null, "testuser",
                "12345678", "12345678");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        User user = captor.getValue();
        assertEquals(1L, userId);
        assertEquals(100, user.getDailyQuota());
        assertEquals(0, user.getBonusQuota());
        assertNotNull(user.getQuotaResetDate());
    }
}
