package net.jessechen.instawifi.asynctask;

import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

/**
 * Try and fetch the wifi protocol and password given an SSID (either from root
 * means or from shared preferences) and then set the protocol and password
 * inputs with the data we fetched.
 */
public class SetWifiProtocolAndPasswordInputFields extends
		AsyncTask<Context, Void, WifiModel> {
	private static final String TAG = SetWifiProtocolAndPasswordInputFields.class
			.getSimpleName();

	private String SSID;
	private Spinner protocolSpinner;
	private EditText passwordField;
	private TextView passwordText;
	private CheckBox revealPassword;

	public SetWifiProtocolAndPasswordInputFields(Spinner protocolSpinner,
			TextView passwordText, EditText passwordField,
			CheckBox revealPassword, String SSID) {
		this.protocolSpinner = protocolSpinner;
		this.passwordField = passwordField;
		this.passwordText = passwordText;
		this.revealPassword = revealPassword;
		this.SSID = Util.concatQuotes(SSID);
	}

	@Override
	protected WifiModel doInBackground(Context... params) {
		Context c = params[0];
		WifiConfiguration wc = WifiUtil.getWifiConfig(c, SSID);
		int protocol = WifiUtil.getWifiProtocol(wc);
		protocol = (protocol == -1) ? 1 : protocol;
		String pw = WifiUtil.fetchWifiPasswordNow(c, SSID);
		if (SSID == null) {
			Log.e(TAG, "SSID is null when getting wifi model");
			Util.shortToast(c, "ERROR: SSID is null");
			return null;
		}

		return new WifiModel(SSID, pw, protocol);
	}

	@Override
	protected void onPostExecute(WifiModel selectedNetwork) {
		if (selectedNetwork != null) {
			passwordText.setVisibility(View.VISIBLE);
			passwordField.setVisibility(View.VISIBLE);
			revealPassword.setVisibility(View.VISIBLE);

			protocolSpinner.setSelection(selectedNetwork.getProtocol());

			passwordField.setText(Util.stripQuotes(selectedNetwork
					.getPassword()));
		}
	}
}
