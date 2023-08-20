package in.eko.connectlib;

import android.content.Context;
import android.webkit.JavascriptInterface;
import android.widget.Toast;
// import android.widget.Toast;

class WebAppInterface {

	private Context mContext;


	/** Instantiate the interface and set the context */
	WebAppInterface(Context c) {
		mContext = c;
	}

	/* Show a toast from the web page *
	@JavascriptInterface
	public void showToast(String toast) {
		Toast.makeText(mContext, toast, Toast.LENGTH_SHORT).show();
	} */

	/** take an action request from the web page */
	@JavascriptInterface
	public void doAction(String action, String data) {
		 //Toast.makeText(mContext, "WEB ACTION: " + action /* + " (" + data + ")" */, Toast.LENGTH_SHORT).show();

		((BaseConnectActivity)mContext).doAction(action, data);
	}


//	@JavascriptInterface
//	public void getBase64FromBlobData(String base64Data) throws IOException {
//		// convertBase64StringToPdfAndStoreIt(base64Data);
//	}
}
