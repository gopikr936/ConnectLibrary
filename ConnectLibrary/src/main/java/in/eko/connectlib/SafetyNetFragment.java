package in.eko.connectlib;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.safetynet.SafetyNet;
import com.google.android.gms.safetynet.SafetyNetApi;
import com.google.android.gms.safetynet.SafetyNetClient;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;

import java.security.SecureRandom;
import java.util.Random;

import in.eko.connectlib.constants.Params;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link SafetyNetFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

// Safety Net Fragment
public class SafetyNetFragment extends Fragment {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER

    private static final String TAG = "SafetyNetFragment";

    private static final String BUNDLE_RESULT = "result";
    private static final String NETWORK_ERROR = "NETWORK_ERROR";

    private final Random mRandom = new SecureRandom();

    private String mResult;
    private String mPendingResult;
    private String nonceString;
    private String attestationKey;

    private int safetyNetCounter = 0;

    private View mView;
    protected BaseConnectActivity mActivity;
    private Context context;

    private onGoogleEventListener googleEventListener;

    public SafetyNetFragment() {
        Log.d(TAG, "inside the safety net constructor");
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SafetyNetFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static SafetyNetFragment newInstance(String param1, String param2) {
        SafetyNetFragment fragment = new SafetyNetFragment();
        Bundle args = new Bundle();
        Log.d(TAG, "inside the safety net fragment");
        fragment.setArguments(args);
        return fragment;
    }


    public interface onSafetyResponseListener {
        // TODO: Change name sendDataToActivity that it is clear on BaseConnectActivity implementation
        // sendSafetynetSignatureToWebview

        void sendDataToActivity(String signature, Boolean flag);
    }


    onSafetyResponseListener safetyResponseListener;
    public interface onGoogleEventListener {
        void registerGoogleEvent(String code, String event);
    }


    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            this.context = context;
            Log.d(TAG, "Inside the attach ");
            safetyResponseListener = (onSafetyResponseListener) activity;
            googleEventListener = (onGoogleEventListener) activity;

        } catch (ClassCastException e) {
            Log.d(TAG, "Inside the attach exception ");
            throw new ClassCastException(activity.toString() + " must implement onSafetyResponseListener");
        }
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        this.nonceString = bundle.getString(Params.NONCE);
        this.attestationKey = bundle.getString(Params.ATTESTATION_KEY);

