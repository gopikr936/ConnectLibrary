//package in.eko.connectlib.auth;

//import android.content.Context;
//import android.util.Log;
//import android.widget.Toast;
//
//import com.facebook.AccessToken;
//import com.facebook.CallbackManager;
//import com.facebook.FacebookCallback;
//import com.facebook.FacebookException;
//import com.facebook.login.LoginManager;
//import com.facebook.login.LoginResult;
//import java.util.Arrays;
//import in.eko.connectlib.BaseConnectActivity;
//public class FacebookSign {
//
//    private static final String TAG = "BaseConnectActivity";
//
//    private CallbackManager callbackManager;
//    private BaseConnectActivity mBaseConnectActivity;
//    private Context mContext;
//
//    public String fbToken;
//
//    private String FB_LOGIN_ACTION = "fb_login";
//    private String FACEBOOK_LOGIN_SUCCESS = "FACEBOOK_LOGIN_SUCCESS";
//    private String FACEBOOK_LOGIN_FAILED = "FACEBOOK_LOGIN_FAILED";
//
//    public FacebookSign(Context context, BaseConnectActivity activity, CallbackManager cbm) {
//        mContext = context;
//        mBaseConnectActivity = activity;
//        callbackManager = cbm;
//        fbToken = null;
//        setupFacebookLogin();
//    }
//
//    public void setupFacebookLogin() {
//        //callbackManager = CallbackManager.Factory.create();
//
//        LoginManager.getInstance().registerCallback(
//                callbackManager,
//                new FacebookCallback<LoginResult>() {
//                    @Override
//                    public void onSuccess(LoginResult loginResult) {
//                        // Handle success
//                        toast("Logged in with Facebook");
//                        mBaseConnectActivity.registerGoogleEvent("", FACEBOOK_LOGIN_SUCCESS);
//                        fbToken = loginResult.getAccessToken().getToken();
//
//                        Log.i(TAG, "Facebook Token Received: " + fbToken);
//                        mBaseConnectActivity.sendWebViewResponse(FB_LOGIN_ACTION, fbToken);
//                    }
//
//                    @Override
//                    public void onCancel() {
//                        toast("Facebook Login cancelled");
//                    }
//
//                    @Override
//                    public void onError(FacebookException e) {
//                        mBaseConnectActivity.registerGoogleEvent(e.getMessage(), FACEBOOK_LOGIN_FAILED);
//                        toast("Facebook Login error: " + e.getMessage());
//                    }
//
//                }
//        );
//    }
//
//    public void fbSignin() {
//        fbToken = null;
//         toast("Logging in with Facebook");
//        LoginManager.getInstance().logInWithReadPermissions(
//                mBaseConnectActivity,
//                Arrays.asList("email", "public_profile")
//        );
//    }
//
//    public void fbSignout() {
//        // toast("Logged out from Facebook");
//        LoginManager.getInstance().logOut();
//    }
//
//    public boolean isFbLoginActive() {
//        AccessToken accessToken = AccessToken.getCurrentAccessToken();
//        return accessToken != null && !accessToken.isExpired();
//    }
//
//    private void toast(String msg)
//    {
//        toast(msg, Toast.LENGTH_SHORT);
//    }
//
//    private void toast(String msg, int length)
//    {
//        Toast.makeText(mContext, msg, length).show();
//    }
//}