package net.jessechen.instawifi.util;

import java.util.ArrayList;
import java.util.List;

import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.RootUtil.PasswordNotFoundException;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

public class WifiUtil {
	public static String WIFI_URI_SCHEME = "wifi://%s/%s#%s";
	public static String QR_WIFI_URI_SCHEME = "WIFI:T:%s;S:%s;P:%s;;";
	public static String WEP = "WEP";
	public static String WPA = "WPA";
	public static String OPEN = "OPEN";
	public static String NOPASS = "nopass";

	@SuppressWarnings("serial")
	public static ArrayList<String> protocols = new ArrayList<String>() {
		{
			add(WEP);
			add(WPA);
			add(OPEN);
		}
	};
	
	public enum ConnectToWifiResult {
		ALREADY_CONNECTED, INVALID_NET_ID, NETWORK_ENABLED, NETWORK_ENABLED_FAILED
	}

	public enum QrImageSize {
		SMALL, LARGE
	}
	
	private static final String TAG = WifiUtil.class.getSimpleName();

	public static Bitmap generateQrImage(String ssid, String protocol,
			String password, QrImageSize size) {
		final int MAGIC_NUMBER = (size.equals(QrImageSize.SMALL)) ? 30 : 60;
        final int DIMENSION = (size.equals(QrImageSize.SMALL)) ? 350 : 700;
		
		QRCodeWriter writer = new QRCodeWriter();
		BitMatrix bm = null;
		try {
			// a little hack for open network configurations s.t. barcode
			// scanner is happy
			if (protocol.equals(OPEN)) {
				protocol = NOPASS;
			}

			bm = writer
					.encode(String.format(QR_WIFI_URI_SCHEME, protocol, ssid,
							password), BarcodeFormat.QR_CODE, DIMENSION, DIMENSION);
		} catch (WriterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		int width = bm.getWidth() - (MAGIC_NUMBER * 2);
		int height = bm.getHeight() - (MAGIC_NUMBER * 2);
		int[] pixels = new int[width * height];
		for (int y = MAGIC_NUMBER; y < bm.getHeight() - MAGIC_NUMBER; y++) {
			int offset = (y - MAGIC_NUMBER) * width;
			for (int x = MAGIC_NUMBER; x < bm.getWidth() - MAGIC_NUMBER; x++) {
				pixels[offset + (x - MAGIC_NUMBER)] = bm.get(x, y) ? Color.BLACK : Color.WHITE;
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(width, height,
				Bitmap.Config.ARGB_8888);
		bitmap.setPixels(pixels, 0, width, 0, 0, width, height);

		return bitmap;
	}

	public static WifiModel getCurrentWifiModel(Context c) {
		WifiConfiguration wc = getCurrentWifiConfig(c);
		if (wc != null) {
			String ssid = wc.SSID;
			String protocol = getWifiProtocol(wc);
			String password = null;
			try {
				password = RootUtil.getWifiPassword(c, ssid);
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

	public static WifiModel getWifiModelFromUri(Context c, Uri wifiUri) {
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

	public static WifiModel getWifiModelFromSsid(Context c, String SSID)
			throws PasswordNotFoundException {
		SSID = Util.concatQuotes(SSID);
		WifiConfiguration wc = getWifiConfig(c, SSID);
		String protocol = getWifiProtocol(wc);
		String pw = RootUtil.getWifiPassword(c, SSID);
		if (SSID == null) {
			Log.e(TAG, "SSID is null when getting wifi model");
			Util.shortToast(c, "ERROR: SSID is null");
			return null;
		}
		return new WifiModel(SSID, pw, protocol);
	}

	public static WifiConfiguration getWifiConfig(Context c, String SSID) {
		WifiManager mWifiManager = (WifiManager) c
				.getSystemService(Context.WIFI_SERVICE);

		WifiConfiguration toReturn = null;
		for (WifiConfiguration conn : mWifiManager.getConfiguredNetworks()) {
			if (conn.SSID.equals(SSID)) {
				toReturn = conn;
				break;
			}
		}

		if (toReturn != null) {
			return toReturn;
		}

		return null;
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
		if (wc.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
			// WPA/WPA2 network, key is in wfc.preSharedKey
			return WPA;
		} else if (wc.allowedAuthAlgorithms.isEmpty()) {
			// this is an open network
			return OPEN;
		} else if (wc.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
			// WEP network, the key is in wfc.wepKeys[wfc.wepTxKeyIndex]
			return WEP;
		} else {
			// not one of the above..
			Log.e(TAG, "Did not find wifi protocol");
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

	public static ConnectToWifiResult connectToWifi(Context c,
			WifiModel mWifiModel) {
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

	public static ConnectToWifiResult connectToNetwork(int netId,
			WifiManager mWm) {
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

	public static String[] getConfiguredNetworks(Context c) {
		WifiManager mWm = (WifiManager) c
				.getSystemService(Context.WIFI_SERVICE);

		List<WifiConfiguration> configuredNetworks = mWm
				.getConfiguredNetworks();

		String[] toReturn = new String[configuredNetworks.size()];

		for (int i = 0; i < configuredNetworks.size(); i++) {
			toReturn[i] = Util.stripQuotes(configuredNetworks.get(i).SSID);
		}

		return toReturn;
	}
}
