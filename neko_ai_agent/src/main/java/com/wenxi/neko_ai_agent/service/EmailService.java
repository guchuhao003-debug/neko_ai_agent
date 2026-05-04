package com.wenxi.neko_ai_agent.service;

public interface EmailService {

    /**
     * 发送验证码
     * @param userEmail
     */
    void sendEmailCode(String userEmail);

    /**
     * 校验验证码是否正确
     * @param userEmail
     * @param inputCode
     * @return
     */
    boolean verifyCode(String userEmail, String inputCode);



}
