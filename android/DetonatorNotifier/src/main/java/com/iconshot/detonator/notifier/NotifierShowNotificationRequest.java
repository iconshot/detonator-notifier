package com.iconshot.detonator.notifier;

import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.core.app.NotificationCompat;

import com.iconshot.detonator.Detonator;
import com.iconshot.detonator.request.Request;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class NotifierShowNotificationRequest extends Request<NotifierShowNotificationRequest.ShowNotificationData> {
    public NotifierShowNotificationRequest(Detonator detonator, IncomingRequest incomingRequest) {
        super(detonator, incomingRequest);
    }

    @Override
    public void run() {
        ShowNotificationData data = decodeData(ShowNotificationData.class);

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
                    detonator.uiHandler.post(() -> {
                       error(new Exception("Error downloading image."));
                    });

                    return;
                }
            }

            notificationManager.notify(data.id, builder.build());

            detonator.uiHandler.post(() -> {
                end();
            });
        }).start();
    }

    static class ShowNotificationData {
        int id;
        String title;
        String body;
        String pictureUrl;
        String channelId;
        String priority;
    }
}
