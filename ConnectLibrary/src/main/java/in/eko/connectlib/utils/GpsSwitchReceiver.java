package in.eko.connectlib.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;

import org.json.JSONException;
import org.json.JSONObject;

import in.eko.connectlib.BaseConnectActivity;
import in.eko.connectlib.constants.Params;

public class GpsSwitchReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().matches(LocationManager.PROVIDERS_CHANGED_ACTION)) {
            LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

            // Toast.makeText(context, "isGpsEnabled : "+isGpsEnabled+"   isNetwork: "+isNetworkEnabled , Toast.LENGTH_LONG).show();
            if (isGpsEnabled || isNetworkEnabled) {
                sendResponse(context, true);
            } else {
                sendResponse(context, false);
            }
        }
    }

    private void sendResponse(Context context, boolean isEnabled) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(Params.GPS_ENABLED, isEnabled);
            obj.put(Params.GPS_AVAILABLE, true);
            ((BaseConnectActivity) context).sendWebViewResponse(Params.GPS_STATUS_RESPONSE, obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
