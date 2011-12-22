package net.jessechen.instawifi;

import java.util.List;

import net.jessechen.instawifi.util.NfcUtil;
import net.jessechen.instawifi.util.Util;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class InstaWifiHandler extends Activity implements
		CreateNdefMessageCallback, OnNdefPushCompleteCallback {
	// maybe this should be a dialog somehow?
	NfcAdapter mNfcAdapter;
	private static final int MESSAGE_SENT = 1;
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION.equals(action)) {
				handleSupplicantConnectionChanged(intent.getBooleanExtra(
						WifiManager.EXTRA_SUPPLICANT_CONNECTED, false));
			}
		}

	};

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handler);

		// register callback (do i need this check?)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			mNfcAdapter.setNdefPushMessageCallback(this, this);
		}
	}

	private void handleSupplicantConnectionChanged(boolean booleanExtra) {
		if (booleanExtra) {
			// wifi success
			Util.shortToast(this, getString(R.string.wifi_connect_success));
		} else {
			Util.shortToast(this, getString(R.string.wifi_connect_fail));
		}
	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		return NfcUtil.getWifiAsNdef(null, null, null);
	}

	@Override
	public void onNdefPushComplete(NfcEvent event) {
		// A handler is needed to send messages to the activity when this
		// callback occurs, because it happens from a binder thread
		mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
	}

	/** This handler receives a message from onNdefPushComplete */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_SENT:
				Util.longToast(getApplicationContext(),
						getString(R.string.beam_success)).show();
				break;
			}
		}
	};

	@Override
	protected void onResume() {
		super.onResume();

		registerWifiReceiver();

		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			NdefMessage[] messages = NfcUtil.getNdefMessages(getIntent());
			String wifiString = new String(
					messages[0].getRecords()[0].getType());
			// wifi://helloworld/cabdad1234/wpa
			Uri wifiUri = Uri.parse(wifiString);
			connectToWifi(wifiUri);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(mReceiver);
	}

	@Override
	public void onNewIntent(Intent intent) {
		// onResume gets called after this to handle the intent
		setIntent(intent);
	}

	private void connectToWifi(Uri wifiUri) {
		WifiManager mWm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		if (!mWm.isWifiEnabled()) { // TODO: what happens in airplane mode?
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

		// thanks to:
		// http://kmansoft.com/2010/04/08/adding-wifi-networks-to-known-list/
		if (protocol.equals("")) { // TODO: something like this, need to check
			// open network configs

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
		if (netId != -1) {
			mWm.enableNetwork(netId, true);
			Log.i(Util.TAG, "attemping to connect to new network");
			// TODO: needs a broadcast receiver on
			// WifiManager.SUPPLICANT_STATE_CHANGED_ACTION
		} else {
			Log.i(Util.TAG,
					"netId == -1, failed to add network to known networks");
		}
	}

	private void registerWifiReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);

		registerReceiver(mReceiver, intentFilter);
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
