package org.ttt.safevaultbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;


import java.util.Properties;

/**
 * 邮件配置
 * 配置阿里云邮件推送 SMTP 服务（使用 STARTTLS 端口 25）
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host}")
    private String host;

    @Value("${spring.mail.port}")
    private int port;

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean starttlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private boolean starttlsRequired;

    @Value("${spring.mail.properties.mail.smtp.connectiontimeout:10000}")
    private int connectionTimeout;

    @Value("${spring.mail.properties.mail.smtp.timeout:10000}")
    private int timeout;

    @Value("${spring.mail.properties.mail.smtp.writetimeout:10000}")
    private int writeTimeout;

    @Value("${spring.mail.properties.mail.debug:false}")
    private boolean mailDebug;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        // 基础配置
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        // 使用标准 SMTP 协议
        mailSender.setProtocol("smtp");

        // SMTP 属性配置
        Properties props = mailSender.getJavaMailProperties();

        // 认证配置
        props.put("mail.smtp.auth", String.valueOf(auth));

        // STARTTLS 配置（端口 25 使用 STARTTLS）
        props.put("mail.smtp.starttls.enable", String.valueOf(starttlsEnable));
        props.put("mail.smtp.starttls.required", String.valueOf(starttlsRequired));

        // SSL 配置
        props.put("mail.smtp.ssl.checkserveridentity", "false");
        props.put("mail.smtp.ssl.trust", "*");

        // 超时配置（毫秒）
        props.put("mail.smtp.connectiontimeout", String.valueOf(connectionTimeout));
        props.put("mail.smtp.timeout", String.valueOf(timeout));
        props.put("mail.smtp.writetimeout", String.valueOf(writeTimeout));

        // 调试模式
        props.put("mail.debug", String.valueOf(mailDebug));

        // 编码配置
        props.put("mail.mime.charset", "UTF-8");
        props.put("mail.mime.encodeutf8", "true");

        return mailSender;
    }
}
