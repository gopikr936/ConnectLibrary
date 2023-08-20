package in.eko.connectlib.auth.biometric;

import android.content.Context;

public class BiometricAuthManager {

    private static volatile BiometricAuthManager sInstance;
    private Biometric mBiometric;

    private BiometricAuthManager(Context context) {
        mBiometric = new BiometricFactory().getBioMetricHandler(context);
    }

    public static BiometricAuthManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (BiometricAuthManager.class) {
                if (sInstance == null) {
                    sInstance = new BiometricAuthManager(context);
                }
            }
        }

        return sInstance;
    }

    // TODO : change return with enum
    public int  checkBiometricSupport() {
        if (mBiometric != null) {
            return mBiometric.checkBiometricSupport();
        }
        return BiometricFactory.NO_SUPPORT_HARD_FAIL;
    }

    public void showBiometricForAuth(Context context) {
        if (mBiometric != null) {
            mBiometric.showBiometricForAuth((BiometricAuthListener) context);
        }
    }

    public void showBiometricToEnable() {}

    public void onDestroy() {
        sInstance = null;
    }
}
