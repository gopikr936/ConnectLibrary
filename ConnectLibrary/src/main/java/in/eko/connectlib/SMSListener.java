package in.eko.connectlib;

public interface SMSListener {

    void onSuccess(String message);
    void onError(String message);
}
