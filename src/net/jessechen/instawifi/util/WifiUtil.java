package net.jessechen.instawifi.util;

import java.util.List;

import net.jessechen.instawifi.R;
import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.RootUtil.PasswordNotFoundException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

import com.bugsense.trace.BugSenseHandler;

public class WifiUtil {
	public static String WIFI_URI_SCHEME = "wifi://%s/%s#%s";
	public static String QR_WIFI_URI_SCHEME = "WIFI:T:%s;S:%s;P:%s;;";
	public static final int NONE = 0;
	public static final int WEP = 1;
	public static final int WPA = 2;
	public static final int DEFAULT_PROTOCOL = NONE;

	public static final String[] protocolStrings = { "None", "WEP", "WPA/WPA2" };

	public enum ConnectToWifiResult {
		ALREADY_CONNECTED, INVALID_NET_ID, NETWORK_ENABLED, NETWORK_ENABLED_FAILED
	}

	// wait up to 8 seconds for wifi to enable
	private static final int WAIT_THRESHOLD = 40;

	private static final String TAG = WifiUtil.class.getSimpleName();

	public static WifiModel getCurrentWifiModel(Context c) {
		WifiConfiguration wc = getCurrentWifiConfig(c);
		if (wc != null) {
			String ssid = wc.SSID;
			int protocol = getWifiProtocol(wc);
			protocol = (protocol == -1) ? 1 : protocol;
			String password = null;
			try {
				password = RootUtil.getWifiPassword(c, ssid);
			} catch (PasswordNotFoundException e) {
				Log.e(TAG,
						"password not found when trying to get it using root access");
				e.printStackTrace();
			}
			return new WifiModel(ssid, password, protocol);
		} else {
			return null;
		}
	}

	public static WifiModel getWifiModelFromUri(Context c, Uri wifiUri) {
		String ssid = wifiUri.getHost();
		String pw = wifiUri.getLastPathSegment();
		int protocol = Integer.parseInt(wifiUri.getFragment());
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
		int protocol = getWifiProtocol(wc);
		protocol = (protocol == -1) ? 1 : protocol;
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

	public static int getWifiProtocol(WifiConfiguration wc) {
		if (wc == null) {
			Log.e(TAG, "wc was null");
			return -1;
		}

		if (wc.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.WPA_PSK)) {
			// WPA/WPA2 network, key is in wfc.preSharedKey
			return WPA;
		} else if (wc.allowedAuthAlgorithms.isEmpty()) {
			// this is an open network
			return NONE;
		} else if (wc.allowedKeyManagement.get(WifiConfiguration.KeyMgmt.NONE)) {
			// WEP network, the key is in wfc.wepKeys[wfc.wepTxKeyIndex]
			return WEP;
		} else {
			// not one of the above..
			Log.e(TAG, "Did not find wifi protocol");
			return -1;
		}
	}

