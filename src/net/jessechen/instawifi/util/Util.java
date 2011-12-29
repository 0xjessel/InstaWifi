package net.jessechen.instawifi.util;

import java.util.List;

import net.jessechen.instawifi.models.WifiModel;
import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;
import android.widget.Toast;

public class Util {
	public static String WIFI_URI_SCHEME = "wifi://%s/%s#%s";
	public static String TAG = "instawifi";
	public static String WEP = "wep";
	public static String WPA = "wpa";
	public static String OPEN = "open";

	public static Toast shortToast(Context c, String msg) {
		return Toast.makeText(c, msg, Toast.LENGTH_SHORT);
	}

	public static Toast longToast(Context c, String msg) {
		return Toast.makeText(c, msg, Toast.LENGTH_LONG);
	}

	public static WifiModel getCurrentWifiModel(Context c) {
		WifiConfiguration wc = getCurrentWifiConfig(c);
		if (wc != null) {
			String ssid = wc.SSID;
			String protocol = getWifiProtocol(wc);
			String password = getWifiPassword(wc, protocol);
			return new WifiModel(ssid, password, protocol);
		} else {
			return null;
		}
	}

	public static WifiModel getWifiModel(Context c, Uri wifiUri) {
		String ssid = "\"".concat(wifiUri.getHost()).concat("\"");
		String pw = wifiUri.getLastPathSegment();
		String protocol = wifiUri.getFragment();
		if (ssid == null) {
			Util.shortToast(c, "ERROR: SSID is null").show();
			return null;
		}
		return new WifiModel(ssid, pw, protocol);
	}

	public static WifiConfiguration getCurrentWifiConfig(Context c) {
		WifiManager mWifiManager = (WifiManager) c
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo currentWifiInfo = mWifiManager.getConnectionInfo();
		String curSSID = "\"".concat(currentWifiInfo.getSSID()).concat("\"");
		
		if (currentWifiInfo != null) {
			WifiConfiguration activeConfig = null;
			for (WifiConfiguration conn : mWifiManager.getConfiguredNetworks()) {
				if (conn.SSID.equals(curSSID)) {
					activeConfig = conn;
					break;
				}
			}
			if (activeConfig != null) {
				return activeConfig;
			}
		}
		return null;
	}

	public static String getWifiProtocol(WifiConfiguration wc) {
		if (wc.allowedAuthAlgorithms.isEmpty()) {
			// this is an open network
			return OPEN;
		} else if (wc.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
			// WEP network, the key is in wfc.wepKeys[wfc.wepTxKeyIndex]
			return WEP;
		} else if (wc.allowedKeyManagement
				.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
			// WPA/WPA2 network, key is in wfc.preSharedKey
			return WPA;
		} else {
			// not one of the above..
			Log.e(TAG, "Did not find wifi protocol");
			return null;
		}

	}

	public static String getWifiPassword(WifiConfiguration wc, String protocol) {
		if (protocol.equals(WEP)) {
			// TODO: i think this just returns '*'..needs root to get a password
			return wc.wepKeys[wc.wepTxKeyIndex];
		} else if (protocol.equals(WPA)) {
			return wc.preSharedKey;
		} else {
			return null;
		}
	}

	public static boolean connectToWifi(Context c, Uri wifiUri) {
		WifiManager mWm = (WifiManager) c
				.getSystemService(Context.WIFI_SERVICE);
		if (!mWm.isWifiEnabled()) { 
			mWm.setWifiEnabled(true);
			Log.i(Util.TAG, "wifi was disabled, enabling wifi");
		}
		
		// waiting until wifi is enabled
		while (!mWm.isWifiEnabled()) {
			// do nothing
			Log.v(TAG, "waiting for wifi to be enabled..");
		}
	
		int netId = getNetworkId(c, wifiUri, mWm);
		if (netId == -1) {
			netId = addWifiNetwork(c, wifiUri, mWm);
		}
		return connectToNetwork(netId, mWm);
	}

	public static boolean connectToNetwork(int netId, WifiManager mWm) {
		if (netId != -1 && mWm.isWifiEnabled()) {
			Log.i(Util.TAG, "attemping to connect to new network..");
			if (mWm.enableNetwork(netId, true)) {
				Log.i(Util.TAG, "succesfully connected to new network!");
				return true;
			} else {
				Log.e(Util.TAG, "failed to connect to new network");
			}
		}
		return false;
	}

	public static int addWifiNetwork(Context c, Uri wifiUri, WifiManager mWm) {
		WifiModel wm = getWifiModel(c, wifiUri);
		String protocol = wm.getProtocol();
		String pw = wm.getPassword();

		WifiConfiguration mWc = new WifiConfiguration();
		mWc.SSID = wm.getSSID();
		mWc.status = WifiConfiguration.Status.DISABLED;

		// thanks to:
		// http://kmansoft.com/2010/04/08/adding-wifi-networks-to-known-list/
		if (protocol.equals(OPEN)) {
			// TODO: something like this, need to check open network configs

			mWc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			mWc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			mWc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			mWc.allowedAuthAlgorithms.clear();
			mWc.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.CCMP);
			mWc.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		} else if (protocol.equals(WEP)) {
			// WEP network configs

			mWc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			mWc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			mWc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			mWc.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
			mWc.allowedAuthAlgorithms
					.set(WifiConfiguration.AuthAlgorithm.SHARED);
			mWc.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.CCMP);
			mWc.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);

			if (isHexString(pw)) {
				mWc.wepKeys[0] = pw;
			} else {
				mWc.wepKeys[0] = "\"".concat(pw).concat("\"");
			}
			mWc.wepTxKeyIndex = 0;
		} else if (protocol.equals(WPA)) {
			// WPA network configs

			mWc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			mWc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			mWc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			mWc.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.CCMP);
			mWc.allowedPairwiseCiphers
					.set(WifiConfiguration.PairwiseCipher.TKIP);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

			mWc.preSharedKey = "\"".concat(pw).concat("\"");
		}

		// add network to known list
		int netId = mWm.addNetwork(mWc);
		if (netId == -1) {
			Log.e(Util.TAG,
					"netId == -1, failed to add new network to known networks");
		}
		if (!mWm.saveConfiguration()) {
			Log.e(Util.TAG, "failed to save wifi configuration");
		}
		return getNetworkId(c, wifiUri, mWm);
	}

	public static int getNetworkId(Context c, Uri wifiUri, WifiManager mWm) {
		List<WifiConfiguration> configuredNetworks = mWm
				.getConfiguredNetworks();

		WifiModel wm = getWifiModel(c, wifiUri);
		String mSSID = wm.getSSID();

		for (WifiConfiguration wifiConfig : configuredNetworks) {
			if (wifiConfig.SSID.equals(mSSID)) {
				return wifiConfig.networkId;
			}
		}
		return -1;
	}

	public static boolean isHexString(String s) {
		if (s == null) {
			return false;
		}
		int len = s.length();
		if (len != 10 && len != 26 && len != 58) {
			return false;
		}
		for (int i = 0; i < len; ++i) {
			char c = s.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
					|| (c >= 'A' && c <= 'F')) {
				continue;
			}
			return false;
		}
		return true;
	}
}
