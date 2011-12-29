package net.jessechen.instawifi;

import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.NfcUtil;
import net.jessechen.instawifi.util.Util;
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
		// TODO: android beam work
		WifiModel currentWifi = Util.getCurrentWifiModel(this);

		return NfcUtil.getWifiAsNdef(currentWifi.getSSID(),
				currentWifi.getPassword(), currentWifi.getProtocol());
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
			Util.connectToWifi(this, wifiUri);
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

	private void registerWifiReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION);

		registerReceiver(mReceiver, intentFilter);
	}
}
