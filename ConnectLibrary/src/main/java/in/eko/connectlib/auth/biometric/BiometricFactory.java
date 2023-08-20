package in.eko.connectlib.auth.biometric;

import android.content.Context;
import android.os.Build;

import androidx.fragment.app.FragmentActivity;

public class BiometricFactory {

    public static final int UNKNOWN = 0;
    public static final int HAS_SUPPORT = 1;
    public static final int NO_SUPPORT_SOFT_FAIL = -1;
    public static final int NO_SUPPORT_HARD_FAIL = -2;

    private final int API_VERSION = Build.VERSION.SDK_INT;

    public Biometric getBioMetricHandler(Context context) {
        if (API_VERSION >= Build.VERSION_CODES.M) {
            return new BiometricAuthApi28(context, (FragmentActivity) context);
        } /*else if (API_VERSION >= Build.VERSION_CODES.M){
            return new BiometricAuthApi23(this.context);
        }*/

        return null;
    }
}


