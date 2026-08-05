package com.caophuc.notification.service.impl;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FirebasePushNotificationServiceImplTest {

    @InjectMocks
    private FirebasePushNotificationServiceImpl notificationService;

    // Mock cho đối tượng FirebaseMessaging
    private FirebaseMessaging firebaseMessagingMock;

    // MockedStatic dùng để mock các phương thức static
    private MockedStatic<FirebaseMessaging> firebaseMessagingStaticMock;

    @Captor
    private ArgumentCaptor<Message> messageCaptor;

    private final String fcmToken = "test-fcm-token";
    private final String title = "Test Title";
    private final String body = "Test Body";

    @BeforeEach
    void setUp() {
        // Tạo mock cho FirebaseMessaging
        firebaseMessagingMock = mock(FirebaseMessaging.class);
        // Bắt đầu mock phương thức static `getInstance()`
        firebaseMessagingStaticMock = mockStatic(FirebaseMessaging.class);
        // Khi `FirebaseMessaging.getInstance()` được gọi, trả về mock của chúng ta
        firebaseMessagingStaticMock.when(FirebaseMessaging::getInstance).thenReturn(firebaseMessagingMock);
    }

    @AfterEach
    void tearDown() {
        // Rất quan trọng: phải đóng static mock sau mỗi test để tránh ảnh hưởng đến các test khác
        firebaseMessagingStaticMock.close();
    }

    @Test
    @DisplayName("Should send push notification successfully")
    void sendPushNotification_Success() throws FirebaseMessagingException, NoSuchFieldException, IllegalAccessException {
        // Given
        String mockResponse = "projects/my-project/messages/12345";
        when(firebaseMessagingMock.send(any(Message.class))).thenReturn(mockResponse);

        // When
        notificationService.sendPushNotification(fcmToken, title, body);

        // Then
        // 1. Xác minh rằng phương thức send() được gọi đúng 1 lần
        verify(firebaseMessagingMock, times(1)).send(messageCaptor.capture());

        // 2. Lấy Message đã được gửi và kiểm tra nội dung
        Message capturedMessage = messageCaptor.getValue();

        // --- SỬ DỤNG REFLECTION ĐỂ TRUY CẬP CÁC TRƯỜNG PRIVATE ---

        // Lấy trường 'token' từ Message
        Field tokenField = Message.class.getDeclaredField("token");
        tokenField.setAccessible(true);
        String actualToken = (String) tokenField.get(capturedMessage);
        assertEquals(fcmToken, actualToken);

        // Lấy trường 'notification' từ Message
        Field notificationField = Message.class.getDeclaredField("notification");
        notificationField.setAccessible(true);
        Notification actualNotification = (Notification) notificationField.get(capturedMessage);
        assertNotNull(actualNotification);

        // Lấy trường 'title' từ Notification
        Field titleField = Notification.class.getDeclaredField("title");
        titleField.setAccessible(true);
        String actualTitle = (String) titleField.get(actualNotification);
        assertEquals(title, actualTitle);

        // Lấy trường 'body' từ Notification
        Field bodyField = Notification.class.getDeclaredField("body");
        bodyField.setAccessible(true);
        String actualBody = (String) bodyField.get(actualNotification);
        assertEquals(body, actualBody);
    }

    @Test
    @DisplayName("Should handle exception gracefully when sending push notification fails")
    void sendPushNotification_Failure_ShouldNotThrowException() throws FirebaseMessagingException {
        // Given
        // Mock phương thức send() để ném ra một ngoại lệ
        when(firebaseMessagingMock.send(any(Message.class))).thenThrow(mock(FirebaseMessagingException.class));

        // When & Then
        // Xác minh rằng phương thức không ném ra bất kỳ ngoại lệ nào
        assertDoesNotThrow(() -> notificationService.sendPushNotification(fcmToken, title, body));

        // (Optional) Xác minh rằng lời gọi send() đã thực sự được thực hiện
        verify(firebaseMessagingMock, times(1)).send(any(Message.class));
    }
}
