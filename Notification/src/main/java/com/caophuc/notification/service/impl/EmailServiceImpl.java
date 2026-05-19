package com.caophuc.notification.service.impl;

import com.caophuc.notification.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender javaMailSender;

    @Value("${spring.mail.username}")
    private String senderEmail;

    @Override
    public void sendEmail(String to, String subject, String body) {
        try {
            log.info("Bắt đầu gửi email tới: {}", to);
            
            // Sử dụng MimeMessage để hỗ trợ gửi Email định dạng HTML (có sẵn mẫu)
            MimeMessage mimeMessage = javaMailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");
            
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            
            // Tham số 'true' ở đây chỉ định rằng nội dung (body) là định dạng HTML
            helper.setText(body, true);

            javaMailSender.send(mimeMessage);
            log.info("Đã gửi email thành công tới: {}", to);
        } catch (Exception e) {
            log.error("Lỗi khi gửi email tới: {}. Lỗi: {}", to, e.getMessage());
        }
    }
}
