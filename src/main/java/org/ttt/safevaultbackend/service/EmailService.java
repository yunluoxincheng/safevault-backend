package org.ttt.safevaultbackend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import jakarta.mail.internet.MimeMessage;

import java.time.LocalDateTime;

/**
 * 邮件服务
 * 使用阿里云 SMTP 服务直接发送验证邮件
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.from:noreply@safevault.com}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080/api}")
    private String baseUrl;

    /**
     * 发送邮箱验证邮件（HTML 格式）
     *
     * @param toEmail         收件人邮箱
     * @param verificationUrl 验证链接（safevault://verify-email?token=xxx）
     * @return 是否发送成功
     */
    public boolean sendVerificationEmail(String toEmail, String verificationUrl) {
        try {
            log.info("Preparing to send verification email to: {}", toEmail);
            log.info("From email: {}", fromEmail);
            log.info("Verification URL: {}", verificationUrl);

            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("验证您的 SafeVault 邮箱");

            // 构建 HTML 邮件内容
            String htmlContent = buildVerificationEmailHtml(toEmail, verificationUrl);
            helper.setText(htmlContent, true);

            log.info("Sending email with MIME message...");

            // 发送邮件
            javaMailSender.send(mimeMessage);

            log.info("Verification email sent successfully to: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("Failed to send verification email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送纯文本验证邮件（备用方案）
     *
     * @param toEmail         收件人邮箱
     * @param verificationUrl 验证链接
     * @return 是否发送成功
     */
    public boolean sendVerificationEmailText(String toEmail, String verificationUrl) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("验证您的 SafeVault 邮箱");

            String textContent = buildVerificationEmailText(toEmail, verificationUrl);
            message.setText(textContent);

            javaMailSender.send(message);

            log.info("Text verification email sent to: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("Failed to send text verification email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 发送密码重置邮件
     *
     * @param toEmail  收件人邮箱
     * @param resetUrl 重置链接
     * @return 是否发送成功
     */
    public boolean sendPasswordResetEmail(String toEmail, String resetUrl) {
        try {
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("重置您的 SafeVault 密码");

            String htmlContent = buildPasswordResetEmailHtml(toEmail, resetUrl);
            helper.setText(htmlContent, true);

            javaMailSender.send(mimeMessage);

            log.info("Password reset email sent to: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage(), e);
            return false;
        }
    }

    /**
     * 构建验证邮件 HTML 内容
     */
    private String buildVerificationEmailHtml(String email, String verificationUrl) {
        String html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>验证您的邮箱</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
                        background-color: #f5f5f5;
                        margin: 0;
                        padding: 0;
                        line-height: 1.6;
                    }
                    .container {
                        max-width: 600px;
                        margin: 40px auto;
                        background-color: #ffffff;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%);
                        padding: 30px;
                        text-align: center;
                    }
                    .header h1 {
                        color: #ffffff;
                        margin: 0;
                        font-size: 24px;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .greeting {
                        font-size: 18px;
                        color: #333333;
                        margin-bottom: 20px;
                    }
                    .message {
                        color: #666666;
                        margin-bottom: 30px;
                    }
                    .button-container {
                        text-align: center;
                        margin: 30px 0;
                    }
                    .verify-button {
                        display: inline-block;
                        padding: 14px 40px;
                        background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%);
                        color: #ffffff;
                        text-decoration: none;
                        border-radius: 6px;
                        font-size: 16px;
                        font-weight: 500;
                    }
                    .verify-button:hover {
                        opacity: 0.9;
                    }
                    .link-text {
                        text-align: center;
                        color: #999999;
                        font-size: 12px;
                        word-break: break-all;
                        margin-top: 20px;
                    }
                    .footer {
                        background-color: #f9f9f9;
                        padding: 20px 30px;
                        text-align: center;
                        color: #999999;
                        font-size: 12px;
                    }
                    .warning {
                        background-color: #fff3cd;
                        border-left: 4px solid #ffc107;
                        padding: 15px;
                        margin: 20px 0;
                        color: #856404;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 SafeVault</h1>
                    </div>
                    <div class="content">
                        <p class="greeting">您好，</p>
                        <p class="message">
                            感谢您注册 SafeVault！请点击下方按钮验证您的邮箱地址：
                        </p>
                        <div class="button-container">
                            <a href="%s" class="verify-button">验证邮箱</a>
                        </div>
                        <p class="message">
                            或者复制以下链接到浏览器中打开：
                        </p>
                        <div class="link-text">%s</div>
                        <div class="warning">
                            ⚠️ 此验证链接将在 10 分钟后失效，请尽快完成验证。
                        </div>
                        <p class="message">
                            如果这不是您的操作，请忽略此邮件。
                        </p>
                    </div>
                    <div class="footer">
                        <p>此邮件由 SafeVault 系统自动发送，请勿直接回复。</p>
                        <p>%s</p>
                    </div>
                </div>
            </body>
            </html>
            """;
        return String.format(html, verificationUrl, verificationUrl, LocalDateTime.now());
    }

    /**
     * 构建验证邮件纯文本内容
     */
    private String buildVerificationEmailText(String email, String verificationUrl) {
        String text = """
            SafeVault - 验证您的邮箱
            ======================================

            您好，

            感谢您注册 SafeVault！请点击以下链接验证您的邮箱地址：

            %s

            如果按钮无法点击，请复制上面的链接到浏览器中打开。

            ⚠️ 此验证链接将在 10 分钟后失效，请尽快完成验证。

            如果这不是您的操作，请忽略此邮件。

            ======================================
            此邮件由 SafeVault 系统自动发送，请勿直接回复。
            时间: %s
            """;
        return String.format(text, verificationUrl, LocalDateTime.now());
    }

    /**
     * 构建密码重置邮件 HTML 内容
     */
    private String buildPasswordResetEmailHtml(String email, String resetUrl) {
        String html = """
            <!DOCTYPE html>
            <html lang="zh-CN">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>重置您的密码</title>
                <style>
                    body {
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        background-color: #f5f5f5;
                        margin: 0;
                        padding: 0;
                    }
                    .container {
                        max-width: 600px;
                        margin: 40px auto;
                        background-color: #ffffff;
                        border-radius: 8px;
                        overflow: hidden;
                        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
                    }
                    .header {
                        background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%);
                        padding: 30px;
                        text-align: center;
                    }
                    .header h1 {
                        color: #ffffff;
                        margin: 0;
                        font-size: 24px;
                    }
                    .content {
                        padding: 40px 30px;
                    }
                    .button-container {
                        text-align: center;
                        margin: 30px 0;
                    }
                    .reset-button {
                        display: inline-block;
                        padding: 14px 40px;
                        background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 100%%);
                        color: #ffffff;
                        text-decoration: none;
                        border-radius: 6px;
                        font-size: 16px;
                        font-weight: 500;
                    }
                    .warning {
                        background-color: #fff3cd;
                        border-left: 4px solid #ffc107;
                        padding: 15px;
                        margin: 20px 0;
                        color: #856404;
                    }
                    .footer {
                        background-color: #f9f9f9;
                        padding: 20px 30px;
                        text-align: center;
                        color: #999999;
                        font-size: 12px;
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h1>🔐 SafeVault</h1>
                    </div>
                    <div class="content">
                        <p>您好，</p>
                        <p>我们收到了您的密码重置请求。请点击下方按钮重置密码：</p>
                        <div class="button-container">
                            <a href="%s" class="reset-button">重置密码</a>
                        </div>
                        <div class="warning">
                            ⚠️ 此重置链接将在 30 分钟后失效。
                        </div>
                        <p>如果这不是您的操作，请忽略此邮件并保持密码安全。</p>
                    </div>
                    <div class="footer">
                        <p>此邮件由 SafeVault 系统自动发送，请勿直接回复。</p>
                        <p>%s</p>
                    </div>
                </div>
            </body>
            </html>
            """;
        return String.format(html, resetUrl, LocalDateTime.now());
    }
}
