package in.eko.connectlib.utils;

import static android.app.Activity.RESULT_OK;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.location.Location;
import android.location.LocationManager;
import android.provider.Settings;
import android.util.Log;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.Granularity;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.Priority;
import com.google.android.gms.location.SettingsClient;

import org.json.JSONException;
import org.json.JSONObject;

import in.eko.connectlib.BaseConnectActivity;
import in.eko.connectlib.constants.Params;

public class LocationHelper {

    private static final String TAG = LocationHelper.class.getSimpleName();
    public static int REQUEST_CHECK_SETTINGS = 100;

    private Activity mActivity;
    private LocationManager mLocationManager;
    private LocationRequest mLocationRequest;
    LocationSettingsRequest mLocationSettingsRequest;

    public LocationHelper(Activity activity) {
        mActivity = activity;
        mLocationManager = (LocationManager) mActivity.getSystemService((Context.LOCATION_SERVICE));

        mLocationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setGranularity(Granularity.GRANULARITY_PERMISSION_LEVEL).
                setWaitForAccurateLocation(true).
                build();
        //mLocationRequest.setInterval(10000);
        //mLocationRequest.setFastestInterval(5000);
        //mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

        if (mLocationRequest != null) {
            LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
            builder.addLocationRequest(mLocationRequest);
            mLocationSettingsRequest = builder.build();
        }
    }

    public void enableGPS() {
        SettingsClient client = LocationServices.getSettingsClient(mActivity);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            if (mLocationSettingsRequest != null) {
                client.checkLocationSettings(mLocationSettingsRequest)
                        .addOnSuccessListener(mActivity, locationSettingsResponse -> {
                            // Already enable. No need to show the dialog;
                            Log.d(TAG, "GPS already enabled");
                            sendResponse(true, true);
                        })
                        .addOnFailureListener(mActivity, e -> {
                            if (e instanceof ApiException) {
                                ApiException ex = (ApiException) e;
                                if (ex.getStatusCode() == LocationSettingsStatusCodes.RESOLUTION_REQUIRED) {
                                    try {
                                        ((ResolvableApiException) ex).startResolutionForResult(mActivity, REQUEST_CHECK_SETTINGS);
                                    } catch (IntentSender.SendIntentException sendIntentException) {
                                        sendIntentException.printStackTrace();
                                        Log.d(TAG, "Unable to start default functionality of GPS");
                                    }
                                } else if (ex.getStatusCode() == LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE) {
                                    // Location settings are unavailable so not possible to show any dialog now
                                    Log.d(TAG, "GPS change not available");
                                    sendResponse(false, false);
                                }
                            }
                        });
            }
        } else {
            Log.d(TAG, "GPS already enabled");
            sendResponse(true, true);
        }
    }

    public void getLocation() {
        FusedLocationProviderClient client = LocationServices.getFusedLocationProviderClient(mActivity);
        client.getLastLocation().addOnCompleteListener(mActivity, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Location location = task.getResult();
                sendLocationToConnect(location);
                Log.d(TAG, "lastKnowLocation " + location.toString());
            } else {
                sendLocationErrorToConnect();
                Log.d(TAG, "lastKnowLocation error " + task.getException());
            }
        });

        client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).addOnCompleteListener(mActivity, task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Location location = task.getResult();
                sendLocationToConnect(location);
                Log.d(TAG, "getCurrentLocation " + location.toString());
            } else {
                sendLocationErrorToConnect();
                Log.d(TAG, "getCurrentLocation error " + task.getException());
            }
        });
    }

    private void sendLocationErrorToConnect() {
        JSONObject response = new JSONObject();
        try {
            response.put("status", 1);
            response.put("code", 2);
            response.put("message", "Location unavailable");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        ((BaseConnectActivity) mActivity).sendWebViewResponse(Params.GEOLOCATION_RESPONSE, response.toString());
    }

    private void sendLocationToConnect(Location location) {
        JSONObject response = new JSONObject();
        try {
            response.put("status", 0);
            response.put("latitude", location.getLatitude());
            response.put("longitude", location.getLongitude());
            response.put("accuracy", Math.round(location.getAccuracy()));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if(response != null) {
            ((BaseConnectActivity) mActivity).sendWebViewResponse(Params.GEOLOCATION_RESPONSE, response.toString());
        }
    }

    public void onLocationDialogResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            //Toast.makeText(mActivity, "GPS enabled", Toast.LENGTH_LONG).show();
            Log.d(TAG, "GPS enabled");
            sendResponse(true, true);
        } else {
            //Toast.makeText(mActivity, "GPS is not enabled", Toast.LENGTH_LONG).show();
            Log.d(TAG, "GPS is not enabled");
            sendResponse(false, true);
        }
    }

    public void checkGpsStatusAndNotifyConnect() {
        if (mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            sendResponse(true, true);
        } else {
            sendResponse(false, true);
        }
    }

    private void sendResponse(boolean isEnabled, boolean isAvailable) {
        JSONObject obj = new JSONObject();
        try {
            obj.put(Params.GPS_ENABLED, isEnabled);
            obj.put(Params.GPS_AVAILABLE, isAvailable);
            ((BaseConnectActivity) mActivity).sendWebViewResponse(Params.GPS_STATUS_RESPONSE, obj.toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void destroy() {
        mActivity = null;
    }

}
