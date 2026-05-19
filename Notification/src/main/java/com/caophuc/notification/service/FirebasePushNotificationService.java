package com.caophuc.notification.service;

public interface FirebasePushNotificationService {

    void sendPushNotification(String fcmToken, String title, String body);

}
