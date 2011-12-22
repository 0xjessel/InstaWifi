package net.jessechen.instawifi;

import java.util.List;

import net.jessechen.instawifi.util.NfcUtil;
import net.jessechen.instawifi.util.Util;
import android.app.Activity;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;

public class InstaWifiHandler extends Activity {
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handler);
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			NdefMessage[] messages = NfcUtil.getNdefMessages(getIntent());
			String wifiString = new String(
					messages[0].getRecords()[0].getType());
			// wifi://helloworld/cabdad1234/wpa
			Uri wifiUri = Uri.parse(wifiString);
			connectToWifi(wifiUri);
		}
	}

	private void connectToWifi(Uri wifiUri) {
		WifiManager mWm = (WifiManager) getSystemService(this.WIFI_SERVICE);
		if (!mWm.isWifiEnabled()) {
			mWm.setWifiEnabled(true);
			Log.i(Util.TAG, "wifi was disabled, enabling wifi");
		}

		String ssid = "\"".concat(wifiUri.getHost()).concat("\"");
		List<String> pathSegments = wifiUri.getPathSegments();
		String pw = pathSegments.get(0);
		String protocol = pathSegments.get(1);

		WifiConfiguration mWc = new WifiConfiguration();
		mWc.SSID = ssid;
		mWc.status = WifiConfiguration.Status.DISABLED;
		
		if (protocol.equals("")) { 
			// open network configs
			
			mWc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
			mWc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			mWc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			mWc.allowedAuthAlgorithms.clear();
			mWc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			mWc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
		} else if (protocol.equals("WEP")) {
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
		} else if (protocol.equals("WPA")) {
			// WPA network configs
			
			mWc.allowedProtocols.set(WifiConfiguration.Protocol.RSN);
			mWc.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
			mWc.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
			mWc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
			mWc.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
			mWc.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);

			mWc.preSharedKey = "\"".concat(pw).concat("\"");

		}

		// add network to known list
		int netId = mWm.addNetwork(mWc);
		if (netId != -1) {
			mWm.enableNetwork(netId, true);
			Log.i(Util.TAG, "attemping to connect to new network");
		} else {
			Log.i(Util.TAG,
					"netId == -1, failed to add network to known networks");
		}
	}

	private boolean isHexString(String s) {
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
