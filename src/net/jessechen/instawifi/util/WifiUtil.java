package net.jessechen.instawifi.util;

import java.util.List;

import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.RootUtil.PasswordNotFoundException;
import android.content.Context;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiUtil {
	public static String WIFI_URI_SCHEME = "wifi://%s/%s#%s";

	public static String WEP = "wep";
	public static String WPA = "wpa";
	public static String OPEN = "open";
	
	private static final String TAG = WifiUtil.class.getName();
	
	public static WifiModel getCurrentWifiModel(Context c) {
		WifiConfiguration wc = getCurrentWifiConfig(c);
		if (wc != null) {
			String ssid = wc.SSID;
			String protocol = getWifiProtocol(wc);
			String password = null;
			try {
				password = RootUtil.getWifiPassword(c, wc);
			} catch (PasswordNotFoundException e) {
				Log.e(TAG,
						"password not found when trying to get it using root access");
				e.printStackTrace();
			} // TODO: FIX password can be null
			return new WifiModel(ssid, password, protocol);
		} else {
			return null;
		}
	}

	public static WifiModel getWifiModel(Context c, Uri wifiUri) {
		String ssid = wifiUri.getHost();
		String pw = wifiUri.getLastPathSegment();
		String protocol = wifiUri.getFragment();
		if (ssid == null) {
			Log.e(TAG, "SSID is null when getting wifi model");
			Util.shortToast(c, "ERROR: SSID is null");
			return null;
		}
		return new WifiModel(ssid, pw, protocol);
	}

	public static WifiConfiguration getCurrentWifiConfig(Context c) {
		WifiManager mWifiManager = (WifiManager) c
				.getSystemService(Context.WIFI_SERVICE);
		WifiInfo currentWifiInfo = mWifiManager.getConnectionInfo();

		if (currentWifiInfo.getSSID() != null
				&& currentWifiInfo.getNetworkId() != -1) {
			String curSSID = Util.concatQuotes(currentWifiInfo.getSSID());

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

	/**
	 * validate incoming URI to ensure that it matches this app's URI schema
	 * 
	 * @param wifiUri
	 * @return true if valid wifi URI, false otherwise
	 */
	public static boolean isValidWifiUri(Uri wifiUri) {
		if (wifiUri.getScheme().equals("wifi") && wifiUri.getHost() != null
				&& wifiUri.getLastPathSegment() != null
				&& wifiUri.getFragment() != null
				&& wifiUri.getPathSegments().size() == 1
				&& wifiUri.getPort() == -1
				&& wifiUri.getQueryParameterNames().size() == 0
				&& wifiUri.getUserInfo() == null) {
			return true;
		}
		return false;
	}

	/**
	 * should call this method before executing an action on the WifiModel
	 * 
	 * @param wm
	 * @return true if valid, false otherwise
	 */
	public static boolean isValidWifiModel(WifiModel wm) {
		if (wm == null) {
			return false;
		} else if (wm.getPassword() == null && wm.getProtocol() == null
				&& wm.getSSID() == null) {
			return false;
		} else {
			return true;
		}
	}

	public static ConnectToWifiResult connectToWifi(Context c, WifiModel mWifiModel) {
		WifiManager mWm = (WifiManager) c
				.getSystemService(Context.WIFI_SERVICE);
		if (!mWm.isWifiEnabled()) {
			mWm.setWifiEnabled(true);
			Log.i(TAG, "wifi was disabled, enabling wifi");
		}

		// waiting until wifi is enabled
		while (!mWm.isWifiEnabled()) {
			// do nothing, this can be bad
			Log.v(TAG, "waiting for wifi to be enabled..");
		}

		int netId = getNetworkId(c, mWifiModel, mWm);
		if (netId == -1) {
			netId = addWifiNetwork(c, mWifiModel, mWm);
		} else if (netId == mWm.getConnectionInfo().getNetworkId()) {
			Log.i(TAG,
					String.format("already connected to %s",
							mWifiModel.getSSID()));
			return ConnectToWifiResult.ALREADY_CONNECTED;
		}
		return connectToNetwork(netId, mWm);
	}

	public static ConnectToWifiResult connectToNetwork(int netId, WifiManager mWm) {
		if (netId == -1) {
			return ConnectToWifiResult.INVALID_NET_ID;
		}

		if (!mWm.isWifiEnabled()) {
			mWm.setWifiEnabled(true);

			// waiting until wifi is enabled
			while (!mWm.isWifiEnabled()) {
				// do nothing, this can be bad
				Log.v(TAG, "waiting for wifi to be enabled..");
			}
		}

		if (mWm.enableNetwork(netId, true)) {
			Log.i(TAG, "attemping to connect to network..");
			return ConnectToWifiResult.NETWORK_ENABLED;
		} else {
			Log.e(TAG, "failed attempt to connect to network");
			return ConnectToWifiResult.NETWORK_ENABLED_FAILED;
		}
	}

	public static int addWifiNetwork(Context c, WifiModel mWifiModel,
			WifiManager mWm) {
		if (mWifiModel == null) {
			return -1;
		}

		String protocol = mWifiModel.getProtocol();
		String pw = mWifiModel.getPassword();

		WifiConfiguration mWc = new WifiConfiguration();
		mWc.SSID = mWifiModel.getSSID();
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

			if (Util.isHexString(pw)) {
				mWc.wepKeys[0] = pw;
			} else {
				mWc.wepKeys[0] = Util.concatQuotes(pw);
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

			mWc.preSharedKey = Util.concatQuotes(pw);
		}

		// add network to known list
		int netId = mWm.addNetwork(mWc);
		if (netId == -1) {
			Log.e(TAG, String.format(
					"netId == -1, failed to add %s to known networks",
					mWifiModel.getSSID()));
		}
		if (!mWm.saveConfiguration()) {
			Log.e(TAG, String.format(
					"failed to save wifi configuration for %s",
					mWifiModel.getSSID()));
			return -1;
		}
		return getNetworkId(c, mWifiModel, mWm);
	}

	public static int getNetworkId(Context c, WifiModel mWifiModel,
			WifiManager mWm) {
		List<WifiConfiguration> configuredNetworks = mWm
				.getConfiguredNetworks();

		String mSSID = mWifiModel.getSSID();

		for (WifiConfiguration wifiConfig : configuredNetworks) {
			if (wifiConfig.SSID.equals(mSSID)) {
				return wifiConfig.networkId;
			}
		}
		return -1;
	}
}
