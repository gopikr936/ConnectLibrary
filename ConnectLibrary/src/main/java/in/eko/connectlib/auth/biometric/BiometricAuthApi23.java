package in.eko.connectlib.auth.biometric;

import static android.content.Context.FINGERPRINT_SERVICE;
import static android.content.Context.KEYGUARD_SERVICE;

import android.Manifest;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;


public class BiometricAuthApi23 implements Biometric {

    private final String TAG = "BiometricAuthApi23";
    private final String KEY_NAME = "AndroidFingerprint";

    private Context context;

    private KeyStore mKeyStore;
    private Cipher mCipher;
    private SecretKey secretKeyObject;

    public BiometricAuthApi23(Context context){
        this.context = context;
    }

    private void notifyUser(String message) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    // generate key store and initialize key generator to create a key storage in the Android Storage to store the encrypted key
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected void generateKey() {
        try {
            mKeyStore = KeyStore.getInstance("AndroidKeyStore");
            KeyGenerator mKeyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore");
            mKeyStore.load(null);
            mKeyGenerator.init(new
                    KeyGenParameterSpec.Builder(KEY_NAME,
                    KeyProperties.PURPOSE_ENCRYPT |
                            KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
                    .setUserAuthenticationRequired(true)
                    .setEncryptionPaddings(
                            KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build());
            secretKeyObject = mKeyGenerator.generateKey();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // init cipher
    @RequiresApi(api = Build.VERSION_CODES.M)
    protected boolean initCipher () {
        try {
            mCipher = Cipher.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7
            );
            mKeyStore.load(null);
            Log.d(TAG, "Fingerprint authentication started -1");
//            SecretKey key = (SecretKey) mKeyStore.getKey(KEY_NAME, null);
            Log.d(TAG, "Fingerprint authentication started 0 ");
            mCipher.init(Cipher.ENCRYPT_MODE, secretKeyObject);
            Log.d(TAG, "Fingerprint authentication started");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public int checkBiometricSupport() {
        KeyguardManager keyMgr = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
        PackageManager pkgMgr = context.getPackageManager();

        if (!keyMgr.isKeyguardSecure()) {
            notifyUser("Lock screen security not enabled in Settings");
            return BiometricFactory.NO_SUPPORT_HARD_FAIL;
        }

        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.USE_BIOMETRIC) !=
                PackageManager.PERMISSION_GRANTED) {

            notifyUser("Fingerprint authentication permission not enabled");
            return BiometricFactory.NO_SUPPORT_SOFT_FAIL;
        }

        if (pkgMgr.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)) {
            return BiometricFactory.HAS_SUPPORT;
        }
        return BiometricFactory.NO_SUPPORT_SOFT_FAIL;
    }

    @Override
    public void showBiometricToEnable(BiometricAuthListener biometricAuthListener) {

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void showBiometricForAuth(BiometricAuthListener biometricAuthListener) {
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(KEYGUARD_SERVICE);
        FingerprintManager mFingerprintManager = (FingerprintManager) context.getSystemService(FINGERPRINT_SERVICE);

        if (!mKeyguardManager.isKeyguardSecure()) {
            Toast.makeText(context, "Lock screen security not enabled in Settings", Toast.LENGTH_LONG).show();
        } else if (!mFingerprintManager.hasEnrolledFingerprints()) {
            Toast.makeText(context, "Register at least one fingerprint in Settings", Toast.LENGTH_LONG).show();
        } else {
            generateKey();
            if (initCipher()) {
                FingerprintManager.CryptoObject mCryptoObject = new FingerprintManager.CryptoObject(mCipher);
                FingerprintHandler helper = new FingerprintHandler(context);
                helper.startAuth(mFingerprintManager, mCryptoObject);
            }
        }
    }
}

@RequiresApi(api = Build.VERSION_CODES.M)
class FingerprintHandler extends
        FingerprintManager.AuthenticationCallback {


    private final String TAG = "FingerprintHandler";

    private CancellationSignal cancellationSignal;
    private Context appContext;



    public FingerprintHandler(Context context) {
        appContext = context;
    }


    public void startAuth(FingerprintManager manager,
                          FingerprintManager.CryptoObject cryptoObject) {

        cancellationSignal = new CancellationSignal();

        if (ActivityCompat.checkSelfPermission(appContext,
                Manifest.permission.USE_FINGERPRINT) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }
        manager.authenticate(cryptoObject, cancellationSignal, 0, this, null);
    }

    @Override
    public void onAuthenticationError(int errMsgId,
                                      CharSequence errString) {
        Toast.makeText(appContext,
                "Authentication error\n" + errString,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAuthenticationHelp(int helpMsgId,
                                     CharSequence helpString) {
        Toast.makeText(appContext,
                "Authentication help\n" + helpString,
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAuthenticationFailed() {
        Log.d(TAG, "Fingerprint authentication failed");
        Toast.makeText(appContext,
                "Authentication failed.",
                Toast.LENGTH_LONG).show();
    }

    @Override
    public void onAuthenticationSucceeded(
            FingerprintManager.AuthenticationResult result) {
        Log.d(TAG, "Fingerprint authentication succeeded");
        Toast.makeText(appContext,
                "Authentication succeeded.",
                Toast.LENGTH_LONG).show();
    }
}