	public static boolean isWifiEnabled(Context c) {
		WifiManager mWm = (WifiManager) c
				.getSystemService(Context.WIFI_SERVICE);

		if (mWm != null && mWm.isWifiEnabled()) {
			return true;
		}
		return false;
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
				&& wifiUri.getPort() == -1 && wifiUri.getQuery() == null
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
		} else if (wm.getPassword() == null && wm.getProtocol() != -1
				&& wm.getSSID() == null) {
			return false;
		} else {
			return true;
		}
	}

	public static boolean isValidSsid(String ssid) {
		return ssid.length() > 0;
	}

	public static boolean isValidPassword(int protocol, String password) {
		switch (protocol) {
		case NONE:
			return true;
		case WEP:
			return password.length() > 0;
		case WPA:
			return password.length() >= 8;
		}
		return false;
	}

	public static ConnectToWifiResult connectToWifi(Context c,
			WifiModel mWifiModel) {
		WifiManager mWm = (WifiManager) c
				.getSystemService(Context.WIFI_SERVICE);

		// enable wifi if disabled, wait until finished
		if (!enableWifiAndWait(mWm)) {
			return ConnectToWifiResult.NETWORK_ENABLED_FAILED;
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
		return connectToNetwork(c, netId);
	}

	public static ConnectToWifiResult connectToNetwork(Context c, int netId) {
		WifiManager mWm = (WifiManager) c
				.getSystemService(Context.WIFI_SERVICE);

		if (netId == -1) {
			return ConnectToWifiResult.INVALID_NET_ID;
		}

		// connect to wifi if disabled, wait until finished
		if (!enableWifiAndWait(mWm)) {
			return ConnectToWifiResult.NETWORK_ENABLED_FAILED;
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

		int protocol = mWifiModel.getProtocol();
		String pw = mWifiModel.getPassword();

		WifiConfiguration mWc = new WifiConfiguration();
		mWc.SSID = mWifiModel.getSSID();
		mWc.status = WifiConfiguration.Status.DISABLED;

		// http://kmansoft.com/2010/04/08/adding-wifi-networks-to-known-list/
		if (protocol == NONE) {
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
		} else if (protocol == WEP) {
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
		} else if (protocol == WPA) {
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

	public static boolean disableWifi(WifiManager mWm) {
		if (mWm.isWifiEnabled()) {
			Log.i(TAG, "wifi was enabled, disabling wifi");
			return mWm.setWifiEnabled(false);
		}
		return true;
	}

	public static boolean enableWifi(WifiManager mWm) {
		if (!mWm.isWifiEnabled()) {
			Log.i(TAG, "wifi was disabled, enabling wifi");
			return mWm.setWifiEnabled(true);
		} else {
			return true;
		}
	}

	// use this for non-ui operations instead of EnableWifiTask
	public static boolean enableWifiAndWait(WifiManager mWm) {
		if (enableWifi(mWm)) {
			int wait = 0;
			// time it so that if waited longer than WAIT_THRESHOLD / 5 seconds,
			// bail out
			while (!mWm.isWifiEnabled() && wait < WAIT_THRESHOLD) {
				// waiting on wifi
				Log.v(TAG, "waiting for wifi to be enabled..");
				try {
					Thread.sleep(200);
				} catch (InterruptedException e) {
					Log.e(TAG,
							"thread was interrupted while sleeping.  was waiting for wifi to be enabled");
					e.printStackTrace();
				}
				wait++;
			}
			if (wait >= WAIT_THRESHOLD) {
				Log.e(TAG,
						"waited for wifi to enable for longer than 8 seconds, bailing out");
				BugSenseHandler.log("ENABLEWIFI", new Exception(
						"enabling wifi timed out, user does not have wifi?"));
				return false;
			}
			return true;
		}
		return false;
	}

	// use this for front-facing enabling wifi tasks
	public static class EnableWifiTask extends AsyncTask<Void, Void, Void> {
		Context c;
		ProgressDialog pd;
		EnableWifiTaskListener listener;

		public EnableWifiTask(Context c, EnableWifiTaskListener listener) {
			this.c = c;
			this.listener = listener;
		}

		@Override
		protected Void doInBackground(Void... params) {
			WifiManager mWm = (WifiManager) c
					.getSystemService(Context.WIFI_SERVICE);

			if (!enableWifiAndWait(mWm)) {
				Log.e(TAG,
						"enabling wifi timed out in WifiUtil.EnableWifiTask.doInBackground");
			}

			return null;
		}

		@Override
		protected void onPreExecute() {
			pd = ProgressDialog.show(c, "WiFi", "Turning on..", true);
		}

		@Override
		protected void onPostExecute(Void result) {
			pd.dismiss();
			if (listener != null) {
				listener.OnWifiEnabled();
			}
		}
	}

	public static class EnableWifiTaskBundle {
		Context c;
		EnableWifiTaskListener listener;

		public EnableWifiTaskBundle(Context c, EnableWifiTaskListener listener) {
			this.c = c;
			this.listener = listener;
		}
	}

	public static interface EnableWifiTaskListener {
		void OnWifiEnabled();
	}

	public static int getNetworkId(Context c, WifiModel mWifiModel,
			WifiManager mWm) {
		List<WifiConfiguration> configuredNetworks = mWm
				.getConfiguredNetworks();

		String mSSID = mWifiModel.getSSID();

		for (WifiConfiguration wifiConfig : configuredNetworks) {
			if ((wifiConfig != null) && wifiConfig.SSID.equals(mSSID)) {
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

		if (configuredNetworks == null) {
			return new String[0];
		}

		String[] toReturn = new String[configuredNetworks.size()];

		for (int i = 0; i < configuredNetworks.size(); i++) {
			toReturn[i] = Util.stripQuotes(configuredNetworks.get(i).SSID);
		}

		return toReturn;
	}

	// dialog to prompt user to enable wifi
	public static void showWifiDialog(final Context c, String msg,
			final EnableWifiTaskListener listener) {
		AlertDialog.Builder builder = new AlertDialog.Builder(c);

		builder.setTitle(R.string.show_wifi_add_title);
		builder.setMessage(msg);
		builder.setPositiveButton(R.string.enable, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				new WifiUtil.EnableWifiTask(c, listener).execute();
			}
		});
		builder.setCancelable(false);

		builder.create().show();
	}

	public static void processWifiUri(Activity a, String wifiString) {
		WifiModel receivedWifiModel = new WifiModel(wifiString);
		if (WifiUtil.isValidWifiModel(receivedWifiModel)) {
			switch (connectToWifi(a, receivedWifiModel)) {
			case NETWORK_ENABLED:
				Log.i(TAG,
						"successfully connected to network, successfully processed");
				break;
			case ALREADY_CONNECTED:
				Log.i(TAG, "tried to connect to current network");

				// turn wifi off
				WifiManager mWm = (WifiManager) a.getApplicationContext()
						.getSystemService(Context.WIFI_SERVICE);
				disableWifi(mWm);

				Util.shortToast(a, String.format(
						a.getString(R.string.wifi_connect_already),
						receivedWifiModel.getTrimmedSSID()));

				a.finish();
				break;
			case INVALID_NET_ID:
				Log.e(TAG,
						"failed to connect to wifi, invalid wifi configs probably");
				Util.shortToast(a, a.getString(R.string.invalid_wifi_sticker));

				a.finish();
				break;
			case NETWORK_ENABLED_FAILED:
				Log.e(TAG, "failed to enable wifi, does this device have wifi?");
				Util.shortToast(a, a.getString(R.string.enable_wifi_fail));
			default:
				Log.e(TAG, "very bad failure");
				Util.shortToast(a, a.getString(R.string.general_fail));
				break;
			}
		} else {
			Log.e(TAG, "invalid wifi model when processing wifi URI");
			Util.shortToast(a, a.getString(R.string.invalid_wifi_sticker));
		}
	}
}
