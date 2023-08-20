package in.eko.connectlib.auth.biometric;

public interface BiometricAuthListener {

    void biometricSuccessCallback();
    void biometricFailureCallback();
    void biometricAdviceCallback(String advice);
    void biometricEnableCallback();
    void biometricErrorCallback(String error);
}
