package com.iconshot.detonator.notifier;

import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import com.iconshot.detonator.Detonator;
import com.iconshot.detonator.request.Request;

public class NotifierCreateChannelGroupRequest extends Request<NotifierCreateChannelGroupRequest.CreateChannelGroupData> {
    public NotifierCreateChannelGroupRequest(Detonator detonator, IncomingRequest incomingRequest) {
        super(detonator, incomingRequest);
    }

    @Override
    public void run() {
        CreateChannelGroupData data = decodeData(CreateChannelGroupData.class);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            end(false);

            return;
        }

        NotificationManager notificationManager =
                (NotificationManager) detonator.context.getSystemService(Context.NOTIFICATION_SERVICE);

        NotificationChannelGroup group = new NotificationChannelGroup(data.id, data.name);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            group.setDescription(data.description);
        }

        notificationManager.createNotificationChannelGroup(group);

        end(true);
    }

    static class CreateChannelGroupData {
        String id;
        String name;
        String description;
    }
}
