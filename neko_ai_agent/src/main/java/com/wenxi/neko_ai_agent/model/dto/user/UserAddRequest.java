package com.wenxi.neko_ai_agent.model.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.io.Serializable;

/**
 * 添加用户请求体（管理员）
 */
@Data
public class UserAddRequest implements Serializable {

    /**
     * 用户昵称
     */
    @NotBlank(message = "用户名不能为空")
    private String userName;

    /**
     * 账号
     */
    @NotBlank(message = "账号不能为空")
    private String userAccount;

    /**
     * 用户头像
     */
    private String userAvatar;

    /**
     * 用户简介
     */
    private String userProfile;

    /**
     * 用户角色：user/admin
     */
    private String userRole;

    private static final long serialVersionUID = 1L;
}
