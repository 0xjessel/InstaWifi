package net.jessechen.instawifi;

import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.NfcUtil;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

public class InstaWifiActivity extends FragmentActivity {
	private boolean mWriteMode = false;
	NfcAdapter mNfcAdapter;

	PendingIntent mNfcPendingIntent;
	IntentFilter[] mWriteTagFilters;
	
	private static final String TAG = InstaWifiActivity.class.getName();

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
		
		android.support.v4.app.ActionBar bar = getSupportActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		bar.addTab(bar.newTab().setText("NFC"));
		bar.addTab(bar.newTab().setText("QR"));
	}

	@Override
	protected void onNewIntent(Intent intent) {
		if (mWriteMode
				&& NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

			WifiModel currentWifi = WifiUtil.getCurrentWifiModel(this);
			if (WifiUtil.isValidWifiModel(currentWifi)) {
				NdefMessage wifiNdefMessage = NfcUtil.getWifiAsNdef(
						currentWifi.getSSID(), currentWifi.getPassword(),
						currentWifi.getProtocol());
				if (NfcUtil.writeTag(wifiNdefMessage, detectedTag, this)) {
					Log.i(TAG, String.format(
							"successfully wrote %s to tag",
							currentWifi.getTrimmedSSID()));
					Util.longToast(this, getString(R.string.write_tag_success));
				} else {
					Util.longToast(this, getString(R.string.write_tag_fail));
					Log.e(TAG,
							"failed to write to tag, probably IOException");
				}
			} else {
				Util.shortToast(this, getString(R.string.write_tag_fail));
				Log.e(TAG,
						"invalid wifi model when writing tag, probably wifi not on");
			}
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
			// WifiModel currentWifi = WifiUtil
			// .getCurrentWifiModel(getApplicationContext());
			// Util.longToast(getApplicationContext(), String.format(
			// "SSID: %s, PW: %s, PROTOCOL: %s", currentWifi.getSSID(),
			// currentWifi.getPassword(), currentWifi.getProtocol()));
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
