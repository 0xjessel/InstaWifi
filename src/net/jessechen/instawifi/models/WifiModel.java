package net.jessechen.instawifi.models;

import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.net.Uri;
import android.util.Log;

public class WifiModel {
	private String SSID;
	private String password;
	private String protocol;

	private static final String TAG = WifiModel.class.getName();

	// ssid needs quotes
	public WifiModel(String ssid, String pw, String pt) {
		protocol = pt;
		if (Util.hasQuotes(ssid)) {
			SSID = ssid;
		} else {
			SSID = Util.concatQuotes(ssid);
		}
		if (!protocol.equals(WifiUtil.OPEN)) {
			password = pw;
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
		this.protocol = wifiUri.getFragment();
	}

	public String getProtocol() {
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
		return String
				.format(WifiUtil.WIFI_URI_SCHEME, SSID, password, protocol);
	}
}
