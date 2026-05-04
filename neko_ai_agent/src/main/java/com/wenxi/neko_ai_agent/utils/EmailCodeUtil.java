package com.wenxi.neko_ai_agent.utils;

/**
 * 验证码工具类
 */
public class EmailCodeUtil {

    /**
     * 随机验证码生成工具
     * @return
     */
    public static String generateCode() {
        return String.valueOf((int) ((Math.random() * 9 + 1) * 100000)); // 6位数字
    }

    /**
     * 校验邮箱工具
     * @param userEmail
     * @return
     */
    public static boolean isValidEmail(String userEmail) {
        return userEmail != null && userEmail.matches("^[\\w.-]+@([\\w-]+\\.)+[\\w-]{2,}$");
    }
}

