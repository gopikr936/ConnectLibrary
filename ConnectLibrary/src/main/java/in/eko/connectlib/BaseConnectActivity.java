package in.eko.connectlib;

import static in.eko.connectlib.constants.Params.UPI_PAYMENT_RESPONSE;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentTransaction;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.credentials.Credential;
import com.google.android.gms.auth.api.credentials.HintRequest;
import com.google.android.gms.auth.api.phone.SmsRetriever;
import com.google.android.gms.auth.api.phone.SmsRetrieverClient;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings;
import com.gspl.leegalitysdk.Leegality;
import com.razorpay.Checkout;
import com.razorpay.PaymentResultListener;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import in.eko.connectlib.auth.GoogleLogin;
import in.eko.connectlib.auth.biometric.BiometricAuthListener;
import in.eko.connectlib.auth.biometric.BiometricAuthManager;
import in.eko.connectlib.auth.biometric.BiometricFactory;
import in.eko.connectlib.constants.Params;
import in.eko.connectlib.permission.PermissionCallback;
import in.eko.connectlib.permission.PermissionHelper;
import in.eko.connectlib.pojo.NotificationMessageEvent;
import in.eko.connectlib.pojo.NotificationRegisterEvent;
import in.eko.connectlib.pojo.WebViewMessage;
import in.eko.connectlib.utils.GpsSwitchReceiver;
import in.eko.connectlib.utils.LocationHelper;
import in.eko.connectlib.utils.RootUtil;
import in.eko.connectlib.utils.SharedPreferencesUtil;
import in.eko.uidai_rdservice_manager_lib.RDServiceEvents;
import in.eko.uidai_rdservice_manager_lib.RDServiceManager;

