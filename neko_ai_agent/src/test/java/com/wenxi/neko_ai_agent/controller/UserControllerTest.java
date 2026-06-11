package com.wenxi.neko_ai_agent.controller;

import com.wenxi.neko_ai_agent.annotation.AuthCheck;
import com.wenxi.neko_ai_agent.constant.UserConstant;
import com.wenxi.neko_ai_agent.exception.BaseResponse;
import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import com.wenxi.neko_ai_agent.model.dto.user.UserUpdateRequest;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.model.vo.UserVO;
import com.wenxi.neko_ai_agent.service.EmailService;
import com.wenxi.neko_ai_agent.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 用户控制器单元测试。
 */
@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private UserController userController;

    @Mock
    private UserService userService;

    @Mock
    private EmailService emailService;

    @Mock
    private HttpServletRequest request;

    /**
     * 初始化被测控制器。
     */
    @BeforeEach
    void setUp() {
        userController = new UserController();
        ReflectionTestUtils.setField(userController, "userService", userService);
        ReflectionTestUtils.setField(userController, "emailService", emailService);
    }

    /**
     * 通用更新接口必须要求登录。
     */
    @Test
    void globalUpdateUserShouldRejectUnauthenticatedUser() {
        UserUpdateRequest updateRequest = buildUpdateRequest(1L);
        when(userService.getLoginUser(request)).thenThrow(
                new BusinessException(ErrorCode.NOT_LOGIN_ERROR));

        assertThrows(BusinessException.class,
                () -> userController.globalUpdateUser(updateRequest, request));
        verify(userService, never()).updateById(any(User.class));
    }

    /**
     * 通用更新接口只能修改当前登录用户。
     */
    @Test
    void globalUpdateUserShouldRejectUpdatingOtherUser() {
        UserUpdateRequest updateRequest = buildUpdateRequest(2L);
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(request)).thenReturn(loginUser);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userController.globalUpdateUser(updateRequest, request));

        assertEquals(ErrorCode.NO_AUTH_ERROR.getCode(), exception.getCode());
        verify(userService, never()).updateById(any(User.class));
    }

    /**
     * 通用更新接口不允许通过 userRole 提权。
     */
    @Test
    void globalUpdateUserShouldIgnoreUserRole() {
        UserUpdateRequest updateRequest = buildUpdateRequest(1L);
        updateRequest.setUserRole("admin");
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(userService.updateById(any(User.class))).thenReturn(true);

        BaseResponse<Boolean> response = userController.globalUpdateUser(updateRequest, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertTrue(response.getData());
        assertEquals(1L, updatedUser.getId());
        assertEquals("新昵称", updatedUser.getUserName());
        assertNull(updatedUser.getUserRole());
        assertNull(updatedUser.getUserPassword());
    }

    /**
     * 用户 VO 查询接口必须显式要求管理员权限，不能依赖内部方法调用。
     */
    @Test
    void getUserVOByIdShouldRequireAdminAuth() throws NoSuchMethodException {
        Method method = UserController.class.getDeclaredMethod("getUserVOById", long.class);

        AuthCheck authCheck = method.getAnnotation(AuthCheck.class);

        assertNotNull(authCheck);
        assertEquals(UserConstant.ADMIN_ROLE, authCheck.mustRole());
    }

    /**
     * 管理员按 ID 查询用户时只返回脱敏 VO，不能暴露密码哈希。
     */
    @Test
    void getUserByIdShouldReturnSanitizedUserVO() {
        User user = new User();
        user.setId(1L);
        user.setUserPassword("hashed-password");
        UserVO userVO = new UserVO();
        userVO.setId(1L);
        userVO.setUserRole("admin");
        when(userService.getById(1L)).thenReturn(user);
        when(userService.getUserVO(user)).thenReturn(userVO);

        BaseResponse<UserVO> response = userController.getUserById(1L);

        assertEquals(1L, response.getData().getId());
        verify(userService).getUserVO(user);
    }

    /**
     * 管理员更新用户时使用白名单字段，避免 BeanUtils 复制敏感字段。
     */
    @Test
    void updateUserShouldUseFieldWhitelist() {
        UserUpdateRequest updateRequest = buildUpdateRequest(1L);
        updateRequest.setUserRole("admin");
        when(userService.updateById(any(User.class))).thenReturn(true);

        BaseResponse<Boolean> response = userController.updateUser(updateRequest);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userService).updateById(userCaptor.capture());
        User updatedUser = userCaptor.getValue();
        assertTrue(response.getData());
        assertEquals(1L, updatedUser.getId());
        assertEquals("admin", updatedUser.getUserRole());
        assertNull(updatedUser.getUserPassword());
        assertNull(updatedUser.getIsDelete());
    }

    /**
     * 邮箱登录的邮箱和验证码为空时应在调用验证码服务前失败。
     */
    @Test
    void userLoginByEmailShouldRejectBlankParamsBeforeVerifyCode() {
        assertThrows(BusinessException.class,
                () -> userController.userLoginByEmail("", "", request));

        verify(emailService, never()).verifyCode(anyString(), anyString());
    }

    /**
     * 构造更新请求。
     */
    private UserUpdateRequest buildUpdateRequest(Long userId) {
        UserUpdateRequest updateRequest = new UserUpdateRequest();
        updateRequest.setId(userId);
        updateRequest.setUserName("新昵称");
        updateRequest.setUserEmail("new@qq.com");
        updateRequest.setUserAvatar("https://example.com/avatar.png");
        updateRequest.setUserProfile("新的简介");
        return updateRequest;
    }
}
