package com.caophuc.notification.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

    @Mock
    private JavaMailSender javaMailSender;

    @InjectMocks
    private EmailServiceImpl emailService;

    @Captor
    private ArgumentCaptor<MimeMessage> mimeMessageCaptor;

    private final String senderEmail = "no-reply@goplay.com";
    private final String recipientEmail = "recipient@example.com";
    private final String subject = "Test Subject";
    private final String body = "<h1>Test Body</h1>";

    @BeforeEach
    void setUp() {
        // Sử dụng ReflectionTestUtils để set giá trị cho trường private được inject bằng @Value
        ReflectionTestUtils.setField(emailService, "senderEmail", senderEmail);
    }

    @Test
    @DisplayName("Should send email successfully")
    void sendEmail_Success() throws MessagingException, IOException {
        // Given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // When
        emailService.sendEmail(recipientEmail, subject, body);

        // Then
        // 1. Xác minh rằng javaMailSender.send() được gọi đúng 1 lần
        verify(javaMailSender, times(1)).send(mimeMessageCaptor.capture());

        // 2. Lấy MimeMessage đã được gửi và kiểm tra nội dung
        MimeMessage capturedMessage = mimeMessageCaptor.getValue();

        assertEquals(senderEmail, capturedMessage.getFrom()[0].toString());
        assertEquals(recipientEmail, capturedMessage.getAllRecipients()[0].toString());
        assertEquals(subject, capturedMessage.getSubject());

        // --- SỬA LỖI KIỂM TRA NỘI DUNG EMAIL ---
        // getContent().toString() không đáng tin cậy.
        // Thay vào đó, chúng ta ghi message vào một stream và kiểm tra nội dung của stream đó.
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        capturedMessage.writeTo(outputStream);
        String emailContent = outputStream.toString();

        assertTrue(emailContent.contains(body));
        // MimeMessageHelper thêm encoding vào content type, nên ta chỉ cần kiểm tra `contains`
        assertTrue(capturedMessage.getContentType().contains("text/html"));
    }

    @Test
    @DisplayName("Should handle exception gracefully when sending email fails")
    void sendEmail_Failure_ShouldNotThrowException() {
        // Given
        MimeMessage mimeMessage = new MimeMessage((Session) null);
        when(javaMailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Mock phương thức send() để ném ra một ngoại lệ khi được gọi
        doThrow(new MailSendException("Failed to send email")).when(javaMailSender).send(any(MimeMessage.class));

        // When & Then
        // Xác minh rằng phương thức sendEmail() không ném ra bất kỳ ngoại lệ nào
        assertDoesNotThrow(() -> emailService.sendEmail(recipientEmail, subject, body));

        // (Optional) Xác minh rằng lời gọi send() đã thực sự được thực hiện
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }
}
