package in.eko.connectlib;

import android.graphics.drawable.Drawable;

public interface ConfigProvider {
    String getUrl();
    String getUatUrl();
    //String getAttestationKey();
    String getGoogleLoginClientId();
    String getAppName();
    Drawable getAppIcon();
}
