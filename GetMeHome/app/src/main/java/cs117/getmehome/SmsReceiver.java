package cs117.getmehome;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

import static android.support.v4.content.ContextCompat.startActivity;

public class SmsReceiver extends BroadcastReceiver {
    public static String MESSAGE = "directions";
    String phone = MainActivity.PHONENO;
    Map<String, String> instructions = new HashMap<String, String>();
    String s;
    String output = "";

    @Override
    public void onReceive(Context context, Intent intent) {
        // TODO: This method is called when the BroadcastReceiver is receiving
        // an Intent broadcast.
        //---get the SMS message passed in---
        Bundle bundle = intent.getExtras();
        SmsMessage[] msgs = null;
        String[] str = null;
        boolean received = false;
        int numSms = 0;

        if (bundle != null) {
            //---retrieve the SMS message received---
            Object[] pdus = (Object[]) bundle.get("pdus");
            msgs = new SmsMessage[pdus.length];
            str = new String[msgs.length];
            for (int i = 0; i < msgs.length; i++) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    String format = bundle.getString("format");
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i], format);
                } else {
                    msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
                }
                Log.d("source", msgs[i].getOriginatingAddress());
                Log.d("the text", msgs[i].getMessageBody());
                if (msgs[i].getOriginatingAddress().equals(phone) ||
                        msgs[i].getOriginatingAddress().equals(phone.substring(2))) {
                    received = true;
                    str[i] = msgs[i].getMessageBody();
                    s = str[i].substring(str[i].indexOf('<') + 1, str[i].indexOf('/'));
                    numSms = Integer.parseInt(str[i].substring(str[i].indexOf('/') + 1, str[i].indexOf('>')));
                    Log.d("#msgs", s);
                    instructions.put(s, str[i].substring(str[i].indexOf('>') + 1));
                }
            }
        }

        Log.d("number of sms", String.valueOf(instructions.size()));

        if (received && instructions.size() == numSms) {
            for (int i = 0; i < instructions.size(); i++) {
                output += instructions.get(String.valueOf(i + 1));
            }

            Intent getDir = new Intent(context, Direction.class);
            getDir.putExtra(MESSAGE, output);
            context.startActivity(getDir);
        }
        //throw new UnsupportedOperationException("Not yet implemented");
    }
}
