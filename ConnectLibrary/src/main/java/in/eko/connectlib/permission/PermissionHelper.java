package in.eko.connectlib.permission;

import static androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import in.eko.connectlib.R;

public class PermissionHelper {

    private static final String TAG = PermissionHelper.class.getSimpleName();
    public static final int REQUEST_CODE = 999;

    private Activity activity;
    private String[] permissions;
    private PermissionCallback mPermissionCallback;
    private boolean showRational;

    public PermissionHelper(Activity activity) {
        this.activity = activity;
    }

    public void request(PermissionCallback permissionCallback, String[] permissions) {
        this.permissions = permissions;
        this.mPermissionCallback = permissionCallback;
        if (checkSelfPermission(permissions) == false) {
            showRational = shouldShowRational(permissions);
            ActivityCompat.requestPermissions(activity, filterNotGrantedPermission(permissions), REQUEST_CODE);
        } else {
            Log.d(TAG, "PERMISSION: Permission Granted");
            if (mPermissionCallback != null)
                mPermissionCallback.onPermissionGranted();
        }
    }

    public boolean checkSelfPermission(String[] permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean shouldShowRational(String[] permissions) {
        boolean currentShowRational = false;
        for (String permission : permissions) {
             if (shouldShowRequestPermissionRationale(activity, permission) == true) {
                currentShowRational = true;
                break;
             }
        }
        return currentShowRational;
    }

    private String[] filterNotGrantedPermission(String[] permissions) {
        List<String> notGrantedPermission = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                notGrantedPermission.add(permission);
            }
        }
        return notGrantedPermission.toArray(new String[notGrantedPermission.size()]);
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE) {
            boolean denied = false;
            int i = 0;
            ArrayList<String> grantedPermissions = new ArrayList<>();
            for (int grantResult : grantResults) {
                if (grantResult != PackageManager.PERMISSION_GRANTED) {
                    denied = true;
                } else {
                    grantedPermissions.add(permissions[i]);
                }
                i++;
            }

            if (denied) {
                boolean currentShowRational = shouldShowRational(permissions);
                if (showRational == false && currentShowRational == false) {
                    Log.d(TAG, "PERMISSION: Permission Denied By System");
                    if (mPermissionCallback != null)
                        mPermissionCallback.onPermissionDeniedBySystem();
                } else {
                    Log.d(TAG, "PERMISSION: Permission Denied");
                    if (mPermissionCallback != null) {
                        mPermissionCallback.onPermissionDenied();
                    }
                }
            } else {
                Log.d(TAG, "PERMISSION: Permission Granted");
                if (mPermissionCallback != null)
                    mPermissionCallback.onPermissionGranted();
            }
        }
    }

}