        Log.d(TAG, "Ending onCreate Bundle");
    }

    public void onResume(){
        super.onResume();
        if (mPendingResult != null) {
            // Restore the previous result once the Activity has fully resumed and the logging
            // framework has been set up.
            mResult = mPendingResult;
            mPendingResult = null;
            Log.d(TAG, "SafetyNet result:\n" + mResult + "\n");
        }
    }
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putString(BUNDLE_RESULT, mResult);
    }

        // male safety net call here
    public void sendSafetyNetRequest(View view, String nonceString) {

        Log.i(TAG, "Sending SafetyNet API request.");

        this.nonceString = nonceString;

        // showSnackBar("Checking device safety", Snackbar.LENGTH_INDEFINITE);

         /*
        Create a nonce for this request.
        The nonce is returned as part of the response from the
        SafetyNet API. Here we append the string to a number of random bytes to ensure it larger
        than the minimum 16 bytes required.
        Read out this value and verify it against the original request to ensure the
        response is correct and genuine.
        NOTE: A nonce must only be used once and a different nonce should be used for each request.
        As a more secure option, you can obtain a nonce from your own server using a secure
        connection. Here in this sample, we generate a String and append random bytes, which is not
        very secure. Follow the tips on the Security Tips page for more information:
        https://developer.android.com/training/articles/security-tips.html#Crypto
         */
        // TODO(developer): Change the nonce generation to include your own, used once value,
        // ideally from your remote server.
//        String nonceData = "Safety Net: " + System.currentTimeMillis();


        /*
         Call the SafetyNet API asynchronously.
         The result is returned through the success or failure listeners.
         First, get a SafetyNetClient for the foreground Activity.
         Next, make the call to the attestation API. The API key is specified in the gradle build
         configuration and read from the gradle.properties file.
         */

        try {
            safetyNetCounter++;
            Log.d(TAG, "Just about to send the safety net request " + getActivity());

            SafetyNetClient client = SafetyNet.getClient(getActivity());

            // Log.d(TAG, "Nonce String:\n" + nonceString);
            // Log.d(TAG, "Nonce Bytes:\n" + nonceString.getBytes());

            Task<SafetyNetApi.AttestationResponse> task = client.attest(nonceString.getBytes() , BuildConfig.attestation_key);    // getString(R.string.attestation_key)
            task.addOnSuccessListener(getActivity(), mSuccessListener)
                    .addOnFailureListener(getActivity(), mFailureListener);
        } catch (Exception e) {
            Log.d(TAG, "Error in sending request " + e );
        }

    }


    private OnSuccessListener<SafetyNetApi.AttestationResponse> mSuccessListener =
            new OnSuccessListener<SafetyNetApi.AttestationResponse>() {
                @Override
                public void onSuccess(SafetyNetApi.AttestationResponse attestationResponse) {
                    /*
                     Successfully communicated with SafetyNet API.
                     Use result.getJwsResult() to get the signed result data. See the server
                     component of this sample for details on how to verify and parse this result.
                     */

                    Log.d(TAG, "Device Verified: Safety Net Request Success");

                    // showSnackBar("Device Verified.", Snackbar.LENGTH_LONG);

                    mResult = attestationResponse.getJwsResult();
                    googleEventListener.registerGoogleEvent("","SAFETYNET_SUCCESS");

                    safetyResponseListener.sendDataToActivity(mResult, true);

                        /*
                         TODO(developer): Forward this result to your server together with
                         the nonce for verification.
                         You can also parse the JwsResult locally to confirm that the API
                         returned a response by checking for an 'error' field first and before
                         retrying the request with an exponential backoff.
                         NOTE: Do NOT rely on a local, client-side only check for security, you
                         must verify the response on a remote server!
                        */
                }
            };


    private OnFailureListener mFailureListener = new OnFailureListener() {


        @Override
        public void onFailure(@NonNull Exception e) {
            // An error occurred while communicating with the service.
            // try 3 times before actually failing safety net
            mResult = null;
            Log.d(TAG, "Device verification failed: Safetynet Failure");


            if (e instanceof ApiException) {
                // An error with the Google Play Services API contains some additional details.
                ApiException apiException = (ApiException) e;
                Log.d(TAG, "Error: " +
                        CommonStatusCodes.getStatusCodeString(apiException.getStatusCode()) + ": " +
                        apiException.getStatusMessage());

                googleEventListener.registerGoogleEvent(CommonStatusCodes.getStatusCodeString(apiException.getStatusCode()),"SAFETYNET_SUCCESS");

                if (CommonStatusCodes.getStatusCodeString((apiException.getStatusCode())) == NETWORK_ERROR && safetyNetCounter < 3) {
                    sendSafetyNetRequest(mView, nonceString);
                } else {
                    safetyNetCounter = 0;
                    safetyResponseListener.sendDataToActivity(CommonStatusCodes.getStatusCodeString((apiException.getStatusCode())),  false);
                }

            } else {
                // A different, unknown type of error occurred.
                Log.d(TAG, "ERROR! " + e.getMessage());

                // control passes back to the connect activity to send data to web app
                safetyResponseListener.sendDataToActivity("Safety Net Request Failure",  false);
            }


            // showSnackBar("Device verification failed.", Snackbar.LENGTH_LONG);

        }
    };


    public void showSnackBar(String message, int length) {
        Snackbar snackbar = Snackbar.make(mView, message, length);
        Snackbar.SnackbarLayout layout = (Snackbar.SnackbarLayout)snackbar.getView();
        layout.setMinimumHeight(120);
        snackbar.show();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        this.mView = inflater.inflate(R.layout.fragment_safety_net, container, false);
        return mView;
    }
}