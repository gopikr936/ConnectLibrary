package in.eko.connectlib;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.common.api.Status;

public class MySmsBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = MySmsBroadcastReceiver.class.getSimpleName();
    private SMSListener smsListener;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "onReceive : "+intent.getAction());

        if (SmsRetriever.SMS_RETRIEVED_ACTION.equals(intent.getAction())) {
            Bundle extras = intent.getExtras();
            Status status = null;
            if (extras != null) {
                status = (Status) extras.get(SmsRetriever.EXTRA_STATUS);
            }
            if (status != null) {
                switch (status.getStatusCode()) {
                    case CommonStatusCodes.SUCCESS:
                        // Get SMS message contents
                        String message = (String) extras.get(SmsRetriever.EXTRA_SMS_MESSAGE);
                        // Extract one-time code from the message and complete verification
                        // by sending the code back to your server.
                        if (smsListener != null)
                            smsListener.onSuccess(message);
                        break;
                    case CommonStatusCodes.TIMEOUT:
                        // Waiting for SMS timed out (5 minutes)
                        // Handle the error ...
                        if (smsListener != null)
                            smsListener.onError("Failed to extract from Broadcast Receiver");
                        break;
                }
            }
        }
    }

    public void initSMSListener(SMSListener listener) {
        smsListener = listener;
    }
}
