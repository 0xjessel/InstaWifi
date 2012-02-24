package net.jessechen.instawifi;

import net.jessechen.instawifi.misc.WifiReceiver;
import net.jessechen.instawifi.util.NfcUtil;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;

public class InstaWifiHandler extends Activity {
	// maybe this should be a dialog somehow?
	NfcAdapter mNfcAdapter;
	WifiReceiver mReceiver;
	boolean receiverRemoved;

	@SuppressWarnings("unused")
	private static final String TAG = InstaWifiHandler.class.getSimpleName();

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

		mReceiver = new WifiReceiver(getApplicationContext(), this,
				(WifiManager) getSystemService(Context.WIFI_SERVICE));

		receiverRemoved = false;
	}

	private void registerWifiReceiver() {
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.SUPPLICANT_STATE_CHANGED_ACTION);

		registerReceiver(mReceiver, intentFilter);
		receiverRemoved = false;
	}

	private void unregisterWifiReceiver() {
		unregisterReceiver(mReceiver);
		receiverRemoved = true;
	}

	@Override
	protected void onResume() {
		super.onResume();

		registerWifiReceiver();

		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			NdefMessage[] messages = NfcUtil.getNdefMessages(getIntent());
			String wifiString = new String(
					messages[0].getRecords()[0].getType());
			WifiUtil.processWifiUri(this, wifiString);
		}
	}

	@Override
	protected void onPause() {
		super.onPause();

		if (!receiverRemoved) {
			unregisterWifiReceiver();
		}
	}
}
