package net.jessechen.instawifi;

import net.jessechen.instawifi.misc.WifiReceiver;
import net.jessechen.instawifi.util.NfcUtil;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockActivity;

@TargetApi(10)
public class InstaWifiHandler extends SherlockActivity {
	static TextView status;
	NfcAdapter mNfcAdapter;
	WifiReceiver mReceiver;
	boolean receiverRemoved;
	static boolean triedAssociating = false;
	static WifiManager mWifiManager;
	static Activity a;

	private static final String TAG = InstaWifiHandler.class.getSimpleName();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.handler);

		a = this;
		mWifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
		status = (TextView) findViewById(R.id.status_text);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		if (mNfcAdapter == null) {
			Util.longToast(this, "NFC is not available");
			finish();
			return;
		}

		mReceiver = new WifiReceiver();
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

		triedAssociating = false;
		
		if (!receiverRemoved) {
			unregisterWifiReceiver();
		}
	}

	public static void handleConnectionChanged(SupplicantState state) {
		if (SupplicantState.COMPLETED.equals(state) && triedAssociating) {
			triedAssociating = false;
			// wifi success
			status.setText(R.string.status_connected);
			String ssid = mWifiManager.getConnectionInfo().getSSID();
			Log.i(TAG, String.format("wifi connection completed on %s", ssid));
			Util.shortToast(a, String.format(
					a.getString(R.string.wifi_connect_success), ssid));
			Log.i(TAG, "finishing activity bye");
			a.finish();
		} else if (SupplicantState.SCANNING.equals(state)) {
			status.setText(R.string.status_scanning);
			Log.i(TAG, "scanning for a network");
		} else if (SupplicantState.ASSOCIATING.equals(state)) {
			status.setText(R.string.status_associating);
			Log.i(TAG, "associating to network");
			triedAssociating = true;
		} else if (SupplicantState.DISCONNECTED.equals(state)
				&& triedAssociating) {
			triedAssociating = false;
			status.setText(R.string.status_disconnected);
			Log.e(TAG, "wifi connection failed");
			Util.longToast(a, a.getString(R.string.wifi_connect_fail));
			a.finish();
		}
	}
}
