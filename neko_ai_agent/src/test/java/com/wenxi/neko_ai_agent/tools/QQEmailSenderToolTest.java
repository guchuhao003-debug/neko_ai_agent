package com.wenxi.neko_ai_agent.tools;

import com.wenxi.neko_ai_agent.utils.GeneratedFileUtils;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * QQ 邮箱发送工具单元测试。
 */
class QQEmailSenderToolTest {

    /**
     * 测试纯文本邮件使用配置注入的发件邮箱。
     */
    @Test
    void sendTextEmailShouldUseConfiguredSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        QQEmailSenderTool tool = new QQEmailSenderTool(mailSender, "sender@qq.com");

        String result = tool.sendTextEmail("receiver@qq.com", "测试邮件", "测试内容");

        ArgumentCaptor<SimpleMailMessage> messageCaptor =
                ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(messageCaptor.capture());
        SimpleMailMessage message = messageCaptor.getValue();
        assertEquals("Neko AI Agent 智能体平台<sender@qq.com>", message.getFrom());
        assertEquals("receiver@qq.com", message.getTo()[0]);
        assertEquals("测试邮件", message.getSubject());
        assertEquals("测试内容", message.getText());
        assertTrue(result.contains("Email sent successfully"));
    }

    /**
     * 测试 HTML 邮件通过 JavaMailSender 构建并发送。
     */
    @Test
    void sendHtmlEmailShouldUseJavaMailSender() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        QQEmailSenderTool tool = new QQEmailSenderTool(mailSender, "sender@qq.com");

        String result = tool.sendHtmlEmail(
                "receiver@qq.com",
                "HTML 测试邮件",
                "<h1>测试内容</h1>"
        );

        verify(mailSender).send(any(MimeMessage.class));
        assertTrue(result.contains("HTML email sent successfully"));
    }

    /**
     * 测试附件邮件会复用已经生成的文件，而不是要求重新生成文件。
     *
     * @throws Exception 邮件内容解析异常
     */
    @Test
    void sendEmailWithAttachmentShouldAttachExistingGeneratedFile() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        QQEmailSenderTool tool = new QQEmailSenderTool(mailSender, "sender@qq.com");
        String fileName = "mail-attachment-test.pdf";
        Path filePath = GeneratedFileUtils.buildCategoryDir(GeneratedFileUtils.PDF_CATEGORY)
                .resolve(fileName);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, "pdf content");

        try {
            String result = tool.sendEmailWithAttachment(
                    "receiver@qq.com",
                    "附件测试邮件",
                    "请查收附件",
                    fileName
            );

            verify(mailSender).send(any(MimeMessage.class));
            assertTrue(result.contains("Attachment email sent successfully"));
            assertTrue(mimeMessage.getContent() instanceof Multipart);
            Multipart multipart = (Multipart) mimeMessage.getContent();
            assertEquals(2, multipart.getCount());
            assertEquals(fileName, multipart.getBodyPart(1).getFileName());
        } finally {
            Files.deleteIfExists(filePath);
        }
    }

    /**
     * 测试 SMTP 认证失败时返回可操作的配置提示。
     */
    @Test
    void sendEmailWithAttachmentShouldExplainAuthenticationFailure() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailAuthenticationException("Authentication failed"))
                .when(mailSender).send(any(MimeMessage.class));
        QQEmailSenderTool tool = new QQEmailSenderTool(mailSender, "sender@qq.com");
        String fileName = "mail-auth-failed-test.pdf";
        Path filePath = GeneratedFileUtils.buildCategoryDir(GeneratedFileUtils.PDF_CATEGORY)
                .resolve(fileName);
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, "pdf content");

        try {
            String result = tool.sendEmailWithAttachment(
                    "receiver@qq.com",
                    "附件测试邮件",
                    "请查收附件",
                    fileName
            );

            assertTrue(result.contains("SMTP 认证失败"));
            assertTrue(result.contains("SMTP 授权码"));
        } finally {
            Files.deleteIfExists(filePath);
        }
    }

    /**
     * 测试缺少发件邮箱配置时返回明确错误。
     */
    @Test
    void sendTextEmailShouldReturnErrorWhenConfigMissing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        QQEmailSenderTool tool = new QQEmailSenderTool(mailSender, "");

        String result = tool.sendTextEmail("receiver@qq.com", "测试邮件", "测试内容");

        assertTrue(result.contains("邮件发送配置未完成"));
    }
}
