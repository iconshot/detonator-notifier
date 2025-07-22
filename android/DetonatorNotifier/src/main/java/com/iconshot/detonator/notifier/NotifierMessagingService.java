package com.iconshot.detonator.notifier;

import android.content.Intent;
import android.os.Bundle;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.Map;

public class NotifierMessagingService extends FirebaseMessagingService {
    @Override
    public void onNewToken(String token) {
        Intent intent = new Intent("com.iconshot.detonator.notifier.intent.token");

        intent.putExtra("token", token);

        sendBroadcast(intent);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Intent intent = new Intent("com.iconshot.detonator.notifier.intent.message");

        Bundle messageBundle = new Bundle();
        Bundle dataBundle = new Bundle();

        messageBundle.putString("messageId", remoteMessage.getMessageId());
        messageBundle.putString("senderId", remoteMessage.getFrom());

        for (Map.Entry<String, String> entry : remoteMessage.getData().entrySet()) {
            dataBundle.putString(entry.getKey(), entry.getValue());
        }

        intent.putExtra("message", messageBundle);
        intent.putExtra("data", dataBundle);

        sendBroadcast(intent);
    }
}