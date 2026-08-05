package com.caophuc.notification.listener;

import com.caophuc.notification.dto.NotificationEventDto;
import com.caophuc.notification.model.Notification;
import com.caophuc.notification.repository.NotificationRepository;
import com.caophuc.notification.service.EmailService;
import com.caophuc.notification.service.FirebasePushNotificationService;
import com.caophuc.notification.util.NotificationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationListenerTest {

    @Mock
    private EmailService emailService;

    @Mock
    private FirebasePushNotificationService pushNotificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @InjectMocks
    private NotificationListener notificationListener;

    @Captor
    private ArgumentCaptor<Notification> notificationCaptor;

    private NotificationEventDto eventDto;

    @BeforeEach
    void setUp() {
        eventDto = NotificationEventDto.builder()
                .userId(1)
                .userEmail("test@example.com")
                .fcmToken("fcm-token-123")
                .title("Test Title")
                .content("Test Content")
                .type("BOOKING_CREATED")
                .build();
    }

    @Test
    @DisplayName("Happy Path: Should process event with both email and FCM token")
    void consumeNotificationEvent_FullEvent_ShouldCallAllServices() {
        // When
        notificationListener.consumeNotificationEvent(eventDto);

        // Then
        // 1. Verify repository save is called
        verify(notificationRepository, times(1)).save(notificationCaptor.capture());
        Notification savedNotification = notificationCaptor.getValue();

        // 2. Assert the captured notification is correct
        assertEquals(eventDto.getUserId(), savedNotification.getUser());
        assertEquals(eventDto.getTitle(), savedNotification.getTitle());
        assertEquals(eventDto.getContent(), savedNotification.getContent());
        assertEquals(NotificationType.BOOKING_CREATED, savedNotification.getType());
        assertFalse(savedNotification.isRead());

        // 3. Verify email service is called
        verify(emailService, times(1)).sendEmail(
                eventDto.getUserEmail(),
                eventDto.getTitle(),
                eventDto.getContent()
        );

        // 4. Verify push notification service is called
        verify(pushNotificationService, times(1)).sendPushNotification(
                eventDto.getFcmToken(),
                eventDto.getTitle(),
                eventDto.getContent()
        );
    }

    @Test
    @DisplayName("Should process event with only email")
    void consumeNotificationEvent_EmailOnly_ShouldCallEmailService() {
        // Given
        eventDto.setFcmToken(null);

        // When
        notificationListener.consumeNotificationEvent(eventDto);

        // Then
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
        verify(pushNotificationService, never()).sendPushNotification(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should process event with only FCM token")
    void consumeNotificationEvent_FcmOnly_ShouldCallPushService() {
        // Given
        eventDto.setUserEmail(null);

        // When
        notificationListener.consumeNotificationEvent(eventDto);

        // Then
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(pushNotificationService, times(1)).sendPushNotification(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should process event with no email or FCM token")
    void consumeNotificationEvent_NoContactInfo_ShouldOnlySaveToDb() {
        // Given
        eventDto.setUserEmail(" "); // Test with blank string
        eventDto.setFcmToken(null);

        // When
        notificationListener.consumeNotificationEvent(eventDto);

        // Then
        verify(notificationRepository, times(1)).save(any(Notification.class));
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(pushNotificationService, never()).sendPushNotification(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle database save exception gracefully")
    void consumeNotificationEvent_DbSaveFails_ShouldNotCallOtherServices() {
        // Given
        doThrow(new RuntimeException("Database connection failed")).when(notificationRepository).save(any(Notification.class));

        // When
        notificationListener.consumeNotificationEvent(eventDto);

        // Then
        // Verify that external services are not called if the initial save fails
        verify(emailService, never()).sendEmail(anyString(), anyString(), anyString());
        verify(pushNotificationService, never()).sendPushNotification(anyString(), anyString(), anyString());
    }
    
    @Test
    @DisplayName("Should map invalid notification type to SYSTEM default")
    void consumeNotificationEvent_InvalidType_ShouldMapToSystem() {
        // Given
        eventDto.setType("INVALID_TYPE");

        // When
        notificationListener.consumeNotificationEvent(eventDto);

        // Then
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(NotificationType.SYSTEM, savedNotification.getType());
    }

    @Test
    @DisplayName("Should map null or empty notification type to SYSTEM default")
    void consumeNotificationEvent_NullOrEmptyType_ShouldMapToSystem() {
        // Given
        eventDto.setType(null);

        // When
        notificationListener.consumeNotificationEvent(eventDto);

        // Then
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification savedNotification = notificationCaptor.getValue();
        assertEquals(NotificationType.SYSTEM, savedNotification.getType());

        // Reset mock for the next verification
        reset(notificationRepository);

        // Given
        eventDto.setType("");

        // When
        notificationListener.consumeNotificationEvent(eventDto);

        // Then
        verify(notificationRepository).save(notificationCaptor.capture());
        Notification savedNotification2 = notificationCaptor.getValue();
        assertEquals(NotificationType.SYSTEM, savedNotification2.getType());
    }
}
