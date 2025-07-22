package com.iconshot.detonator.notifier;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;

import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.firebase.messaging.FirebaseMessaging;

import com.iconshot.detonator.Detonator;
import com.iconshot.detonator.module.Module;

import java.util.HashMap;
import java.util.Map;

public class NotifierModule extends Module {
    private String token = "";

    public NotifierModule(Detonator detonator) {
        super(detonator);
    }

    private void setToken(String token) {
        if (this.token.contentEquals(token)) {
            return;
        }

        this.token = token;

        detonator.emit("com.iconshot.detonator.notifier.token", token);
    }

    @Override
    public void setUp() {
        detonator.setRequestClass("com.iconshot.detonator.notifier::showNotification", NotifierShowNotificationRequest.class);

        detonator.setRequestClass("com.iconshot.detonator.notifier::createChannel", NotifierCreateChannelRequest.class);
        detonator.setRequestClass("com.iconshot.detonator.notifier::createChannelGroup", NotifierCreateChannelGroupRequest.class);

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(task -> {
                    String token = task.isSuccessful() ? task.getResult() : "";

                    setToken(token);
                });

        BroadcastReceiver tokenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String token = intent.getStringExtra("token");

                setToken(token);
            }
        };

        BroadcastReceiver messageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle messageBundle = intent.getBundleExtra("message");
                Bundle dataBundle = intent.getBundleExtra("data");

                Message message = new Message();

                message.messageId = messageBundle.getString("messageId");
                message.senderId = messageBundle.getString("senderId");

                message.data = new HashMap<>();

                for (String key : dataBundle.keySet()) {
                    message.data.put(key, dataBundle.getString(key));
                }

                detonator.emit("com.iconshot.detonator.notifier.message", message);
            }
        };

        Lifecycle lifecycle = ((LifecycleOwner) detonator.context).getLifecycle();

        lifecycle.addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onResume(LifecycleOwner owner) {
                ContextCompat.registerReceiver(
                        detonator.context,
                        tokenReceiver,
                        new IntentFilter("com.iconshot.detonator.notifier.intent.token"),
                        ContextCompat.RECEIVER_NOT_EXPORTED
                );

                ContextCompat.registerReceiver(
                        detonator.context,
                        messageReceiver,
                        new IntentFilter("com.iconshot.detonator.notifier.intent.message"),
                        ContextCompat.RECEIVER_NOT_EXPORTED
                );
            }

            @Override
            public void onDestroy(LifecycleOwner owner) {
                detonator.context.unregisterReceiver(tokenReceiver);
                detonator.context.unregisterReceiver(messageReceiver);
            }
        });
    }

    static class Message {
        String messageId;
        String senderId;
        Map<String, String> data;
    }
}
