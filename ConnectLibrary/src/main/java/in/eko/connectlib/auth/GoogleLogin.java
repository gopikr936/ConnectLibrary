package in.eko.connectlib.auth;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.util.concurrent.Executor;

import in.eko.connectlib.BaseConnectActivity;
import in.eko.connectlib.constants.Params;

public class GoogleLogin implements Executor {

    private static final String TAG = "GoogleLogin";
    
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private String GOOGLE_LOGIN_SUCCESS = "GOOGLE_LOGIN_SUCCESS";
    private String GOOGLE_LOGIN_FAILED = "GOOGLE_LOGIN_FAILED";
    private String GOOGLE_LOGIN_ERROR = "GOOGLE_LOGIN_ERROR";
    
    private GoogleSignInClient mGoogleSignInClient;
    public GoogleApiClient mGoogleApiClient;

    private String googleToken;

    BaseConnectActivity mBaseConnectActivity;
    Context mContext;



    // Google login class
    // Handle the google login functionality
    public GoogleLogin(Context context, BaseConnectActivity activity,String googleLoginClientId) {
        context.getResources();
        mContext = context;
        mBaseConnectActivity = activity;

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(googleLoginClientId)
                .requestEmail()
                .build();

        // Build a GoogleSignInClient with the options specified by gso.
        mGoogleSignInClient = GoogleSignIn.getClient(mBaseConnectActivity, gso);
    }


    public String getGoogleToken() {
        return googleToken;
    }
    public GoogleApiClient getGoogleApiClient() {
        return mGoogleApiClient;
    }


    /**
     * Handles the Google sign in result and returns the Google Token if success
     * @param data Intent
     */
    public void handleGoogleSignInResult(Intent data)
    {
        try
        {
            // The Task returned from this call is always completed, no need to attach
            // a listener.
            Task<GoogleSignInAccount> completedTask = GoogleSignIn.getSignedInAccountFromIntent(data);

            GoogleSignInAccount account = completedTask.getResult(ApiException.class);

            // Signed in successfully
            mBaseConnectActivity.registerGoogleEvent(GOOGLE_LOGIN_SUCCESS, GOOGLE_LOGIN_SUCCESS);
            toast("Logged in with Google as: " + account.getDisplayName());

            onGoogleLoginSuccess(account);

        }
        catch (ApiException e)
        {
            // The ApiException status code indicates the detailed failure reason.
            // Please refer to the GoogleSignInStatusCodes class reference for more information.
            mBaseConnectActivity.registerGoogleEvent(e.getMessage(), GOOGLE_LOGIN_FAILED);
            toast(GOOGLE_LOGIN_ERROR + ":" + e.getMessage());

        }
    }


    public void checkActiveGoogleLogin(BaseConnectActivity activity)
    {
//         new GoogleSilentSigninTask().execute(mGoogleSignInClient);

        mGoogleSignInClient.silentSignIn().addOnCompleteListener(activity, new OnCompleteListener<GoogleSignInAccount>() {
            @Override
            public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                onActiveGoogleLoginResult(task);
            }
        });
    }


    private void onActiveGoogleLoginResult(Task<GoogleSignInAccount> task)
    {
        if (task != null && task.isSuccessful()) {
            // toast("Google signed in account found");
            GoogleSignInAccount signInAccount = task.getResult();
            onGoogleLoginSuccess(signInAccount);
        }
        else
        {
            // toast("Google signed in account not found");    //  + task.getException().getMessage()
            Log.w(TAG, "Cached Google sign-in account not found");
        }
    }

    public void onGoogleLoginSuccess(GoogleSignInAccount account)
    {

        Log.d(TAG, account.getId());
        Log.d(TAG, account.getIdToken());

        googleToken = account.getIdToken();
        mBaseConnectActivity.sendWebViewResponse(Params.GOOGLE_LOGIN_ACTION, googleToken);
    }


    public void googleSignin()
    {
        googleToken = null;

        toast("Logging in with Google");
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        mBaseConnectActivity.startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    public void googleSignout()
    {
        // toast("Logged out from Google");
        mGoogleSignInClient.signOut();

        /* .addOnCompleteListener(this, new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                // Signout complete

            }
        }); */
    }
    private void toast(String msg)
    {
        toast(msg, Toast.LENGTH_SHORT);
    }

    private void toast(String msg, int length)
    {
        Toast.makeText(mContext, msg, length).show();
    }

    @Override
    public void execute(Runnable runnable) {

    }
}
