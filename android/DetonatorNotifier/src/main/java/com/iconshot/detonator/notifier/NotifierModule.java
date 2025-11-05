package com.iconshot.detonator.notifier;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;

import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;

import com.google.firebase.messaging.FirebaseMessaging;

import com.iconshot.detonator.Detonator;
import com.iconshot.detonator.module.Module;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
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

        detonator.send("com.iconshot.detonator.notifier.token", token);
    }

    @Override
    public void setUp() {
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

                detonator.send("com.iconshot.detonator.notifier.message", message);
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

        detonator.setRequestListener("com.iconshot.detonator.notifier::showNotification", (promise, value, edge) -> {
            ShowNotificationData data = detonator.decode(value, ShowNotificationData.class);

            new Thread(() -> {
                NotificationManager notificationManager =
                        (NotificationManager) detonator.context.getSystemService(Context.NOTIFICATION_SERVICE);

                NotificationCompat.Builder builder = new NotificationCompat.Builder(detonator.context, data.channelId);

                builder.setSmallIcon(detonator.context.getApplicationInfo().icon);

                builder.setContentTitle(data.title);
                builder.setContentText(data.body);

                int priority = NotificationCompat.PRIORITY_DEFAULT;

                if (data.priority != null) {
                    switch (data.priority) {
                        case "high": {
                            priority = NotificationCompat.PRIORITY_HIGH;

                            break;
                        }

                        case "low": {
                            priority = NotificationCompat.PRIORITY_LOW;

                            break;
                        }

                        case "min": {
                            priority = NotificationCompat.PRIORITY_MIN;

                            break;
                        }

                        case "max": {
                            priority = NotificationCompat.PRIORITY_MAX;

                            break;
                        }

                        case "default":
                        default: {
                            priority = NotificationCompat.PRIORITY_DEFAULT;

                            break;
                        }
                    }
                }

                builder.setPriority(priority);

                if (data.pictureUrl != null) {
                    try {
                        URL url = new URL(data.pictureUrl);

                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                        connection.setDoInput(true);
                        connection.setInstanceFollowRedirects(true);

                        connection.connect();

                        InputStream input = connection.getInputStream();

                        Bitmap bitmap = BitmapFactory.decodeStream(input);

                        builder.setLargeIcon(bitmap);

                        builder.setStyle(new NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap).bigLargeIcon((Bitmap) null));
                    } catch (Exception exception) {
                        promise.reject("Error downloading image.");

                        return;
                    }
                }

                notificationManager.notify(data.id, builder.build());

                promise.resolve();
            }).start();
        });

        detonator.setRequestListener("com.iconshot.detonator.notifier::createChannel", (promise, value, edge) -> {
            CreateChannelData data = detonator.decode(value, CreateChannelData.class);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                promise.resolve(false);

                return;
            }

            NotificationManager notificationManager =
                    (NotificationManager) detonator.context.getSystemService(Context.NOTIFICATION_SERVICE);

            int importance = NotificationManager.IMPORTANCE_DEFAULT;

            if (data.importance != null) {
                switch (data.importance) {
                    case "high": {
                        importance = NotificationManager.IMPORTANCE_HIGH;

                        break;
                    }

                    case "low": {
                        importance = NotificationManager.IMPORTANCE_LOW;

                        break;
                    }

                    case "min": {
                        importance = NotificationManager.IMPORTANCE_MIN;

                        break;
                    }

                    case "max": {
                        importance = NotificationManager.IMPORTANCE_MAX;

                        break;
                    }

                    case "none": {
                        importance = NotificationManager.IMPORTANCE_NONE;

                        break;
                    }

                    case "unspecified": {
                        importance = NotificationManager.IMPORTANCE_UNSPECIFIED;

                        break;
                    }

                    case "default":
                    default: {
                        importance = NotificationManager.IMPORTANCE_DEFAULT;

                        break;
                    }
                }
            }

            NotificationChannel channel = new NotificationChannel(data.id, data.name, importance);

            channel.setDescription(data.description);
            channel.setGroup(data.groupId);

            notificationManager.createNotificationChannel(channel);

            promise.resolve(true);
        });

        detonator.setRequestListener("com.iconshot.detonator.notifier::createChannelGroup", (promise, value, edge) -> {
            CreateChannelGroupData data = detonator.decode(value, CreateChannelGroupData.class);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                promise.resolve(false);

                return;
            }

            NotificationManager notificationManager =
                    (NotificationManager) detonator.context.getSystemService(Context.NOTIFICATION_SERVICE);

            NotificationChannelGroup group = new NotificationChannelGroup(data.id, data.name);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                group.setDescription(data.description);
            }

            notificationManager.createNotificationChannelGroup(group);

            promise.resolve(true);
        });
    }

    public static class Message {
        String messageId;
        String senderId;
        Map<String, String> data;
    }

    public static class ShowNotificationData {
        int id;
        String title;
        String body;
        String pictureUrl;
        String channelId;
        String priority;
    }

    public static class CreateChannelData {
        String id;
        String name;
        String description;
        String importance;
        String groupId;
    }

    public static class CreateChannelGroupData {
        String id;
        String name;
        String description;
    }
}
