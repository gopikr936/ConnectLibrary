package in.eko.connectlib.auth.biometric;

public interface Biometric {

    int checkBiometricSupport();
    void showBiometricToEnable(BiometricAuthListener biometricAuthListener);
    void showBiometricForAuth(BiometricAuthListener biometricAuthListener);

}
