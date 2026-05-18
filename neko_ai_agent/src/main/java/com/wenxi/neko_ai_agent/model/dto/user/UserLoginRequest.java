package com.wenxi.neko_ai_agent.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.io.Serializable;

/**
 * 用户登录请求体
 */
@Data
public class UserLoginRequest implements Serializable {

    private static final long serialVersionUID = 672359853938040518L;

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

}
