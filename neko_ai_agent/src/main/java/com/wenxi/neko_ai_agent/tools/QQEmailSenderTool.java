package com.wenxi.neko_ai_agent.tools;

import cn.hutool.core.util.StrUtil;
import com.wenxi.neko_ai_agent.utils.GeneratedFileUtils;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;

import java.nio.file.Path;

/**
 * QQ 邮箱邮件发送工具。
 */
@Slf4j
public class QQEmailSenderTool {

    private static final String SENDER_DISPLAY_NAME = "Neko AI Agent 智能体平台";

    private final JavaMailSender mailSender;

    private final String fromEmail;

    /**
     * 创建邮件发送工具，SMTP 服务器、端口和授权码由 spring.mail 配置提供。
     *
     * @param mailSender Spring 邮件发送器
     * @param fromEmail 发件邮箱
     */
    public QQEmailSenderTool(JavaMailSender mailSender, String fromEmail) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    /**
     * 发送纯文本邮件。
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件正文
     * @return 工具执行结果
     */
    @Tool(description = "Send a plain text email using the configured SMTP mail account.")
    public String sendTextEmail(
            @ToolParam(description = "Recipient's email address") String to,
            @ToolParam(description = "Email subject line") String subject,
            @ToolParam(description = "Plain text content of the email") String content) {
        try {
            validateEmailConfig();
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(buildFromAddress());
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);
            log.info("文本邮件发送成功，收件人：{}", to);
            return "Email sent successfully to " + to;
        } catch (MailAuthenticationException e) {
            log.warn("文本邮件 SMTP 认证失败，收件人：{}", to, e);
            return buildAuthenticationFailureMessage();
        } catch (Exception e) {
            log.warn("文本邮件发送失败，收件人：{}", to, e);
            return "Error sending email: " + e.getMessage();
        }
    }

    /**
     * 发送 HTML 格式邮件。
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param htmlContent HTML 邮件正文
     * @return 工具执行结果
     */
    @Tool(description = "Send an HTML email using the configured SMTP mail account.")
    public String sendHtmlEmail(
            @ToolParam(description = "Recipient's email address") String to,
            @ToolParam(description = "Email subject line") String subject,
            @ToolParam(description = "HTML content of the email") String htmlContent) {
        try {
            validateEmailConfig();
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(buildFromAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("HTML 邮件发送成功，收件人：{}", to);
            return "HTML email sent successfully to " + to;
        } catch (MailAuthenticationException e) {
            log.warn("HTML 邮件 SMTP 认证失败，收件人：{}", to, e);
            return buildAuthenticationFailureMessage();
        } catch (MessagingException e) {
            log.warn("HTML 邮件构建失败，收件人：{}", to, e);
            return "Error building HTML email: " + e.getMessage();
        } catch (Exception e) {
            log.warn("HTML 邮件发送失败，收件人：{}", to, e);
            return "Error sending HTML email: " + e.getMessage();
        }
    }

    /**
     * 发送带已有生成文件附件的邮件。
     *
     * @param to 收件人邮箱
     * @param subject 邮件主题
     * @param content 邮件正文
     * @param fileReference 文件名、文件访问链接或文件保存路径
     * @return 工具执行结果
     */
    @Tool(description = """
            Send an email with an existing generated file attachment. Use this when the user asks
            to email a previously generated PDF, markdown, downloaded file, or file link.
            The fileReference can be a bare filename, an /api/files/... URL, or a saved path.
            """)
    public String sendEmailWithAttachment(
            @ToolParam(description = "Recipient's email address") String to,
            @ToolParam(description = "Email subject line") String subject,
            @ToolParam(description = "Plain text content of the email") String content,
            @ToolParam(description = "Generated file name, access URL, or saved path")
            String fileReference) {
        try {
            validateEmailConfig();
            Path filePath = GeneratedFileUtils.resolveExistingFilePath(fileReference);
            if (filePath == null) {
                return "Error sending attachment email: file not found - " + fileReference;
            }

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(buildFromAddress());
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(StrUtil.blankToDefault(content, ""), false);
            helper.addAttachment(filePath.getFileName().toString(), filePath.toFile());
            mailSender.send(message);
            log.info("附件邮件发送成功，收件人：{}，附件：{}", to, filePath);
            return "Attachment email sent successfully to " + to + " with file "
                    + filePath.getFileName();
        } catch (MailAuthenticationException e) {
            log.warn("附件邮件 SMTP 认证失败，收件人：{}", to, e);
            return buildAuthenticationFailureMessage();
        } catch (MessagingException e) {
            log.warn("附件邮件构建失败，收件人：{}", to, e);
            return "Error building attachment email: " + e.getMessage();
        } catch (Exception e) {
            log.warn("附件邮件发送失败，收件人：{}", to, e);
            return "Error sending attachment email: " + e.getMessage();
        }
    }

    /**
     * 校验邮件工具必需配置。
     */
    private void validateEmailConfig() {
        if (mailSender == null || StrUtil.isBlank(fromEmail)) {
            throw new IllegalStateException("邮件发送配置未完成，请检查 spring.mail 配置");
        }
    }

    /**
     * 构建发件人地址。
     *
     * @return 发件人地址
     */
    private String buildFromAddress() {
        return SENDER_DISPLAY_NAME + "<" + fromEmail + ">";
    }

    /**
     * 构建 SMTP 认证失败提示。
     *
     * @return 认证失败提示
     */
    private String buildAuthenticationFailureMessage() {
        return "邮件发送失败：SMTP 认证失败。请检查 spring.mail.username 是否为发件 QQ 邮箱，"
                + "spring.mail.password 是否为 QQ 邮箱 SMTP 授权码而不是登录密码，"
                + "并确认 QQ 邮箱已开启 POP3/SMTP 服务。";
    }
}
