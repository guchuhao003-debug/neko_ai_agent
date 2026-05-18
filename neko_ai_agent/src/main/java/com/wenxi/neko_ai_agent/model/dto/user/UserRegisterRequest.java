package com.wenxi.neko_ai_agent.model.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "用户名不能为空")
    @Size(max = 10, message = "用户名不能超过10个字符")
    private String userName;

    /**
     * 邮箱（选填）
     */
    @Email(message = "邮箱格式不正确")
    private String userEmail;

    /**
     * 账号
     */
    @NotBlank(message = "账号不能为空")
    @Size(min = 4, message = "账号长度不能小于4位")
    private String userAccount;

    /**
     * 密码
     */
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码长度不能小于8位")
    private String userPassword;

    /**
     * 确认密码
     */
    @NotBlank(message = "确认密码不能为空")
    @Size(min = 8, message = "确认密码长度不能小于8位")
    private String checkPassword;
}
