package com.wenxi.neko_ai_agent.tools;

import org.junit.jupiter.api.Test;

import jakarta.mail.MessagingException;

import static org.junit.jupiter.api.Assertions.*;


class QQEmailSenderToolTest {

    @Test
    void sendTextEmail() {
        try {
            QQEmailSenderTool.sendTextEmail(
                    "2836143370@qq.com",
                    "测试邮件",
                    "这是一封通过 Java 发送的测试邮件。"
            );
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @Test
    void sendHtmlEmail() {
        try {
            String html = """
                <h2> 🎉 这是一封HTML测试邮件 </h2>
                <p>
                    <b>
                        <a href='https://codefather.cn'>这是一封通过 Java 发送的测试邮件。</a>
                    </b>
                </p>
                """;
            QQEmailSenderTool.sendHtmlEmail("2836143370@qq.com", "测试邮件", html);
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }
}
