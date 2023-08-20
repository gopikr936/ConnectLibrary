package in.eko.connectlib.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKeys;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;

import in.eko.connectlib.constants.Params;


public class SharedPreferencesUtil {

    private static final String TAG = "RefreshTokenUtil";
    private static final int ENCRYPTED_SHARED_PREF_API_LEVEL = 23;

    private static SharedPreferences sharedPreferences;

    public static SharedPreferences getSharedPreferences(Activity activity) {
        if (sharedPreferences != null) return sharedPreferences;

        if (Build.VERSION.SDK_INT > ENCRYPTED_SHARED_PREF_API_LEVEL) {
            try {
                sharedPreferences = EncryptedSharedPreferences.create(
                        "preferences",
                        MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
                        activity.getApplicationContext(),
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                );
            } catch (GeneralSecurityException | IOException e) {
                e.printStackTrace();
                Log.e(TAG, "" + e.getMessage());
                return null;
            }
        } else {
            sharedPreferences = activity.getPreferences(Context.MODE_PRIVATE);
        }

        return sharedPreferences;
    }


    // clear refresh token from shared preferences
    public static void clear(Activity activity) {
        if (sharedPreferences == null) {
            getSharedPreferences(activity);
        }
        if (sharedPreferences != null) {
            Log.i(TAG, "Clearing Auth Tokens");

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.remove(Params.REFRESH_TOKEN);
            editor.remove(Params.LONG_SESSION);
            editor.remove(Params.BIOMETRIC_ENABLED);
            editor.apply();
        } else {
            Log.w(TAG, "Error in clearing Auth Tokens");
        }
    }


    // save refresh token received from back end
    public static void saveAuthTokens(Activity activity, String refreshToken, boolean longSession) {
        if (sharedPreferences == null) {
            getSharedPreferences(activity);
        }

        if (sharedPreferences == null) {
            Log.w(TAG, "SharedPreferences is null. Cannot save refresh token and long session.");
            return;
        }

        Log.i(TAG, "Saving " + Params.REFRESH_TOKEN + " " + refreshToken);
        Log.i(TAG, "Saving " + Params.LONG_SESSION + " " + longSession);

        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(Params.REFRESH_TOKEN, refreshToken);
        editor.putBoolean(Params.LONG_SESSION, longSession);
        editor.apply();
    }


    // save biometric support invoked during biometric init
    public static void saveBiometricSupport(Activity activity, int hasBiometricSupport) {
        if (sharedPreferences == null) {
            getSharedPreferences(activity);
        }

        if (sharedPreferences != null) {
            Log.i(TAG, "Saving " + Params.BIOMETRIC_SUPPORT + " " + hasBiometricSupport);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putInt(Params.BIOMETRIC_SUPPORT, hasBiometricSupport);
            editor.apply();
        } else {
            Log.w(TAG, "Error in saving biometric support");
        }
    }

    // save biometric enabled invoked during successful new login
    public static void saveBiometricEnabled(Activity activity, boolean biometricEnabled) {
        if (sharedPreferences == null) {
            getSharedPreferences(activity);
        }

        if (sharedPreferences != null) {
            Log.i(TAG, "Saving " + Params.BIOMETRIC_ENABLED + " " + biometricEnabled);

            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean(Params.BIOMETRIC_ENABLED, biometricEnabled);
            editor.apply();
        } else {
            Log.w(TAG, "Error in saving biometric enabled");
        }
    }

    // save app closed time
    public static void saveAppClosedTime(Activity activity) {
        if (sharedPreferences == null) {
            getSharedPreferences(activity);
        }

        if (sharedPreferences != null) {

            long timestamp = new Date().getTime();

            Log.i(TAG, "App Closed at time: " + timestamp);

            SharedPreferences.Editor editor = sharedPreferences.edit();

            editor.putLong(Params.APP_CLOSED_TIME, timestamp);
            editor.apply();


        } else {
            Log.w(TAG, "Error in saving biometric enabled");
        }
    }



}
