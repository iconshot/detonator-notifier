package com.iconshot.detonator.notifier;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.iconshot.detonator.Detonator;
import com.iconshot.detonator.request.Request;

public class NotifierCreateChannelRequest extends Request<NotifierCreateChannelRequest.CreateChannelData> {
    public NotifierCreateChannelRequest(Detonator detonator, IncomingRequest incomingRequest) {
        super(detonator, incomingRequest);
    }

    @Override
    public void run() {
        CreateChannelData data = decodeData(CreateChannelData.class);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            end(false);

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

        end(true);
    }

    static class CreateChannelData {
        String id;
        String name;
        String description;
        String importance;
        String groupId;
    }
}
