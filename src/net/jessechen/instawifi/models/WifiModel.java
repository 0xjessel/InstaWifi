package net.jessechen.instawifi.models;

import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.net.Uri;
import android.util.Log;

/**
 * WifiModel has 3 internal data fields.
 * 
 * SSID is the name of the network, which must have enclosing quotes around the
 * ssid. The constructor will take care of adding quotes if it doesn't have it.
 * 
 * protocol is the type of security/encryption used for the network. WifiUtil
 * has 3 integer constants that should be consistently used across the app.
 * 
 * password should just be text not enclosed in quotes
 * 
 * @author Jesse Chen
 * 
 */
public class WifiModel {
	private String SSID; // must be in quotes
	private String password; // when network has no pw, password should be ""
	private int protocol; // WifiUtil constants represented w/ int

	private static final String TAG = WifiModel.class.getName();

	// ssid needs quotes
	public WifiModel(String ssid, String pw, int protocol) {
		this.protocol = protocol;
		if (Util.hasQuotes(ssid)) {
			SSID = ssid;
		} else {
			SSID = Util.concatQuotes(ssid);
		}
		if (!(protocol == WifiUtil.OPEN)) {
			password = pw;
		} else {
			password = "";
		}
	}

	public WifiModel(String wifiString) {
		Uri wifiUri = Uri.parse(wifiString);
		if (!WifiUtil.isValidWifiUri(wifiUri)) {
			Log.e(TAG, String.format(
					"invalid URI when creating new WifiModel: %s",
					wifiUri.toString()));
		}

		this.SSID = wifiUri.getHost();
		this.password = wifiUri.getLastPathSegment();
		this.protocol = Integer.parseInt(wifiUri.getFragment());
	}

	public int getProtocol() {
		return protocol;
	}

	public String getSSID() {
		return SSID;
	}

	public String getTrimmedSSID() {
		if (Util.hasQuotes(SSID)) {
			return SSID.substring(1, SSID.length() - 1);
		} else {
			return SSID;
		}
	}

	public String getPassword() {
		return password;
	}

	public String toWifiUri() {
		return String.format(WifiUtil.WIFI_URI_SCHEME, SSID, password,
				protocol);
	}
}
