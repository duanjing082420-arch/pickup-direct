package cn.pickup.launcher;

import android.app.Notification;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

/**
 * 监听系统通知里的取件码（菜鸟、淘宝、拼多多的到件推送常带取件码）。
 * 需要用户在「设置 → 通知 → 通知使用权」里手动开启本服务。
 */
public final class PickupNotificationListener extends NotificationListenerService {

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (sbn == null) {
            return;
        }
        Notification notification = sbn.getNotification();
        if (notification == null) {
            return;
        }
        Bundle extras = notification.extras;
        if (extras == null) {
            return;
        }
        CharSequence title = extras.getCharSequence(Notification.EXTRA_TITLE);
        CharSequence text = extras.getCharSequence(Notification.EXTRA_TEXT);
        CharSequence bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT);

        StringBuilder builder = new StringBuilder();
        if (title != null) {
            builder.append(title).append(' ');
        }
        if (text != null) {
            builder.append(text).append(' ');
        }
        if (bigText != null) {
            builder.append(bigText);
        }

        CodeParser.Result result = CodeParser.parse(builder.toString());
        if (result != null && !result.code.isEmpty()) {
            PackageStore.addIfAbsent(this, result.carrier, result.code, result.station);
        }
    }

    /** 判断用户是否已经开启了通知使用权。 */
    static boolean isEnabled(Context context) {
        String flat = Settings.Secure.getString(
                context.getContentResolver(),
                "enabled_notification_listeners"
        );
        if (TextUtils.isEmpty(flat)) {
            return false;
        }
        ComponentName component = new ComponentName(context, PickupNotificationListener.class);
        return flat.contains(component.flattenToString())
                || flat.contains(component.flattenToShortString());
    }
}
