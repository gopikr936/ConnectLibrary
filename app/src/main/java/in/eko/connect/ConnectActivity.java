package in.eko.connect;

import android.graphics.drawable.Drawable;
import android.os.Bundle;

import in.eko.connectlib.BaseConnectActivity;
import in.eko.connectlib.ConfigProvider;

public class ConnectActivity extends BaseConnectActivity implements ConfigProvider {

	@Override
	public String getUrl(){
		return BuildConfig.web_url;
	}

	@Override
	public String getUatUrl(){
		return BuildConfig.web_uat_url;
	}

	@Override
	public String getGoogleLoginClientId(){
		return BuildConfig.google_server_client_id;
	}

	@Override
	public String getAppName() {
		return getString(R.string.app_name);
	}
	@Override
	public Drawable getAppIcon() {
		return getDrawable(R.mipmap.ic_launcher);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		mVersionCode = BuildConfig.VERSION_CODE;

		super.onCreate(savedInstanceState);
	}

}

