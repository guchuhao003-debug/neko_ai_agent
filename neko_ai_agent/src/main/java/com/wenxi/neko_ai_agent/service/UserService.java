package com.wenxi.neko_ai_agent.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.wenxi.neko_ai_agent.model.dto.UserQueryRequest;
import com.wenxi.neko_ai_agent.model.entity.User;
import com.wenxi.neko_ai_agent.model.vo.LoginUserVO;
import com.wenxi.neko_ai_agent.model.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
* @author kk
* @description 针对表【user(用户)】的数据库操作Service
* @createDate 2026-05-03 18:26:25
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     * @param userName
     * @param userAccount
     * @param userPassword
     * @param checkPassword
     * @return
     */
    long userRegister(String userName, String userEmail, String userAccount, String userPassword, String checkPassword);


    /**
     * 获取加密后的密码
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 用户登录
     * @param userAccount
     * @param userPassword
     * @param request
     * @return
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 用户注销（退出登录）
     * @param request
     * @return
     */
    Boolean userLogout(HttpServletRequest request);

    /**
     * 获取当前登录用户
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取脱敏后的用户
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后的用户列表
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 获取查询条件
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 获取已脱敏后的登录用户信息
     * @param user
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 邮箱登录
     * @param userEmail
     * @param request
     * @return
     */
    LoginUserVO userLoginByEmail(String userEmail, HttpServletRequest request);

}
