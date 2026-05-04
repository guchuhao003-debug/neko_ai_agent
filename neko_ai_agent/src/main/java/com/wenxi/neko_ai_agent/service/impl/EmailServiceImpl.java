package com.wenxi.neko_ai_agent.service.impl;


import com.wenxi.neko_ai_agent.exception.BusinessException;
import com.wenxi.neko_ai_agent.exception.ErrorCode;
import com.wenxi.neko_ai_agent.service.EmailService;
import com.wenxi.neko_ai_agent.utils.EmailCodeUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;


@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    @Resource
    private JavaMailSender mailSender;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final long CODE_EXPIRE_SECONDS = 300; // 5分钟

    private static final long COOLDOWN_SECONDS = 60;     // 冷却时间：60秒

    private static final String EMAIL_CODE_KEY_PREFIX = "email_login_code:";
    private static final String COOLDOWN_KEY_PREFIX = "email_code_cooldown:";

    // 发送者邮箱
    @Value("${spring.mail.username}")
    private String from;


    /**
     * 【异步发送邮件】使用 CompletableFuture 实现真正的异步，避免同类调用 @Async 不生效问题
     */
    private void sendEmailCodeAsync(String userEmail, String code) {
        CompletableFuture.runAsync(() -> {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom("Neko AI Agent 智能体平台 <" + from + ">");
                message.setTo(userEmail);
                message.setSubject("【Neko AI Agent】登录验证码");
                message.setText("【Neko AI Agent】验证码 " + code + " 用于 Neko AI Agent 智能体平台身份验证，5分钟内有效，请勿泄露或转发。如非本人操作，请忽略此邮件。");
                mailSender.send(message);
                log.info("验证码邮件发送成功: {}", userEmail);
            } catch (Exception e) {
                log.error("异步发送验证码邮件失败，邮箱: {}", userEmail, e);
            }
        });
    }

    /**
     * 【同步入口】供 Controller 调用：立即返回，内部触发异步发送
     */
    @Override
    public void sendEmailCode(String userEmail){
        // 先校验邮箱
        validEmail(userEmail);
        // 再校验频率
        checkCooldown(userEmail);

        String code = EmailCodeUtil.generateCode();
        String redisKey = EMAIL_CODE_KEY_PREFIX + userEmail;
        String cooldownKey = COOLDOWN_KEY_PREFIX + userEmail;

        // 存入 redis，5分钟过期
        redisTemplate.opsForValue().set(redisKey, code, Duration.ofSeconds(CODE_EXPIRE_SECONDS));
        // 标记冷却（防止 1分钟 内重复发送）
        redisTemplate.opsForValue().set(cooldownKey, "1", Duration.ofSeconds(COOLDOWN_SECONDS));
        // 调用异步发送邮件
        sendEmailCodeAsync(userEmail, code);
    }

    /**
     * 校验验证码
     * @param userEmail
     * @param inputCode
     * @return
     */
    @Override
    public boolean verifyCode(String userEmail, String inputCode){
        if(userEmail == null || inputCode == null){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱或验证码不可为空");
        }
        String redisKey = EMAIL_CODE_KEY_PREFIX + userEmail;
        String correctCode = redisTemplate.opsForValue().get(redisKey);
        if(correctCode == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR,"验证码已过期或不存在");
        }
        if(correctCode.equals(inputCode)){
            // 验证成功，需要删除验证码（一次性使用）
            redisTemplate.delete(redisKey);
            return true;
        }
        return false;
    }

    /**
     * 校验邮箱格式工具
     */
    private void validEmail(String userEmail) {
        if(userEmail == null || !userEmail.matches("^[\\w.-]+@([\\w-]+\\.)+[\\w-]{2,}$")){
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "邮箱格式不正确");
        }
    }

    /**
     * 校验冷却
     * @param userEmail
     */
    private void checkCooldown(String userEmail) {
        String cooldownKey = COOLDOWN_KEY_PREFIX + userEmail;
        Boolean hasKey = redisTemplate.hasKey(cooldownKey);
        if(Boolean.TRUE.equals(hasKey)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR,"操作过于频繁，请60秒后再试!");
        }
    }

}
