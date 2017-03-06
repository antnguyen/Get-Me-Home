package cs117.getmehome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import static android.support.v4.content.ContextCompat.startActivity;

public class SmsReceiver extends BroadcastReceiver {
    public static String MESSAGE = "directions";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //---get the SMS message passed in---
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String[] str = {""};
        if (bundle != null) {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];
            for (int i = 0; i < msgs.length; i++) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    String format = bundle.getString("format");
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                } else {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }
                Log.d("source", msgs[i].getOriginatingAddress());
                str[i] += msgs[i].getMessageBody();
                str[i] += "\n";
            }
        }
        Intent getDir = new Intent(context, Direction.class);
        getDir.putExtra(MESSAGE, str);
        context.startActivity(getDir);
    }
}
