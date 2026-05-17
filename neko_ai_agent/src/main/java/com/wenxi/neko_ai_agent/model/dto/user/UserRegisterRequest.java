package com.wenxi.neko_ai_agent.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求体
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 672359853938040518L;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 邮箱
     */
    private String userEmail;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;
}
