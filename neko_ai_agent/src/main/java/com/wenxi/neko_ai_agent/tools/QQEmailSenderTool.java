package com.wenxi.neko_ai_agent.tools;


import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

import java.util.Properties;

/**
 * QQ 邮箱 -> 邮件发送工具
 */
@Slf4j
public class QQEmailSenderTool {


    /**
     * 邮箱
     */
    private static final String FROM_EMAIL = "935070021@qq.com";

    /**
     * 授权码
     */
    private static final String AUTH_CODE = "rvqitiwywhqpbehc";

    // QQ 邮箱 SMTP 配置
    private static final String SMTP_HOST = "smtp.qq.com";

    /**
     * 推荐使用 STARTTLS（端口号 587）,也可以使用 SSL（端口号 465）
     */
    private static final String SMTP_POST = "587";

    /**
     * 发送纯文本邮件
     * @param to
     * @param subject
     * @param content
     */
    @Tool(description = "Send an HTML formatted email using QQ Mail's SMTP service." +
    "Requires the recipient's email address, subject, and HTML content." +
    "The sender's QQ email address and SMTP authorization code must be pre-configured in the system.")
    public static void sendTextEmail(
            @ToolParam(description = "Recipient's email address") String to,
            @ToolParam(description = "Email subject line")String subject,
            @ToolParam(description = "HTML content of the email")String content) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_POST);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");  // 启用 STARTTLS 加密

        Session session = Session.getInstance(props,new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, AUTH_CODE);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipient(Message.RecipientType.TO,new InternetAddress(to));
        message.setSubject(subject);
        message.setText(content);

        Transport.send(message);
        log.info("✅ 文本邮件发送成功！收件人：{}", to);
    }

    /**
     * 发送 HTML 格式邮件
     * @param to
     * @param subject
     * @param htmlContent
     * @throws MessagingException
     */
    public static void sendHtmlEmail(String to, String subject, String htmlContent) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST);
        props.put("mail.smtp.port", SMTP_POST);
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");  // 启用 STARTTLS 加密

        Session session = Session.getInstance(props,new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, AUTH_CODE);
            }
        });

        Message message = new MimeMessage(session);
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipient(Message.RecipientType.TO,new InternetAddress(to));
        message.setSubject(subject);
        message.setContent(htmlContent, "text/html;charset=UTF-8");

        Transport.send(message);
        log.info("✅ HTML 邮件发送成功！收件人：{}", to);
    }



}
