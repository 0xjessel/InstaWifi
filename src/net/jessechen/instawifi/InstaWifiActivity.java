package net.jessechen.instawifi;

import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.NfcUtil;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.view.View;

public class InstaWifiActivity extends Activity {
	private boolean mWriteMode = false;
	NfcAdapter mNfcAdapter;

	PendingIntent mNfcPendingIntent;
	IntentFilter[] mWriteTagFilters;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

		setContentView(R.layout.main);
		findViewById(R.id.b_writetag).setOnClickListener(mTagWriter);
		findViewById(R.id.b_test).setOnClickListener(mTestButtonListener);

		// Handle all of our received NFC intents in this activity.
		mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// Intent filters for writing to a tag
		IntentFilter tagDetected = new IntentFilter(
				NfcAdapter.ACTION_TAG_DISCOVERED);
		mWriteTagFilters = new IntentFilter[] { tagDetected };
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (mWriteMode
				&& NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
			WifiModel currentWifi = WifiUtil.getCurrentWifiModel(this);
			NfcUtil.writeTag(
					NfcUtil.getWifiAsNdef(currentWifi.getSSID(),
							currentWifi.getPassword(),
							currentWifi.getProtocol()), detectedTag, this);
		}
	}

	private View.OnClickListener mTagWriter = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			// Write to a tag for as long as the dialog is shown.
			enableTagWriteMode();

			new AlertDialog.Builder(InstaWifiActivity.this)
					.setTitle(getString(R.string.dialog_write_tag))
					.setOnCancelListener(
							new DialogInterface.OnCancelListener() {
								@Override
								public void onCancel(DialogInterface dialog) {
									disableTagWriteMode();
								}
							}).create().show();
		}
	};

	private View.OnClickListener mTestButtonListener = new View.OnClickListener() {

		@Override
		public void onClick(View v) {
			WifiModel currentWifi = WifiUtil
					.getCurrentWifiModel(getApplicationContext());
			Util.longToast(
					getApplicationContext(),
					String.format("SSID: %s, PW: %s, PROTOCOL: %s",
							currentWifi.getSSID(), currentWifi.getPassword(),
							currentWifi.getProtocol())).show();
			//
			// String url = String.format(Util.WIFI_URI_SCHEME, "clink",
			// "5104779276", "wep");
			// Uri wifiUri = Uri.parse(url);
			// Util.connectToWifi(getApplicationContext(), wifiUri);
		}
	};

	private void enableTagWriteMode() {
		mWriteMode = true;
		IntentFilter tagDetected = new IntentFilter(
				NfcAdapter.ACTION_TAG_DISCOVERED);
		mWriteTagFilters = new IntentFilter[] { tagDetected };
		mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent,
				mWriteTagFilters, null);
	}

	private void disableTagWriteMode() {
		mWriteMode = false;
		mNfcAdapter.disableForegroundDispatch(this);
	}
}