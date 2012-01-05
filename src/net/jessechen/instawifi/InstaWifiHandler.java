package net.jessechen.instawifi;

import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.NfcUtil;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
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
import android.os.Parcelable;
import android.util.Log;

public class InstaWifiHandler extends Activity implements
		CreateNdefMessageCallback, OnNdefPushCompleteCallback {
	// maybe this should be a dialog somehow?
	NfcAdapter mNfcAdapter;
	private static final int MESSAGE_SENT = 1;
	private String ssid = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handler);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter == null) {
			Util.longToast(this, "NFC is not available");
			finish();
			return;
		}

		// register callback (do i need this check?)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			mNfcAdapter.setNdefPushMessageCallback(this, this);
		}
	}

	private void registerWifiReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);

		registerReceiver(mReceiver, intentFilter);
	}

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

	private void handleSupplicantConnectionChanged(boolean booleanExtra) {
		if (booleanExtra) {
			// wifi success
			Log.i(Util.TAG, String.format(
					"wifi connected to broadcast received on SSID: %s", ssid));
			Util.shortToast(this, String.format(
					getString(R.string.wifi_connect_success), ssid));
			finish();
		} else {
			Util.shortToast(this, getString(R.string.wifi_connect_fail));
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		registerWifiReceiver();

		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			NdefMessage[] messages = NfcUtil.getNdefMessages(getIntent());
			String wifiString = new String(
					messages[0].getRecords()[0].getType());
			processWifiUri(wifiString);
		}
	}

	/**
	 * Parses the NDEF Message from the intent and prints to the TextView
	 */
	void processIntent(Intent intent) {
		Parcelable[] rawMsgs = intent
				.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		// only one message sent during the beam
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		// record 0 contains the MIME type, record 1 is the AAR, if present
		String wifiString = new String(msg.getRecords()[0].getPayload());
		processWifiUri(wifiString);
	}

	@Override
	protected void onPause() {
		super.onPause();

		unregisterReceiver(mReceiver);
	}

	private void processWifiUri(String wifiString) {
		Uri wifiUri = Uri.parse(wifiString);
		if (WifiUtil.isValidWifiUri(wifiUri)) {
			WifiUtil.connectToWifi(this, wifiUri);
			ssid = wifiUri.getHost();
		} else {
			Log.e(Util.TAG, "invalid wifi uri");
			Util.shortToast(this, getString(R.string.invalid_wifi_sticker));
		}
	}

	@Override
	public void onNewIntent(Intent intent) {
		// onResume gets called after this to handle the intent
		setIntent(intent);
	}

	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		// TODO: android beam work
		WifiModel currentWifi = WifiUtil.getCurrentWifiModel(this);

		if (currentWifi != null) {
			return NfcUtil.getWifiAsNdef(currentWifi.getSSID(),
					currentWifi.getPassword(), currentWifi.getProtocol());
		} else {
			Util.longToast(this, "Error: Wifi is not enabled");
			return null;
		}
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
						getString(R.string.beam_success));
				break;
			}
		}
	};
}
