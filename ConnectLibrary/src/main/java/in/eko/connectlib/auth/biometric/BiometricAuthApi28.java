package in.eko.connectlib.auth.biometric;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import in.eko.connectlib.R;

public class BiometricAuthApi28 implements Biometric {

    private final String TAG = "BiometricAuthApi28";

    private BiometricPrompt biometricPrompt;
    private BiometricPrompt.PromptInfo promptInfo;

    private Context context;
    private FragmentActivity activity;

    public BiometricAuthApi28(Context context, FragmentActivity activity){
        this.context = context;
        this.activity = activity;
    }

    /**
     * Find whether user device has biometric support or not.
     * Called when the app starts during biometric initialization
     *
     * Requires API 28
     *
     * @return Integer support value
     *      1    Has Support
     *      0    Has no support - Soft Failure
     *      -1   Has no support - Hard Failure
     */
    @Override
    public int checkBiometricSupport() {

        BiometricManager biometricManager;

        try {
            biometricManager = BiometricManager.from(this.context);

        } catch (Exception e) {

            e.printStackTrace();
            Log.e(TAG, e.getMessage());

            return BiometricFactory.NO_SUPPORT_SOFT_FAIL;
        }

        int canAuthenticate = biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                        BiometricManager.Authenticators.BIOMETRIC_WEAK |
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL);

        switch (canAuthenticate) {

            case BiometricManager.BIOMETRIC_SUCCESS:
                Log.i(TAG, "App can authenticate using biometrics.");
                return BiometricFactory.HAS_SUPPORT;

            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                Log.i(TAG, "No biometric features available on this device (sensor or keyboard).");
                return BiometricFactory.NO_SUPPORT_HARD_FAIL;

            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                Log.i(TAG, "Biometric features are currently unavailable. Try again later.");
                return BiometricFactory.NO_SUPPORT_SOFT_FAIL;

            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                Log.i(TAG, "No Biometric or device credential is enrolled.");

                // This happens behind the scene. No message is shown to user.
                //
                // biometricAuthListener.biometricAdviceCallback("Register your biometric " +
                //    "fingerprint or PIN for faster login");

                return BiometricFactory.NO_SUPPORT_SOFT_FAIL;

            case BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED:
                Log.i(TAG, "Phone requires a security update to use biometric.");

                // This happens behind the scene. No message is shown to user.
                //
                // biometricAuthListener.biometricAdviceCallback("Your phone requires a security " +
                //    "update to use biometric or PIN for faster login");
                return BiometricFactory.NO_SUPPORT_SOFT_FAIL;

            case BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED:
                Log.i(TAG, "Options are incompatible with the current");
                return BiometricFactory.NO_SUPPORT_HARD_FAIL;

            case BiometricManager.BIOMETRIC_STATUS_UNKNOWN:
                Log.i(TAG, "Unable to determine whether the user can authenticate.");
                return BiometricFactory.UNKNOWN;
        }

        return BiometricFactory.UNKNOWN;
    }

    @Override
    public void showBiometricToEnable(BiometricAuthListener biometricAuthListener) {

        biometricPrompt = new BiometricPrompt(activity, ContextCompat.getMainExecutor(context),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        Log.i(TAG, "Biometric Enable Error: " + errString);
                    }

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);

                        Log.i(TAG, "Biometric Enable Success");
                        biometricAuthListener.biometricEnableCallback();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();

                        Log.i(TAG, "Biometric Enable Failed");
                        biometricAuthListener.biometricErrorCallback("Biometric failed to enable");
                    }
                });


        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.biometric_enable_title))
                .setSubtitle(context.getString(R.string.biometric_enable_subtitle))
//                .setDescription(context.getString(R.string.biometric_description))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        /*
         * When running biometricPrompt.authenticate, I am getting an error
         * androidx.fragment.app.FragmentManager Must be called from main thread of fragment host
         *
         * To fix this refer:
         * https://medium.com/@yossisegev/understanding-activity-runonuithread-e102d388fe93
         */
        activity.runOnUiThread (() -> biometricPrompt.authenticate(promptInfo));
    }

    @Override
    public void showBiometricForAuth(BiometricAuthListener biometricAuthListener) {
        biometricPrompt = new BiometricPrompt(this.activity, ContextCompat.getMainExecutor(context),
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationError(int errorCode,
                                                      @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);

                        Log.i(TAG, "Biometric Auth Error: " + errString);
                    }

                    @Override
                    public void onAuthenticationSucceeded(
                            @NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);

                        Log.i(TAG, "Biometric Auth Success");
                        biometricAuthListener.biometricSuccessCallback();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();

                        Log.i(TAG, "Biometric Auth Failed");
                        biometricAuthListener.biometricErrorCallback("Biometric Auth Failed");
                    }
                });


        promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(context.getString(R.string.biometric_auth_title))
                .setSubtitle(context.getString(R.string.biometric_auth_subtitle))
//                .setDescription(context.getString(R.string.biometric_description))
                .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG
                        | BiometricManager.Authenticators.BIOMETRIC_WEAK
                        | BiometricManager.Authenticators.DEVICE_CREDENTIAL)
                .build();

        /*
         * When running biometricPrompt.authenticate, I am getting an error
         * androidx.fragment.app.FragmentManager Must be called from main thread of fragment host
         *
         * To fix this refer:
         * https://medium.com/@yossisegev/understanding-activity-runonuithread-e102d388fe93
         */
        activity.runOnUiThread (() -> biometricPrompt.authenticate(promptInfo));
    }
}