public class BaseConnectActivity extends AppCompatActivity implements PaymentResultListener,
        RDServiceEvents, SafetyNetFragment.onSafetyResponseListener,
        SafetyNetFragment.onGoogleEventListener,
        BiometricAuthListener {

    private static final String TAG = "BaseConnectActivity";

    private static final String EXCEPTION_PREFIX = "ERROR: ";
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;	// Request code for updating Google Play Services
    private static final int RC_GOOGLE_SIGN_IN = 9001;                  // Google Login
    private static final int MY_PERMISSION_REQUEST_LOCATION = 9003;     // Location Permission Request
    private static final int MY_PERMISSION_REQUEST_CAMERA = 9004;       // Location Permission Request
    private static final int REQUEST_SELECT_FILE = 9005;                // Show File Upload Dialog
    private static final int REQUEST_WRITE_EXT_STORAGE = 9007;          // Write-External-Storage Permission Request
    private static final int RESOLVE_PHONE_NUMBER_HINT = 9008;          // User's Phone Number Selector Hint Request
    private static final int RC_LEEGALITY_ESIGN = 9009;                 // Leegality ESign
    private static final int RC_APP_UPDATE = 9090;
    private static final int UPI_PAYMENT = 8000;
    private static final long PRINT_TIME_THRESHOLD_VALUE = 500;

    private LinearLayout splash = null;
    private ImageView spinner = null;
    private Button btnReload = null;
    private TextView appNameTextView = null;
    private ProgressBar progressBar = null;
    private RDServiceManager rdServiceManager = null;
    private AppUpdateManager mAppUpdateManager;

    private FirebaseRemoteConfig mFirebaseRemoteConfig;

    private SafetyNetFragment safetynetFragment;
    private GoogleLogin mGoogleLogin;
    private MySmsBroadcastReceiver mSMSReceiver;
    private LocationHelper mLocationHelper;
    private GpsSwitchReceiver mGpsSwitchReceiver;

    private GeolocationPermissions.Callback mGeolocationCallback;
    private ValueCallback<Uri[]> mFilePathCallback;     		// For File Upload in WebView
    private PermissionRequest mPermissionRequest;
    private PermissionHelper mPermissionHelper;
    private Queue<WebViewMessage> connectMsgQueue;

    private WebView webView;

    private String base_url;
    private int hasBiometricSupport;

    private String nonce;
    private boolean isSafetynetEnabled;
    private boolean isRefreshTokenEnabled;
    private String refreshToken;
    private boolean isLongSession = false;

    private String mGeolocationRequestOrigin;
    private String[] mRequestedResources;

    private String mFileSaveBlobCache;
    private String _app_system_settings_opened_for = "";

    private Boolean connectReady = false;
    private boolean _app_system_settings_opened = false;

    protected int mVersionCode;
    private long mLastPrintTimeStamp = 0;
    private String url;
    private String uatUrl;
    private String attestationKey;
    private String googleLoginClientId;
    private String appName;
    private Drawable appIcon;

    public ConfigProvider configProvider;

    public void setConfigProvider(ConfigProvider _configProvider) {
        configProvider = _configProvider;
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connect);

        setConfigProvider((ConfigProvider) this);
        initSecrets();

        splash = findViewById(R.id.splash);
        spinner = findViewById(R.id.img_logo);
        btnReload = findViewById(R.id.btnReload);
        appNameTextView = findViewById(R.id.tv_appName);
        progressBar = findViewById(R.id.progress_bar);

        spinner.setImageDrawable(appIcon);
        appNameTextView.setText(appName);
        splash.setVisibility(View.VISIBLE);

        connectMsgQueue = new LinkedList<>();

        // Register EventBus...
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }

        // SECURITY: Don't allow rooted devices...
        // TODO: (?) Replace custom logic with https://developer.android.com/training/safetynet
        if (RootUtil.isDeviceRooted()) {
            toast(getString(R.string.reject_rooted));
            this.finishAndRemoveTask();
        }

        // Get enablement flags - whether isSafetynetEnabled, isRefreshTokenEnabled
        initFirebaseRemoteConfig();

        // Load refresh token, long session, hasBiometric
        initSessionFromSharedPreferences();

        // Check if device has biometric support and store that in shared preferences.
        initBiometricAuth();

        // TODO: Check for existing refresh token, send to connect -
        //  do need to wait for connect_ready
        dispatchCachedRefreshToken();

        loadSafetynetFragment();
        loadActivity();
    }

    /**
     * Handle deep link
     * @param url Deep link url
     */
    private void handleDeepLink(Uri url) {
        if (url == null) {
            return;
        }

        Log.d(TAG, "handleDeepLink: toString: " + url.toString() );
        Log.d(TAG, "handleDeepLink: getScheme: " + url.getScheme() );
        Log.d(TAG, "handleDeepLink: getHost: " + url.getHost() );
        Log.d(TAG, "handleDeepLink: getPath: " + url.getPath() );
        Log.d(TAG, "handleDeepLink: getFragment: " + url.getFragment() );
        Log.d(TAG, "handleDeepLink: getQuery: " + url.getQuery() );

        if (url.getScheme().equals("ekoconnect")) {
            // Handle "ekoconnect://..." links:
            if (url.getHost().equals("") && !url.getPath().equals("")) {
                String connect_link = url.getPath().replaceFirst("^!?\\/+", "");
                sendWebViewResponse("open_connect_url", "ekoconnect://" + connect_link);
            } else {
                sendWebViewResponse("open_connect_url", url.toString().replaceAll("\\?.*?$", ""));
            }
        } else {
            // Normal "https://connect.eko.in/..." links:
            String frag = url.getFragment();
            if (frag != null && !frag.equals("")) {
                String connect_link = frag.replaceFirst("^!?\\/+", "");
                Log.d(TAG, "handleDeepLink: Sending link to Connect WebView: " + connect_link );
                sendWebViewResponse("open_connect_url", "ekoconnect://" + connect_link);
            }
        }
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.isFocused() && shouldConsumeBackKey()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    private boolean shouldConsumeBackKey() {
        return !webView.getUrl().equals(base_url + "/")
                && !webView.getUrl().equals(base_url + getString(R.string.startup_path));
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Save timestamp when the app is closed
        SharedPreferencesUtil.saveAppClosedTime(this);
    }

    /**
     * Load Safetynet Fragment
     */
    public void loadSafetynetFragment () {
        Bundle bundle = new Bundle();
        bundle.putString(Params.NONCE, nonce);
        bundle.putString(Params.ATTESTATION_KEY, attestationKey);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        safetynetFragment = new SafetyNetFragment();
        safetynetFragment.setArguments(bundle);
        transaction.replace(R.id.safetynet_fragment, safetynetFragment);
        transaction.commit();
        getSupportFragmentManager().executePendingTransactions();
    }

    /**
     * Load connect activity, setup:
     * Google/Facebook clients, RD service, WebView, WebSettings
     * Base URL
     * Send Viewview Response
     * Get FCM (Firebase Cloud Messaging) Token
     * Check For Updates
     */
    private void loadActivity () {
        splash.setVisibility(View.INVISIBLE);

        Log.d(TAG, "Refresh Token Flag: " + isRefreshTokenEnabled);
        Log.d(TAG, "Safetynet Flag: " + isSafetynetEnabled);

        mGoogleLogin = new GoogleLogin(getApplicationContext(), BaseConnectActivity.this , googleLoginClientId);

        // Setup RDService ..................
        rdServiceManager = new RDServiceManager.Builder(this).create();

        // ----------------- WEB VIEW ------------------------------------------
        webView = findViewById(R.id.activity_connect_webview);
        setupWebView();

        WebSettings webSettings = webView.getSettings();
        setupWebSettings(webSettings);

        setupBaseUrl();
        webView.loadUrl(base_url + getString(R.string.startup_path));
        try {
            sendWebViewResponse("set_native_version", "" + getPackageManager().getPackageInfo(getPackageName(),0).versionName);
        } catch (PackageManager.NameNotFoundException e){
            sendWebViewResponse("set_native_version" , "Version Not Found");
        }

        // Send the native app version to Connect...
         //sendWebViewResponse("set_native_version", "" + mVersionCode);

        // Get FCM token...
        getFCMToken();

        mLocationHelper = new LocationHelper(this);
        mLocationHelper.checkGpsStatusAndNotifyConnect();

        // Start Gps Switch receiver
        mGpsSwitchReceiver = new GpsSwitchReceiver();
        IntentFilter filter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
        filter.addAction(Intent.ACTION_PROVIDER_CHANGED);
        registerReceiver(mGpsSwitchReceiver, filter);

        btnReload.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                reloadWebView();
            }
        });

        // Check For Updates...
        checkForUpdates();
    }

    private void setupBaseUrl() {
        base_url = url; // https://connect.eko.in
        // DEBUG MODE?
        if (0 != (getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE)) {
            if (uatUrl.isEmpty()) { // Connect Beta - https://beta.ekoconnect.in
                base_url = url;
            } else {
                base_url = uatUrl;
            }
            WebView.setWebContentsDebuggingEnabled(true);               // Enable Remote Debugging (chrome://inspect#devices)

            //findViewById(R.id.txtDebug).setVisibility(View.VISIBLE);    // Show Debug Banner
        }
        // base_url = "http://192.168.1.10:3000";       // Home WiFi Test (TODO: Comment Out)
        // base_url = "http://192.168.1.24:3000";     // Office WiFi Test (TODO: Comment Out)
        // NOTE: In the manifest <application> add attribute: android:usesCleartextTraffic="true"
    }

    private void getFCMToken(){
        // Firebase Cloud Messaging
        // https://firebase.google.com/docs/cloud-messaging/android/client

//		FirebaseInstanceId.getInstance().getInstanceId()
//				.addOnCompleteListener(task -> {
//					if (!task.isSuccessful()) {
//						Log.w(TAG, "getInstanceId failed: ", task.getException());
//						return;
//					}
//
//					// Get new Instance ID token
//					String token = task.getResult().getToken();
//
//					// Send to Connect to be sent to server...
//					sendWebViewResponse("fcm_push_token", token);
//
//					Log.d(TAG, "on Get Token: " + token);
//				});

        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        // Get new FCM registration token
                        String token = task.getResult();

                        // Send to Connect to be sent to server...
                        sendWebViewResponse("fcm_push_token", token);

                        Log.d(TAG, "on Get Token: " + token);
                    }
                });
    }

    private void setupWebView(){
        webView.addJavascriptInterface(new WebAppInterface(this), "Android");
        webView.setWebViewClient(new ConnectWebViewClient());
        webView.setWebChromeClient(new ConnectWebChromeClient());
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

        webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
            Log.d(TAG, "onDownloadStart: " + url);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            startActivity(i);
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebSettings(WebSettings webSettings){
        webSettings.setJavaScriptEnabled(true);
        webSettings.setSupportZoom(false);
        webSettings.setBuiltInZoomControls(false);

        webSettings.setCacheMode(WebSettings.LOAD_CACHE_ELSE_NETWORK);
        webSettings.setDatabaseEnabled(true);
        webSettings.setDomStorageEnabled(true);
        webSettings.setAllowFileAccess(true);

        webSettings.setGeolocationEnabled(true);
        webSettings.setGeolocationDatabasePath(getFilesDir().getPath());

        webSettings.setMediaPlaybackRequiresUserGesture(false);
        webSettings.setUserAgentString(webSettings.getUserAgentString() + " / ekoconnectandroidwebview");
    }

    /**
     * Handling page navigation
     * https://developer.android.com/guide/webapps/webview#HandlingNavigation
     */
    private class ConnectWebViewClient extends WebViewClient {

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {

            final String host = Uri.parse(url).getHost();
            final String httphost = "http://" + host;
            final String httpshost = "https://" + host;

            // Log.i(TAG, "[CONNECT URL LOAD] HOST=" + host + ", URL=" + url);

            // if (host.equals("connect.eko.in") || host.equals("ekonnect.app") || host.equals("beta.ekoconnect.in") || host.equals("ekoconnect.in"))
            if (httpshost.equals(base_url) || httphost.equals(base_url))
            {
                // This is my website, so do not override; let my WebView load the page
                return false;
            }

            // toast("Opening Link");

            // TODO: Open in Another WebView (and, give option to open in default browser)
            // Otherwise, the link is not for a page on my site, so launch another Activity that handles URLs
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            try {
                startActivity(intent);
            }
            catch (ActivityNotFoundException e) {
                FirebaseCrashlytics.getInstance().recordException(e);
                return true;
            }
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            // toast("Loading Started", Toast.LENGTH_SHORT);
            splash.setVisibility(View.VISIBLE);
            spinner.setVisibility(View.VISIBLE);
            btnReload.setVisibility(View.GONE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            spinner.clearAnimation();
            Log.d(TAG, "onPageFinished: " + url);
            Log.d(TAG, "onPageFinished: "+ String.valueOf(view.getUrl()));

            if ( view.getTitle().toLowerCase().contains("welcome"))
            {
                splash.setVisibility(View.INVISIBLE);
                btnReload.setVisibility(View.INVISIBLE);
            } else {
                btnReload.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Handling page navigation
     * https://developer.android.com/guide/webapps/webview#HandlingNavigation
     */
    private class ConnectWebChromeClient extends WebChromeClient {

        @Override
        public void onGeolocationPermissionsShowPrompt(final String origin, final GeolocationPermissions.Callback callback) {

            mGeolocationRequestOrigin = null;
            mGeolocationCallback = null;

            // Do we need to ask for permission?
            if (ContextCompat.checkSelfPermission(BaseConnectActivity.this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                // Should we show the explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(BaseConnectActivity.this,
                        Manifest.permission.ACCESS_FINE_LOCATION)) {

                    new AlertDialog.Builder(BaseConnectActivity.this)
                            .setMessage(R.string.permission_location_rationale)
                            .setNeutralButton(android.R.string.ok, (dialog, which) -> {
                                mGeolocationRequestOrigin = origin;
                                mGeolocationCallback = callback;
                                ActivityCompat.requestPermissions(BaseConnectActivity.this,
                                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSION_REQUEST_LOCATION);
                            })
                            .show();
                } else {

                    // No explanation needed
                    mGeolocationRequestOrigin = origin;
                    mGeolocationCallback = callback;
                    ActivityCompat.requestPermissions(BaseConnectActivity.this,
                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, MY_PERMISSION_REQUEST_LOCATION);
                }
            } else {

                // Tell the WebView that permission has been granted
                callback.invoke(origin, true, false);
            }
        }


        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {

            Log.d(TAG, "<onShowFileChooser>");

            // make sure there is no existing message
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = null;
                // return true;
            }

            mFilePathCallback = filePathCallback;

            Intent takePictureIntent = null;

            // Log.d(TAG, ">>>> IMAGE CAPTURE INTENT: " + fileChooserParams.isCaptureEnabled() + "   ~~~~    " + fileChooserParams.getAcceptTypes() );

			/* if (fileChooserParams.isCaptureEnabled() && getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
				takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
					// Create the File where the photo should go
					File photoFile = null;
					try {
						photoFile = createImageFile();
						// takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
					} catch (IOException ex) {
						// Error occurred while creating the File
						Log.e(TAG, "Unable to create Image File", ex);
					}

					// Continue only if the File was successfully created
					if (photoFile != null) {

						Uri photoURI = FileProvider.getUriForFile(BaseConnectActivity.this,
								"in.eko.connect.fileprovider",
								photoFile);

						mCameraPhotoPath = "file:" + photoFile.getAbsolutePath();
						takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);          // Uri.fromFile(photoFile)
					} else {
						takePictureIntent = null;
					}
				}
			} */

            // Intent contentSelectionIntent = new Intent(Intent.ACTION_GET_CONTENT);
            // contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            // contentSelectionIntent.setType("image/*");


            Intent contentSelectionIntent = fileChooserParams.createIntent();


            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);

            Intent[] intentArray;
            if (takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser");
            } else {
                intentArray = new Intent[0];
                chooserIntent.putExtra(Intent.EXTRA_TITLE, "Select File");
            }

            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);


            try {

                startActivityForResult(chooserIntent, REQUEST_SELECT_FILE);

            } catch (ActivityNotFoundException e) {

                mFilePathCallback = null;
                toast("Cannot open file chooser", Toast.LENGTH_LONG);
                return false;
            }

            return true;
        }



        @Override
        public void onPermissionRequest(final PermissionRequest request) {

            Log.d(TAG, "onPermissionRequest for " + Arrays.toString(request.getResources()));

            // toast(request.getResources().toString());

			/*runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (request.getOrigin().toString().startsWith(base_url)) {
						request.grant(request.getResources());
					} else {
						request.deny();
					}
				}
			});

			if (true) return; */

            mPermissionRequest = request;

            final String[] requestedResources = request.getResources();
            for (String r : requestedResources) {

                if (PermissionRequest.RESOURCE_VIDEO_CAPTURE.equals(r)) {
                    requestPermissionHelper(request, Manifest.permission.CAMERA, R.string.permission_camera_rationale,
                            MY_PERMISSION_REQUEST_CAMERA, requestedResources);
                }
            }
        }



        // This method is called when the permission request is canceled by the web content.
        @Override
        public void onPermissionRequestCanceled(PermissionRequest request) {
            Log.i(TAG, "onPermissionRequestCanceled");
            // We dismiss the prompt UI here as the request is no longer valid.
            mPermissionRequest = null;
        }
    }

    private void requestPermissionHelper(final PermissionRequest request, final String permission,
                                         final int rationale_string, final int intentCode, String[] other_permissions) {
        // Do we need to ask for permission?
        if (other_permissions == null) {
            other_permissions = new String[]{permission};
        } else {
            other_permissions = Arrays.copyOf(other_permissions, other_permissions.length + 1);
            other_permissions[other_permissions.length-1] = permission;
        }

        mRequestedResources = other_permissions;    // request.getResources();

        if (ContextCompat.checkSelfPermission(BaseConnectActivity.this, permission) != PackageManager.PERMISSION_GRANTED) {

            // Should we show the explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(BaseConnectActivity.this, permission)) {

                new AlertDialog.Builder(BaseConnectActivity.this)
                        .setMessage(rationale_string)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ActivityCompat.requestPermissions(BaseConnectActivity.this, new String[]{permission}, intentCode);
                            }
                        })
                        .show();
            } else {

                // No explanation needed
                Log.d(TAG, "Camera Permission Already Granted");
                ActivityCompat.requestPermissions(BaseConnectActivity.this, new String[]{permission}, intentCode);
            }
        } else {

            if (request != null)
            {
                // other_permissions[other_permissions.length] = permission;
                request.grant(other_permissions);        // new String[]{permission}
            }

            // Tell the WebView that permission has been granted
            // toast("Geolocation Permission");
            // callback.invoke(origin, true, false);
        }
    }

    private void initSecrets() {
        url = configProvider.getUrl();
        uatUrl = configProvider.getUatUrl();
        //attestationKey = configProvider.getAttestationKey();
        googleLoginClientId = configProvider.getGoogleLoginClientId();
        appName = configProvider.getAppName();
        appIcon = configProvider.getAppIcon();
    }
    /**
     * Initialize Firebase Remote Config to fetch certain flags from cloud.
     */
    private void initFirebaseRemoteConfig(){

        // remote config from firebase
        mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance();
        FirebaseRemoteConfigSettings configSettings = new FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(0)
                .build();
        mFirebaseRemoteConfig.setConfigSettingsAsync(configSettings);
        mFirebaseRemoteConfig.setDefaultsAsync(R.xml.default_remote_config);

//        isSafetynetEnabled = mFirebaseRemoteConfig.getString(Params.SAFETY_NET_FLAG)
//                .equals(getString(R.string.enabled));

        isRefreshTokenEnabled = mFirebaseRemoteConfig.getString(Params.REFRESH_TOKEN_FLAG)
                .equals(getString(R.string.enabled));
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(BaseConnectActivity.this, task -> {
                    if (task.isSuccessful()) {

                        Log.i(TAG, "Successfully fetched Firebase Remote Config");
                        //mFirebaseRemoteConfig.activate();

//                        isSafetynetEnabled = mFirebaseRemoteConfig.getString(Params.SAFETY_NET_FLAG)
//                                .equals(getString(R.string.enabled));

                        isRefreshTokenEnabled = mFirebaseRemoteConfig.getString(Params.REFRESH_TOKEN_FLAG)
                                .equals(getString(R.string.enabled));
                        Log.i(TAG, "Successfully fetched Firebase Remote Config " + isSafetynetEnabled);

                    } else {
                        Log.w(TAG, "Failed to get Firebase Remote Config");
                    }
                });


        //loadSafetynetFragment();
        //loadActivity();
    }

    //Checking BioMetric Support
    private void initBiometricAuth(){
        // TODO Phase 2: hasBiometricSupport should be handled inside biometricFactory

        SharedPreferences sharedPreferences = SharedPreferencesUtil.getSharedPreferences(this);
        if (sharedPreferences != null) {
            hasBiometricSupport = sharedPreferences.getInt(Params.BIOMETRIC_SUPPORT,
                    BiometricFactory.UNKNOWN);
        }

        if (hasBiometricSupport == BiometricFactory.HAS_SUPPORT) {
            // Already supported, do nothing here

            // TODO: Need to check for it from the device atleast once a day
            //  A user may have logged in and hasBiometricSupport has been set
            //  the user can go to phone settings are remove biometric.

            // Current Behavior:
            //  If the user removes his biometric PIN/password etc. later
            //  We stop auto login via refresh token
            //  we let user login via mobile otp everytime.

        } else if (hasBiometricSupport == BiometricFactory.UNKNOWN || hasBiometricSupport == BiometricFactory.NO_SUPPORT_SOFT_FAIL) {

            // Check again from device
            hasBiometricSupport = BiometricAuthManager.getInstance(this).checkBiometricSupport();
            SharedPreferencesUtil.saveBiometricSupport(this, hasBiometricSupport);
        } else {
            // Hard fail
        }
        Log.i(TAG, "Is Biometric Enabled: " + hasBiometricSupport);

        // TODO Phase 2
        //  isBiometricEnabled = sharedPreferences.getBoolean(Params.BIOMETRIC_ENABLED, false);
    }

    public void dispatchCachedRefreshToken () {
        // TODO Phase 2
        //  Check if app closed time is within APP_CLOSED_THRESHOLD value
        //  By pass, biometric auth and send refresh token

 		/*
			boolean bypassBiometric = false;
			sharedPreferences = getSharedPreferences();

			if (sharedPreferences != null){
				long appClosedTimestamp = sharedPreferences.getLong(Params.APP_CLOSED_TIME, -1);
				long now = new Date().getTime();


				Log.i(TAG,
						"App Closed Time: " + appClosedTimestamp + " " +
								(now - appClosedTimestamp) + " millis ago [" + APP_CLOSED_THRESHOLD + "]");


				if ((now - appClosedTimestamp) < APP_CLOSED_THRESHOLD) {
					bypassBiometric = true;
				}
			}
		*/

        // If user has long session then
        // after biometric check, the cached refresh token is sent to connect webview
        if (refreshToken != null && !refreshToken.isEmpty() && isLongSession) {

			/*
				if (bypassBiometric) {
					Log.i(TAG, "Bypass Biometric, sending refreshToken " + refreshToken);
					sendWebViewResponse(Params.CACHED_REFRESH_TOKEN, refreshToken);
				}
			 */

            if (hasBiometricSupport == BiometricFactory.HAS_SUPPORT) {
                BiometricAuthManager.getInstance(this).showBiometricForAuth(this); 	// TODO: Add isBiometricEnabled in Phase 2
            }
        }
    }

    @Override
    public void biometricSuccessCallback() {
        // Send to Webview
        sendWebViewResponse(Params.CACHED_REFRESH_TOKEN, refreshToken);

        // Analytics
        sendAnalyticsToWebView(Params.BIOMETRIC_SUCCESS, "Auth success.");
    }

    @Override
    public void biometricFailureCallback() {
        SharedPreferencesUtil.clear(this);

        refreshToken = "";
        isLongSession = false;
        // isBiometricEnabled = false; 		// For Phase 2

        // Analytics
        sendAnalyticsToWebView(Params.BIOMETRIC_FAILURE, "Incorrect biometric.");
    }

    @Override
    public void biometricAdviceCallback(String advice) {
        // Don't show any advice to user (for now), keeping things at the backend
        // toast(advice, Toast.LENGTH_LONG);
    }

    @Override
    public void biometricEnableCallback() {
        // isBiometricEnabled = true;		// Phase 2
        // SharedPreferencesUtil.saveBiometricEnabled(getSharedPreferences(), isBiometricEnabled);
    }

    @Override
    public void biometricErrorCallback(String error) {
        // isBiometricEnabled = true; 		// Phase 2
        // toast("Authentication error: " + error, Toast.LENGTH_LONG);

        // Analytics
        sendAnalyticsToWebView(Params.BIOMETRIC_FAILURE, error);
    }

    // TODO Phase 2
    private void promptUserToEnableBiometric() {
        BiometricAuthManager.getInstance(this).showBiometricToEnable();
    }

    private void initSessionFromSharedPreferences() {
        SharedPreferences sharedPreferences = SharedPreferencesUtil.getSharedPreferences(this);
        if (sharedPreferences != null) {
            refreshToken = sharedPreferences.getString(Params.REFRESH_TOKEN, "");
            isLongSession = sharedPreferences.getBoolean(Params.LONG_SESSION, false);
        }

        Log.i(TAG, "Is Long Session: " + isLongSession);
    }

    private void toast(String msg) {
        toast(msg, Toast.LENGTH_SHORT);
    }

    private void toast(String msg, int length) {
        Toast.makeText(getApplicationContext(), msg, length).show();
    }

    private void keepScreenOn(String status) {
        if (status.equals("1")) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    /*
    When safetynet successfully attests the nonce token and gets the
    JWS signature from Google. Send this signature to webview
    */
    @Override
    public void sendDataToActivity(String signature, Boolean safetynetSuccess) {
        if (safetynetSuccess) {
            try {

                sendAnalyticsToWebView(Params.SAFETYNET_SUCCESS, "Signature attestation success.");

                // Send attestation signature, nonce and secure lock to webview.
                JSONObject obj = new JSONObject();
                obj.put(Params.NONCE, nonce);
                obj.put(Params.SIGNATURE, signature);
                obj.put(Params.SECURE_LOCK, hasBiometricSupport == BiometricFactory.HAS_SUPPORT);

                sendWebViewResponse(Params.ATTESTATION_TOKEN_RESPONSE, obj.toString());

            } catch (Exception e) {
                Log.e(TAG,  EXCEPTION_PREFIX + e);

                sendAnalyticsToWebView(Params.SAFETYNET_FAILURE, e.getMessage());

                sendWebViewResponse(Params.ATTESTATION_TOKEN_RESPONSE, "{exception: " + e + "}");
            }

        } else {

            // TODO: safetynet has failed. 	(1) Don't show mobile button (BAU)
            //								(2) Notify webapp

            sendAnalyticsToWebView(Params.SAFETYNET_FAILURE, "Signature attestation failed.");

            try {
                JSONObject obj = new JSONObject();
                obj.put("error", signature);
                sendWebViewResponse(Params.ATTESTATION_TOKEN_RESPONSE, obj.toString());
            } catch (Exception e) {
                Log.e(TAG, EXCEPTION_PREFIX + e);
                sendWebViewResponse(Params.ATTESTATION_TOKEN_RESPONSE, "{exception: " + e + "}");
            }
        }
    }

    @Override
    public void registerGoogleEvent(String code, String event) {
        String android_id = Settings.Secure.getString(this.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(Params.DEVICE_ID, android_id);
            jsonObject.put(Params.CODE, code);
            jsonObject.put(Params.EVENT, event);
            sendWebViewResponse(Params.GOOGLE_EVENT, jsonObject.toString());
        } catch (Exception e) {
            Log.e(TAG, EXCEPTION_PREFIX + e.getMessage());
            sendWebViewResponse(Params.GOOGLE_EVENT_FAILED, e.toString());
        }
    }

    @Override
    public void onRDServiceDriverDiscovery(String rdServiceInfo, String rdServicePackage, Boolean isWhitelisted) {
        Log.d(TAG, "onRDServiceDriverDiscovery: " + isWhitelisted + " ~ " + rdServiceInfo + " ~ " + rdServicePackage);

        sendWebViewResponse("rdservice_info",
                rdServiceInfo +
                        "<RD_SERVICE_ANDROID_PACKAGE=\"" + rdServicePackage + "\" " +
                        (isWhitelisted ? "WHITELISTED" : "") +
                        " />");
    }

    @Override
    public void onRDServiceCaptureResponse(String pidData, String rdServicePackage) {
        Log.d(TAG, "onRDServiceCaptureResponse: " + pidData);

        sendWebViewResponse("rdservice_resp", pidData);
    }

    @Override
    public void onRDServiceDriverNotFound() {
        Log.d(TAG, "onRDServiceDriverNotFound");
        sendWebViewResponse("rdservice_discovery_failed", "");
    }

    @Override
    public void onRDServiceDriverDiscoveryFailed(int resultCode, Intent data, String pkg, String reason) {
        Log.d(TAG, "onRDServiceDriverDiscoveryFailed: " + resultCode + " ~ " + data + " ~ " + pkg + " ~ " + reason);
    }

    @Override
    public void onRDServiceCaptureFailed(int resultCode, Intent data, String pkg) {
        Log.d(TAG, "onRDServiceCaptureFailed: " + resultCode + " ~ " + data + " ~ " +  pkg);
        sendWebViewResponse("rdservice_resp", "");
    }

    /**
     * EventBus Message Subscription
     * @param event A simple event that contains token
     */
    @Subscribe
    public void onNotificationRegisterEvent(NotificationRegisterEvent event) {

        // Send to Connect to be sent to server...
        sendWebViewResponse("fcm_push_token", event.token);
    }

    /**
     * EventBus Message Subscription
     * @param event A simple event that contains token
     */
    @Subscribe
    public void onNotificationMessageEvent(NotificationMessageEvent event) {

        // Send to Connect to be sent to server...
        sendWebViewResponse("fcm_push_msg", event.notification_data);
    }

    private void dispatchPendingConnectmessages() {
        Log.d(TAG, "connectMsgQueue: dispatching pending msgs... " + connectMsgQueue.size());

        while (connectMsgQueue.size() > 0) {
            try {
                WebViewMessage m = connectMsgQueue.remove();
                sendWebViewResponse(m.action, m.data);
            } catch (NoSuchElementException e) {
                break;
            }
        }
    }

    public void reloadWebView() {
        webView.reload();
        btnReload.setVisibility(View.INVISIBLE);
    }


    // ===========================================================================================================
    //                               WEBVIEW COMMUNICATION
    // ===========================================================================================================

    /**
     * Android Action requested by Connect
     * @param action action sent by connect webapp
     * @param data data sent by connect webapp
     */
    public void doAction(String action, String data) {
        Log.d(TAG, "Android Action From Connect: " + action + "("+data+")");

        switch (action) {
            case "connect_ready":
                Log.d(TAG, "Connect Ready");
                connectReady = true;
                dispatchPendingConnectmessages();
                mLocationHelper.enableGPS();
                break;

            case "check_active_login":
                    mGoogleLogin.checkActiveGoogleLogin(this);
                break;

            case "google_login":
                mGoogleLogin.googleSignin();
                break;

            case "google_logout":
                mGoogleLogin.googleSignout();
                break;

            case "fb_login":
                //mFacebookSign.fbSignin();
                break;

            case "fb_logout":
                //mFacebookSign.fbSignout();
                break;

            case "mobile_request_hint":
                try {
                    requestPhoneNumberHint();
                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }
                break;

            case "discover_rdservice":
                rdServiceManager.discoverRdService();
                break;

            case "capture_rdservice":
                try {
                    JSONObject jsonData = new JSONObject(data);

                    String _package = jsonData.getString("package");
                    String _pidopts = jsonData.getString("pidopts");
                    rdServiceManager.captureRdService(_package, _pidopts);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                break;

            case "save_file_blob":
                saveFileFromBlob(data);
                break;

            case "share":
                shareContent(data);
                break;

            case "print_page":
                if (webView != null && connectReady) {
                    createWebPrintJob(webView, data);
                }
                break;

            case "razorpay_open":
                startRazorPayPayment(data);
                break;

            case "leegality_esign_open":
                startLeegalityESign(data);
                break;

            case "keep_screen_on":
                keepScreenOn(data);
                break;

            case "grant_permission":
                grantPermission(data);
                break;

            case "get_geolocation":
                getGeoLocation();
                break;

            case "check_android_permission":
                checkAndroidPermissionForConnect(data);
                break;

            case "open_app_sys_settings":
                openAppSystemSettings(this, data);
                break;

            case "enable_gps_prompt":
                mLocationHelper.enableGPS();
                break;

            //------ Safetynet protocols ----------/

            case "get_attestation_token":
                //handleGetAttestationToken(data);
                break;

            case "save_refresh_token":
                handleSaveAuthTokens(data);
                break;

            case "clear_refresh_token":
                handleClearSharedPreferences();
                break;

            case "otp_fetch_request":
                startOTPListener();
                break;

            case "open_upi_payment":
                handleUpiPayment(data);
                break;

            default:
                Log.e(TAG, "Invalid action");
        }
    }

    private void handleUpiPayment(String data) {
        Uri uri = Uri.parse(data);

        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);

        Intent chooser = Intent.createChooser(upiPayIntent, "Pay With");

        if (chooser.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(chooser, UPI_PAYMENT);
        } else {
            Toast.makeText(this, "No UPI app found, please install one to continue",
                    Toast.LENGTH_SHORT).show();
            onUpiPaymentFailed();
        }
    }

    private void onUpiPaymentSuccess(Intent data) {
        String response = data.getStringExtra("response");
        //Log.d(TAG, "response ="+response);
        if (response == null || response.length() < 10) {
            onUpiPaymentFailed();
            return;
        }
        String status = getStatusFromMessage(response).equalsIgnoreCase("success") ? "success" : "failed";
        sendWebViewResponse(UPI_PAYMENT_RESPONSE, status);

    }

    private String getStatusFromMessage(String message) {
        int start = message.indexOf("Status") + 7;
        int end = message.indexOf('&', start);
        if (end == -1) end = message.length()-1;
        //System.out.println("start = "+start+" "+end+", "+message.substring(start, end));
        if (end > start) {
            return message.substring(start, end);
        } else {
            return "";
        }
    }

    private void onUpiPaymentFailed() {
        sendWebViewResponse(UPI_PAYMENT_RESPONSE, "failed");
    }

    private void grantPermission(String permission_type) {
        if (mPermissionHelper == null) {
            mPermissionHelper = new PermissionHelper(this);
        }

        PermissionCallback callback = new PermissionCallback() {
            @Override
            public void onPermissionGranted() {
                sendWebViewResponse(Params.PERMISSION_CHECK_RESPONSE, ""+PackageManager.PERMISSION_GRANTED);
            }

            @Override
            public void onPermissionDenied() {
                sendWebViewResponse(Params.PERMISSION_CHECK_RESPONSE, ""+PackageManager.PERMISSION_DENIED);
            }

            @Override
            public void onPermissionDeniedBySystem() {
                displayDialog(permission_type);
            }
        };

        switch (permission_type) {
            case "LOCATION":
                mPermissionHelper.request(callback, new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
                break;

            case "CAMERA":
                mPermissionHelper.request(callback, new String[] {Manifest.permission.CAMERA});
                break;

            default:
                break;
        }
    }

    private void displayDialog(String permission_type) {
        String permission = permission_type.substring(0,1).toUpperCase() + permission_type.substring(1).toLowerCase();
        new AlertDialog.Builder(this)
                .setTitle(getString(R.string.permission_alert_dialog_title, permission))
                .setMessage(getString(R.string.permission_alert_dialog_message, permission.toLowerCase()))
                .setPositiveButton(getString(R.string.allow), (dialog, which) -> {
                    openAppSystemSettings(getBaseContext(), permission_type);
                })
                .setNegativeButton(getString(android.R.string.cancel), (dialog, which) -> {
                    sendWebViewResponse(Params.PERMISSION_CHECK_RESPONSE, ""+PackageManager.PERMISSION_DENIED);
                })
                .show();
    }

    private void getGeoLocation() {
        if (mPermissionHelper == null) {
            mPermissionHelper = new PermissionHelper(this);
        }

        if (mPermissionHelper.checkSelfPermission(new String[] {Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION})) {
            mLocationHelper.getLocation();
        } else {
            JSONObject response = new JSONObject();
            try {
                response.put("status", 1);
                response.put("code", 1);
                response.put("message", "Permission denied");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            sendWebViewResponse(Params.GEOLOCATION_RESPONSE, response.toString());
        }

    }

    // interaction to web app through this
    // send data to web app
    public void sendWebViewResponse(final String action, final String data) {
        if (connectReady) {
            webView.post(() -> {
                Log.d(TAG, data);
                String lineData = data.replaceAll("[\\n\\r]+", " ").replaceAll("'", "\'");
                Log.d(TAG, "sendWebViewResponse: " +
                        "\naction: " + action +
                        "\ndata: " + data +
                        "\n ==> " + "callFromAndroid('" + action + "', '" + lineData + "')\n");

                String script = "javascript:callFromAndroid('" + action + "', '" + lineData + "')";

                webView.evaluateJavascript(script, value -> {
                    // Value returned by the Javascript function
                });
            });
        } else {
            // Connect not yet ready. Cache for later...
            Log.d(TAG, "connectMsgQueue.add: " + action + "  ,  " + data);
            connectMsgQueue.add(new WebViewMessage(action, data));
        }
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.i(TAG, "onActivityResult: " + requestCode + ", " + resultCode);

        if (requestCode == LocationHelper.REQUEST_CHECK_SETTINGS) {
            mLocationHelper.onLocationDialogResult(resultCode, data);
        }

        if (resultCode == RESULT_OK) {

            if (rdServiceManager != null) {
                rdServiceManager.onActivityResult(requestCode, resultCode, data);
            }

            switch (requestCode) {
                case RC_GOOGLE_SIGN_IN:
                    // Result returned from launching the Intent from GoogleSignInClient.getSignInIntent(...);
                    mGoogleLogin.handleGoogleSignInResult(data);
                    break;

                case REQUEST_SELECT_FILE:
                    requestSelectFile(resultCode, data);
                    break;

                case RESOLVE_PHONE_NUMBER_HINT:
                    onPhoneSelectorHintResponse(data);
                    break;

                case PLAY_SERVICES_RESOLUTION_REQUEST:
                    //callSafetyNetForAttestation();
                    break;

                case RC_LEEGALITY_ESIGN:
                    callLeegalityEsign(data);
                    break;

                case UPI_PAYMENT:
                    onUpiPaymentSuccess(data);
                    break;
                default:


            }
        } else {
            switch (requestCode) {
                case RC_GOOGLE_SIGN_IN:
                    toast(getString(R.string.google_login_failed));
                    break;

                case REQUEST_SELECT_FILE:
                    if (mFilePathCallback != null)
                    {
                        mFilePathCallback.onReceiveValue(null);
                        mFilePathCallback = null;
                    }
                    // toast("File select cancelled");
                    break;

                case RC_APP_UPDATE:
                    toast("App update failed!");
                    break;

                case RC_LEEGALITY_ESIGN:
                    leegalityESignFailed();
                    break;

                case UPI_PAYMENT:
                    onUpiPaymentFailed();
                    break;

                default:
					/*
					 Error case data could be null. When on Battery Saver mode,
					 someone trying to connect AePS Fingerprint device, phone may close the
					 activity and data comes null - NullPointerException
				 	*/
//					toast("Something went wrong!");
                    //sendWebViewResponse();
                    Log.e(TAG,
                            "Something went wrong onActivityResult: " +
                                    "\nRequest Code: " + requestCode +
                                    "\nResult Code: " + resultCode +
                                    "\nData: " + ((data != null)? data.getDataString(): "") + "\n");
            }

            Log.w(TAG,"onActivityResult FAILED (req/result/data): "
                    + requestCode + " ~ " + resultCode + " ~ " + data);

        }
    }

    public void requestSelectFile(int resultCode, Intent data){
        if (mFilePathCallback == null) return;

        Uri[] results = null;

        if (data.getData() == null) {
            Log.w(TAG, REQUEST_SELECT_FILE + " getData is null");
            // If there is not data, then we may have taken a photo
						/*
							if (mCameraPhotoPath != null) {
								results = new Uri[]{Uri.parse(mCameraPhotoPath)};
							}
						*/
        } else {
            results = WebChromeClient.FileChooserParams.parseResult(resultCode, data);
        }

        mFilePathCallback.onReceiveValue(results);
        mFilePathCallback = null;
    }

    // ===========================================================================================================
    //                                 SAFETYNET
    // ===========================================================================================================


//    private void handleGetAttestationToken(String data) {
//
//        Log.i(TAG, "Safetynet Enabled: " + isSafetynetEnabled);
//
//        if (isSafetynetEnabled) {
//            try {
//
//                Log.i(TAG, "Verifying device via Safetynet");
//                JSONObject jsonObject = new JSONObject(data);
//                nonce = jsonObject.getString("nonce_token");
//
//                Log.i(TAG, "Calling SafetyNet for Attestation");
//                callSafetyNetForAttestation();
//            } catch (Exception e) {
//                e.printStackTrace();
//                Log.e(TAG, "Error while calling safetynet for attestation: " + e.getMessage());
//            }
//
//        }
//    }

    /**
     * Check google play version. if not updated,
     * ask the user to update then call safety net
     */
//    public void callSafetyNetForAttestation() {
//
//        if (GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this, 13000000) ==
//                ConnectionResult.SUCCESS) {
//            // The SafetyNet Attestation API is available.
//            View current = getWindow().getCurrentFocus();
//            safetynetFragment.sendSafetyNetRequest(current, nonce);
//
//        } else {
//            // API not available.
//
//            GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
//            int result = googleAPI.isGooglePlayServicesAvailable(this);
//
//            if(googleAPI.isUserResolvableError(result)) {
//
//                // Prompt user to update Google Play Services.
//                googleAPI.getErrorDialog(this,result,PLAY_SERVICES_RESOLUTION_REQUEST).show();
//            }
//
//        }
//    }

    private void handleClearSharedPreferences(){
        SharedPreferencesUtil.clear(this);
    }

    private void handleSaveAuthTokens(String data) {
        try {
            JSONObject jsonData = new JSONObject(data);

            if (jsonData.has(Params.REFRESH_TOKEN)) {
                // Overriding refresh token with new one
                refreshToken = jsonData.getString(Params.REFRESH_TOKEN);
                Log.d(TAG, "handleSaveAuthTokens: " + refreshToken);
            }
            if (jsonData.has(Params.LONG_SESSION)) {
                // Overriding long session with new one
                isLongSession = jsonData.getBoolean(Params.LONG_SESSION);
                Log.d(TAG, "handleSaveAuthTokens: "+ isLongSession);
            }


            Log.i(TAG, "New Auth Tokens : \n" + jsonData.toString());

            SharedPreferencesUtil.saveAuthTokens(this, refreshToken, isLongSession);


			/*
			  -> Phase 1: If user has biometric enrolled, the automatically use biometric login

			  Phase 2: Prompt user to enable biometric. If enabled or not, handle both cases.
			    If enabled, then authenticate via biometric during login.


				// Check if long session, don't show biometric for Google/FB
				if (isLongSession) {

					if (jsonData.has(Params.NEW_LOGIN) && jsonData.getBoolean(Params.NEW_LOGIN)){

						// Check if biometric is already enabled
						// if yes, then don't need to enable biometric
						if (!sharedPrefs.getBoolean(Params.BIOMETRIC_ENABLED, false)){
							promptUserToEnableBiometric();
						}
					}
				}

		 	*/


        } catch (JSONException e) {
            e.printStackTrace();

            Log.e(TAG, "ERROR: " + e.getMessage());
        }
    }


    // ===========================================================================================================
    // 											ANALYTICS
    // ===========================================================================================================
    public void sendAnalyticsToWebView(String action, String label) {
        try {
            JSONObject _analytics = new JSONObject();
            _analytics.put("action", action);
            _analytics.put("label", label);

            sendWebViewResponse(Params.TRACK_EVENT, _analytics.toString());
        } catch (Exception e) {

            Log.e(TAG, "Error sending analytic for action: " + action + " label: " + label);
        }

    }


    // ===========================================================================================================
    //                                  GOOGLE CREDENTIALS API -  MOBILE LOGIN
    // ===========================================================================================================

    // Construct a request for phone numbers and show the picker
    private void requestPhoneNumberHint() throws IntentSender.SendIntentException {
        mGoogleLogin.mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Auth.CREDENTIALS_API)
                .build();

        HintRequest hintRequest = new HintRequest.Builder()
                .setPhoneNumberIdentifierSupported(true)
                .build();

        PendingIntent intent = Auth.CredentialsApi.getHintPickerIntent(
                mGoogleLogin.getGoogleApiClient(), hintRequest);

        startIntentSenderForResult(intent.getIntentSender(),
                RESOLVE_PHONE_NUMBER_HINT, null, 0, 0, PendingIntent.FLAG_IMMUTABLE);

    }

    private void onPhoneSelectorHintResponse(Intent data) {
        Credential credential = data.getParcelableExtra(Credential.EXTRA_KEY);
        // credential.getId(); <-- E.164 format phone number on 10.2.+ devices
        // toast(credential.getId());
        sendWebViewResponse("mobile_hint_response", ""+credential.getId());
    }

    // ===========================================================================================================
    //                                 REQUEST ANDROID PERMISSIONS
    // ===========================================================================================================
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MY_PERMISSION_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty

                if (mGeolocationCallback == null || mGeolocationRequestOrigin == null) {
                    break;
                }

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was grated!
                    mGeolocationCallback.invoke(mGeolocationRequestOrigin, true, true);
                    sendWebViewResponse(Params.PERMISSION_CHECK_RESPONSE, "0");
                } else {

                    // Permission denied...disable functionality that depends on this permission
                    mGeolocationCallback.invoke(mGeolocationRequestOrigin, false, true);
                }
            }
            break;
            
            case MY_PERMISSION_REQUEST_CAMERA: {
                // If request is cancelled, the result arrays are empty

                if (mPermissionRequest == null) {
                    break;
                }

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // Permission was grated!
                    // mPermissionRequest.grant(new String[]{Manifest.permission.CAMERA});
                    // mPermissionRequest.grant(permissions);
                    if (mRequestedResources.length > 0) {
                        // mPermissionRequest.grant(mRequestedResources);
                        for (String r : mRequestedResources) {
                            if (r.equals(PermissionRequest.RESOURCE_VIDEO_CAPTURE)) {
                                mPermissionRequest.grant(new String[]{PermissionRequest.RESOURCE_VIDEO_CAPTURE});
                                break;
                            }
                        }
                    } else {
                        mPermissionRequest.grant(new String[]{Manifest.permission.CAMERA});
                    }
                    Log.d(TAG, "Permission granted.");
                } else {

                    // Permission denied...disable functionality that depends on this permission
                    mPermissionRequest.deny();
                    Log.d(TAG, "Permission request denied.");
                }
            }
            break;

            case REQUEST_WRITE_EXT_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted
                    // contacts-related task you need to do.
                    if (mFileSaveBlobCache != null) {
                        saveFileFromBlob(mFileSaveBlobCache);
                    }
                } else {
                    // permission denied
                    toast(getString(R.string.file_save_permission_rejected));
                }
            }
            break;

            case PermissionHelper.REQUEST_CODE:
                if (mPermissionHelper != null) {
                    mPermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }

            // Handle other permissions that may be requested by the web app by adding corresponding cases
        }
    }


    private void checkAndroidPermissionForConnect(String permission_type)
    {
        int perm = 1;
        switch (permission_type)
        {
            case "LOCATION":
                perm = ContextCompat.checkSelfPermission(BaseConnectActivity.this, Manifest.permission.ACCESS_FINE_LOCATION);
                break;

            case "CAMERA":
                perm = ContextCompat.checkSelfPermission(BaseConnectActivity.this, Manifest.permission.CAMERA);
                break;

            case "STORAGE":
                perm = ContextCompat.checkSelfPermission(BaseConnectActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                break;
        }

        // Log.w(TAG, "checkAndroidPermissionForConnect: PERM RESULT: " + perm);

        sendWebViewResponse(Params.PERMISSION_CHECK_RESPONSE, ""+perm);
    }


    private void openAppSystemSettings(Context context, String permission_type)
    {
        _app_system_settings_opened = true;
        _app_system_settings_opened_for = permission_type;

        final Intent i = new Intent();
        i.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        i.addCategory(Intent.CATEGORY_DEFAULT);
		i.setData(Uri.parse("package:" + context.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        i.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        i.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(i);

        toast("Open PERMISSIONS and enable " + permission_type, Toast.LENGTH_LONG);  // Open PERMISSIONS and enable Location
    }

    @Override
    protected void onStart () {
        super.onStart();
        //The fetchAndActivate() method is called on the Firebase Remote Config instance.
        // This method fetches the latest configuration from Firebase and then activates the fetched values,
        // making them available for use.
        mFirebaseRemoteConfig.fetchAndActivate()
                .addOnCompleteListener(BaseConnectActivity.this, task -> {
                    if (task.isSuccessful()) {

                        Log.i(TAG, "Successfully fetched Firebase Remote Config");
                        //mFirebaseRemoteConfig.activate();

                        isSafetynetEnabled = mFirebaseRemoteConfig.getString(Params.SAFETY_NET_FLAG)
                                .equals(getString(R.string.enabled));

                        isRefreshTokenEnabled = mFirebaseRemoteConfig.getString(Params.REFRESH_TOKEN_FLAG)
                                .equals(getString(R.string.enabled));
                        Log.i(TAG, "Successfully fetched Firebase Remote Config " + isSafetynetEnabled);

                    } else {
                        Log.w(TAG, "Failed to get Firebase Remote Config");
                    }
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Log.i(TAG, "onResume: " + _app_system_settings_opened +"  -  " + _app_system_settings_opened_for);
        if (_app_system_settings_opened) {
            _app_system_settings_opened = false;
            checkAndroidPermissionForConnect(_app_system_settings_opened_for);
        }
    }

    @Override
	protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        // Native DeepLinks............................
        // ATTENTION: This was auto-generated to handle app links.
        Intent appLinkIntent = getIntent();
        String appLinkAction = appLinkIntent.getAction();
        Uri appLinkData = appLinkIntent.getData();
        if (appLinkAction != null && appLinkAction.equals("android.intent.action.VIEW")
                && appLinkData != null) {
            Log.d(TAG, "[Native DeepLink onNewIntent] " + appLinkAction + " / " + appLinkData
                    + " // " + appLinkData.getEncodedPath());

            handleDeepLink(appLinkData);
        }
    }

    // ===========================================================================================================
    //                                                  FILE DOWNLOAD
    // ===========================================================================================================

    private void saveFileFromBlob(String data)
    {
        JSONObject jsonData;
        String _blob, _name;


        // Check if external storage is available (eg: storage not mounted on PC)
        String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            // External Storge Not Available
            toast(getString(R.string.storage_unavailable));
            return;
        }


        // Check for write-permissions
        if (ContextCompat.checkSelfPermission(BaseConnectActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted.

            // Cache until file write permission is granted
            mFileSaveBlobCache = data;

            // Ask for permission.
            ActivityCompat.requestPermissions(BaseConnectActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    REQUEST_WRITE_EXT_STORAGE);
            // Try file save again after permission is granted
            return;
        }

        mFileSaveBlobCache = null;  // Delete older cache, if any.


        try
        {
            jsonData = new JSONObject(data);
            _blob = jsonData.getString("blob");
            // String _type = jsonData.getString("type");
        }
        catch (JSONException e)
        {
            toast(getString(R.string.file_save_failed));
            e.printStackTrace();
            return;
        }

        try
        {
            _name = jsonData.getString("name");
        }
        catch (JSONException e)
        {
            _name = "file";
        }

        try
        {
            final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS) + "/" + _name);

            Log.d(TAG, "FILE DOWNLOAD FROM BLOB REQUEST: " + dwldsPath.toString());

            byte[] fileAsBytes = Base64.decode(_blob, 0);
            FileOutputStream os;
            os = new FileOutputStream(dwldsPath, false);
            os.write(fileAsBytes);
            os.flush();
            toast(getString(R.string.file_saved));
        } catch (IOException e)
        {
            toast(getString(R.string.file_save_failed));
            e.printStackTrace();
        }
    }

    // ===========================================================================================================
    //                                                  PRINT PAGE
    // ===========================================================================================================
    private void createWebPrintJob(final WebView webView, final String document_title) {
        // Get a PrintManager instance
        final PrintManager printManager = (PrintManager) getSystemService(Context.PRINT_SERVICE);

        final String jobName = (document_title == null || document_title.equals("")) ?
                getString(R.string.app_name) + " Document" : document_title;

        runOnUiThread(() -> {
            // Get a print adapter instance
            PrintDocumentAdapter printAdapter = webView.createPrintDocumentAdapter(jobName);

            long currentTimeStamp = new Date().getTime();
            if (currentTimeStamp - mLastPrintTimeStamp > PRINT_TIME_THRESHOLD_VALUE) {
                printManager.print(jobName, printAdapter, new PrintAttributes.Builder().build());
            } else {
                Log.d(TAG, "skip print event because of double tap");
            }
            mLastPrintTimeStamp = currentTimeStamp;;

            // Save the job object for later status checking
            // mPrintJobs.add(printJob);
        });
    }

    // ===========================================================================================================
    //                                                  RAZOR-PAY
    // ===========================================================================================================
    public void startRazorPayPayment(String strOptions) {
        Checkout checkout = new Checkout();
        checkout.setKeyID(BuildConfig.razorpay_api_key);

        /*
         * Set your logo here
         */
        checkout.setImage(R.drawable.eko_logo);

        /*
         * Pass your payment options to the Razorpay Checkout as a JSONObject
         */
        try {
            JSONObject options = new JSONObject(strOptions);
            checkout.open(BaseConnectActivity.this, options);
        } catch(Exception e) {
            Log.e(TAG, "Error in starting Razorpay Checkout", e);
        }
    }

    @Override
    public void onPaymentSuccess(String razorpayPaymentID) {
        /*
         * Add your logic here for a successful payment response
         */
        sendWebViewResponse("razorpay_response", razorpayPaymentID);
    }

    @Override
    public void onPaymentError(int code, String response) {
        /*
         * Add your logic here for a failed payment response
         */
        sendWebViewResponse("razorpay_response", "");
    }

    // ===========================================================================================================
    //                                        LEEGALITY Document eSign
    // ===========================================================================================================

    String document_id;
    private void callLeegalityEsign(Intent data){
        String error = data.hasExtra("error") ? data.getExtras().getString("error") : null;
        String message = data.hasExtra("message") ? data.getExtras().getString("message") : "";
        Log.d(TAG, "callLeegalityEsign:  " +  message);
        if (error != null) {
            leegalityESignFailed();
        } else if (message != null) {
            onLeegalityEsignSuccess();
        } else {
            leegalityESignFailed();
        }
    }

    public void startLeegalityESign(String strOptions) {

        // Leegality Documentation: https://gitlab.leegality.com/leegality-public/android-sdk/tree/v4.2

        String signing_url;

        try {
            JSONObject options = new JSONObject(strOptions);
            signing_url = options.getString("signing_url");
            document_id= options.getString("document_id");
        }
        catch (JSONException je) {
            Log.e(TAG, "ERROR parsing Leegality parameters: ", je);
            leegalityESignFailed();
            return;
        }

        Intent intent = new Intent(getApplicationContext(), Leegality.class);
        intent.putExtra("url", signing_url);
        startActivityForResult(intent, RC_LEEGALITY_ESIGN);
    }


    // Callback listener functions
    public void onLeegalityEsignSuccess()
    {
        //Create Json Object
        try{
            JSONObject obj = new JSONObject();
            obj.put("agreement_status" , "success");
            obj.put("document_id" , document_id);
            sendWebViewResponse(Params.LEEGALITY_ESIGN_RESPONSE, obj.toString());
        } catch (Exception e ){
            sendWebViewResponse(Params.LEEGALITY_ESIGN_RESPONSE , "error");
        }
    }



    public void leegalityESignFailed()
    {
        sendWebViewResponse(Params.LEEGALITY_ESIGN_RESPONSE, "");
    }

    // ===========================================================================================================
    //                                        APP UPDATES
    // ===========================================================================================================


    InstallStateUpdatedListener installStateUpdatedListener = new
            InstallStateUpdatedListener() {
                @Override
                public void onStateUpdate(InstallState state) {
                    if (state.installStatus() == InstallStatus.DOWNLOADED){
                        popupSnackbarForCompleteUpdate();
                    } else if (state.installStatus() == InstallStatus.INSTALLED){
                        if (mAppUpdateManager != null){
                            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
                        }

                    } else {
                        Log.i(TAG, "InstallStateUpdatedListener: state: " + state.installStatus());
                    }
                }
            };

    private void checkForUpdates() {

        mAppUpdateManager = AppUpdateManagerFactory.create(this);
        mAppUpdateManager.registerListener(installStateUpdatedListener);

        mAppUpdateManager.getAppUpdateInfo().addOnSuccessListener(appUpdateInfo -> {

            if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE
                    && appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)){

                try {
                    mAppUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo, AppUpdateType.FLEXIBLE, BaseConnectActivity.this, RC_APP_UPDATE);

                } catch (IntentSender.SendIntentException e) {
                    e.printStackTrace();
                }

            } else if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                popupSnackbarForCompleteUpdate();
            } else {
                Log.e(TAG, "checkForAppUpdateAvailability: something else");
            }
        });
    }

    private void popupSnackbarForCompleteUpdate() {

        Snackbar snackbar =
                Snackbar.make(
                        findViewById(R.id.activity_connect_webview),
                        "New app is ready!",
                        Snackbar.LENGTH_INDEFINITE);

        snackbar.setAction("Install", view -> {
            if (mAppUpdateManager != null) {
                mAppUpdateManager.completeUpdate();
            }
        });

        snackbar.setActionTextColor(getResources().getColor(R.color.colorAccent));
        snackbar.show();
    }

    // ===========================================================================================================
    //                                        Share Content
    // ===========================================================================================================

    private void shareContent(String data) {
        JSONObject options = null;
        String message = null;
        try {
            options = new JSONObject(data);
            message = options.getString("text");
        } catch (JSONException e) {
            e.printStackTrace();
            return;
        }

        if (message == null) {
            Log.e(TAG, "No referral code found");
            return;
        }

        Intent intentShare = new Intent(Intent.ACTION_SEND);
        intentShare.setType("text/plain");
        intentShare.putExtra(Intent.EXTRA_TEXT,message);

        startActivity(Intent.createChooser(intentShare, "Share referral code via..."));
    }

    // ===========================================================================================================
    //                                        APP UPDATES
    // ===========================================================================================================
    private void startOTPListener() {
        mSMSReceiver = new MySmsBroadcastReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SmsRetriever.SMS_RETRIEVED_ACTION);
        registerReceiver(mSMSReceiver, intentFilter);

        SmsRetrieverClient client = SmsRetriever.getClient(this);
        Task<Void> task = client.startSmsRetriever();

        task.addOnSuccessListener(aVoid -> {
            mSMSReceiver.initSMSListener(new SMSListener() {
                @Override 
                public void onSuccess(String message) {
                    // Log.d(TAG, "onSuccess : "+message);
                    if (message != null) {
                        String otp = fetchOTP(message);
                        if (otp != null) {
                            sendWebViewResponse(Params.OTP_FETCH_RESPONSE, otp);
                        } else {
                            Log.d(TAG, "OTP not found in message");
                        }
                    }
                    unRegisterSMSReceiver();
                }

                @Override
                public void onError(String message) {
                    Log.d(TAG, "OnError : "+message);
                    unRegisterSMSReceiver();
                }
            });
        });

        task.addOnFailureListener(e -> {
            e.printStackTrace();
            unRegisterSMSReceiver();
        });
    }

    private String fetchOTP(String message) {
        Pattern pattern = Pattern.compile("(?:^|\\W)([0-9]{3,8}+)(?:$|\\W)");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(0).trim();
        }
        return null;
    }

    private void unRegisterSMSReceiver() {
        if (mSMSReceiver != null) {
           // unregisterReceiver(mSMSReceiver);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        BiometricAuthManager.getInstance(this).onDestroy();

        // Store app paused/destroy time

        // Deregister App Update Listener
        if (mAppUpdateManager != null) {
            mAppUpdateManager.unregisterListener(installStateUpdatedListener);
        }

        unRegisterSMSReceiver();

        if (mGpsSwitchReceiver != null) {
            unregisterReceiver(mGpsSwitchReceiver);
        }

        if (mLocationHelper != null) {
            mLocationHelper.destroy();
        }

        if (mPermissionHelper != null) {
            mPermissionHelper = null;
        }
    }
}
