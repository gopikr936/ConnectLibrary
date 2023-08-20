package in.eko.connectlib;

import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.greenrobot.eventbus.EventBus;

import java.util.Map;

import in.eko.connectlib.pojo.NotificationMessageEvent;
import in.eko.connectlib.pojo.NotificationRegisterEvent;


public class ConnectFirebaseMessagingService extends FirebaseMessagingService {
	private static final String TAG = "EkoFCMMsgService";

	public ConnectFirebaseMessagingService() {
	}

	@Override
	public void onNewToken(String token) {
		super.onNewToken(token);

		Log.w(TAG, "onNewToken: " + token);

		// Toast.makeText(getApplicationContext(), "onNewToken: " + token, Toast.LENGTH_SHORT).show();

		// Send Info to Connect
		EventBus.getDefault().post(new NotificationRegisterEvent(token));
	}

//	@Override
//	public void onTokenRefresh() {
//		// Get updated InstanceID token.
//		String refreshedToken = FirebaseInstanceId.getInstance().getToken();
//		Log.d(TAG, "Refreshed token: " + refreshedToken);
//
//		// If you want to send messages to this application instance or
//		// manage this apps subscriptions on the server side, send the
//		// Instance ID token to your app server.
//		// sendRegistrationToServer(refreshedToken);
//	}

	@Override
	public void onMessageReceived(RemoteMessage remoteMessage) {

		// Not getting messages here? See why this may be: https://goo.gl/39bRNJ
		Log.w(TAG, "From: " + remoteMessage.getFrom());


		// Check if message contains a data payload.
		if (remoteMessage.getData().size() > 0) {
			Log.w(TAG, "Message data payload: " + remoteMessage.getData());

			Map<String, String> notif_data_map = remoteMessage.getData();

			// Send Info to Connect
			EventBus.getDefault().post(new NotificationMessageEvent(notif_data_map.get("connect_data")));
		}

		// Check if message contains a notification payload.
		if (remoteMessage.getNotification() != null) {
			Log.w(TAG, "Message Notification Body: " + remoteMessage.getNotification().getBody());
		}

		// Also if you intend on generating your own notifications as a result of a received FCM
		// message, here is where that should be initiated. See sendNotification method below.
	}
}
