package in.eko.connectlib.permission;

public interface PermissionCallback {

    public void onPermissionGranted();
    public void onPermissionDenied();
    public void onPermissionDeniedBySystem();
}
