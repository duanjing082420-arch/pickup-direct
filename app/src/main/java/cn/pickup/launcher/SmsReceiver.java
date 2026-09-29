package cn.pickup.launcher;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

/**
 * 监听取件短信，自动提取取件码并保存到本地包裹库。
 * 需要 RECEIVE_SMS 权限（运行时申请）。
 */
public final class SmsReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null
                || !Telephony.Sms.Intents.SMS_RECEIVED_ACTION.equals(intent.getAction())) {
            return;
        }
        Bundle bundle = intent.getExtras();
        if (bundle == null) {
            return;
        }
        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null || pdus.length == 0) {
            return;
        }
        String format = bundle.getString("format");
        StringBuilder builder = new StringBuilder();
        for (Object pdu : pdus) {
            if (pdu instanceof byte[]) {
                SmsMessage message = SmsMessage.createFromPdu((byte[]) pdu, format);
                if (message != null) {
                    builder.append(message.getMessageBody());
                }
            }
        }
        CodeParser.Result result = CodeParser.parse(builder.toString());
        if (result != null && !result.code.isEmpty()) {
            PackageStore.addIfAbsent(context, result.carrier, result.code, result.station);
        }
    }
}
