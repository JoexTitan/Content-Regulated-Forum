package com.springboot.blog.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.beans.factory.annotation.Value;

@Configuration
@EnableScheduling
public class MailConfig {
    @Value("${spring.mail.port}")
    private int mailPort;
    @Value("${spring.mail.host}")
    private String mailHost;
    @Value("${spring.mail.username}")
    private String mailUsername;
    @Value("${spring.mail.password}")
    private String mailPassword;

    @Bean
    public JavaMailSender javaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setPort(mailPort);
        mailSender.setHost(mailHost);
        mailSender.setUsername(mailUsername);
        mailSender.setPassword(mailPassword);
        return mailSender;
    }
}
