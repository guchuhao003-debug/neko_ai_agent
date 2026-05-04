package com.wenxi.neko_ai_agent.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wenxi.neko_ai_agent.annotation.AuthCheck;
import com.wenxi.neko_ai_agent.common.DeleteRequest;
import com.wenxi.neko_ai_agent.common.ResultUtils;
import com.wenxi.neko_ai_agent.constant.UserConstant;
import com.wenxi.neko_ai_agent.exception.BaseResponse;
import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import com.wenxi.neko_ai_agent.exception.ThrowUtils;
import com.wenxi.neko_ai_agent.model.dto.*;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.model.vo.LoginUserVO;
import com.wenxi.neko_ai_agent.model.vo.UserVO;
import com.wenxi.neko_ai_agent.service.EmailService;
import com.wenxi.neko_ai_agent.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Resource
    private EmailService emailService;

    /**
     * 用户注册
     * @param userRegisterRequest
     * @return
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        // 1. 校验参数
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userName = userRegisterRequest.getUserName();
        String userEmail = userRegisterRequest.getUserEmail();
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        // 2. 调用注册服务
        long result = userService.userRegister(userName, userEmail,userAccount, userPassword, checkPassword);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     * @param userLoginRequest
     * @param request
     * @return
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 邮箱登录 -> 1、发送验证码接口
     *
     * @param userEmail
     * @return
     */
    @PostMapping("/send_code")
    public BaseResponse<String> sendCode(@RequestParam String userEmail) {
        // 校验参数
        ThrowUtils.throwIf(userEmail == null, ErrorCode.PARAMS_ERROR, "请求参数为空");
        // 发送验证码
        emailService.sendEmailCode(userEmail);
        return ResultUtils.success("验证码发送成功");
    }

    /**
     * 邮箱登录 -> 2、验证码登录接口
     * @param userEmail
     * @param inputCode
     * @param request
     * @return
     */
    @PostMapping("/login/email")
    public BaseResponse<LoginUserVO> userLoginByEmail(@RequestParam String userEmail, @RequestParam String inputCode, HttpServletRequest request){
        // 校验验证码是否正确
        boolean checkCodeResult = emailService.verifyCode(userEmail, inputCode);
        ThrowUtils.throwIf(!checkCodeResult,ErrorCode.NOT_FOUND_ERROR,"验证码错误或已过期");
        // 登录成功
        LoginUserVO loginUserVO = userService.userLoginByEmail(userEmail, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    @GetMapping("/get/current")
    public BaseResponse<LoginUserVO> getCurrentUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销登录
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        Boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    /**
     * 创建用户 （仅限管理员）
     * @param userAddRequest
     * @return
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)  // 管理员权限
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userAddRequest,  user);
        // 默认密码
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        // 插入数据库
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result,ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户 （管理员权限）
     * @param id
     * @return
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 仅限管理员权限
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取封装类 （脱敏后的用户信息）
     * @param id
     * @return
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户 （管理员权限）
     * @param deleteRequest
     * @return
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE) // 仅限管理员权限
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean result = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(result);
    }

    /**
     * 更新用户信息 （管理员权限）
     * @param userUpdateRequest
     * @return
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)  // 管理员权限
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 用户修改个人信息  (通用)
     *
     * @param userUpdateRequest 更新请求参数
     * @return
     */
    @PostMapping("/user/update")
    public BaseResponse<Boolean> GlobalUpdateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 分页查询用户列表 （管理员权限）
     *
     * @param userQueryRequest 查询请求参数
     * @return
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)  //仅限管理员可查看用户列表
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }


}
