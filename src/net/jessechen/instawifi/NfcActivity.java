package net.jessechen.instawifi;

import net.jessechen.instawifi.billing.BillingService;
import net.jessechen.instawifi.misc.AddNetworkDialog;
import net.jessechen.instawifi.misc.MyTabListener;
import net.jessechen.instawifi.misc.SpinnerArrayAdapter;
import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.NfcUtil;
import net.jessechen.instawifi.util.RootUtil.PasswordNotFoundException;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.bugsense.trace.BugSenseHandler;

@TargetApi(14)
public class NfcActivity extends SherlockFragmentActivity implements
		OnItemSelectedListener {
	boolean mWriteMode = false;

	NfcAdapter mNfcAdapter;
	PendingIntent mNfcPendingIntent;
	IntentFilter[] mWriteTagFilters;
	final static int MESSAGE_SENT = 1;

	AlertDialog alert;
	Button writeTag;
	Spinner networkSpinner;
	ArrayAdapter<String> networkAdapter;
	Spinner protocolSpinner;
	TextView passwordText;
	EditText passwordField;
	CheckBox revealPassword;

	static Context c;
	Intent picIntent;
	BillingService mBillingService;

	private static final String TAG = NfcActivity.class.getSimpleName();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.nfc_activity);

		c = getApplicationContext();

		// crash reporting and analytics
		BugSenseHandler.setup(this, Util.bugsenseKey);

		mBillingService = new BillingService();
		mBillingService.setContext(c);

		if (Util.hasNfc(c)
				&& (mNfcAdapter = ((NfcManager) getSystemService(Context.NFC_SERVICE))
						.getDefaultAdapter()) != null) {

			// Android Beam setup
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mNfcAdapter.setNdefPushMessageCallback(beamPushSetup(), this);
				mNfcAdapter.setOnNdefPushCompleteCallback(
						beamPushCompleteSetup(), this);
			}

			writeTag = (Button) findViewById(R.id.b_write_tag);
			networkSpinner = (Spinner) findViewById(R.id.network_spinner);
			protocolSpinner = (Spinner) findViewById(R.id.protocol_spinner);
			passwordText = (TextView) findViewById(R.id.password_text);
			passwordField = (EditText) findViewById(R.id.password_field);
			revealPassword = (CheckBox) findViewById(R.id.password_checkbox);

			writeTag.setOnClickListener(mTagWriter);

			revealPassword.setOnCheckedChangeListener(mCheckBoxListener);

			String[] networks = WifiUtil.getConfiguredNetworks(this);
			if (networks.length == 0) {
				WifiUtil.showWifiDialog(this,
						getString(R.string.show_wifi_msg_default),
						new WifiUtil.EnableWifiTaskListener() {

							@Override
							public void OnWifiEnabled() {
								setupNfcView(WifiUtil.getConfiguredNetworks(c));
							}
						});
			} else {
				setupNfcView(networks);
			}

			// restore QR tab if it was previously selected
			if (savedInstanceState != null
					&& savedInstanceState.getString("tab").equals(Util.QR)) {
				Util.curTab = Util.QR;
			}

			com.actionbarsherlock.app.ActionBar bar = getSupportActionBar();
			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
			bar.addTab(
					bar.newTab()
							.setText(Util.NFC)
							.setTabListener(
									new MyTabListener(this,
											getSupportFragmentManager(),
											Util.NFC)), Util.isNfcTabSelected());
			bar.addTab(
					bar.newTab()
							.setText(Util.QR)
							.setTabListener(
									new MyTabListener(this,
											getSupportFragmentManager(),
											Util.QR)), !Util.isNfcTabSelected());
		} else {
			// hide NFC layout
			View layout = findViewById(R.id.nfc_layout);
			layout.setVisibility(View.GONE);

			// set QR layout
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.fragment, QrFragment.getInstance()).commit();
		}
	}

	private void setupNfcView(String[] networks) {
		networkAdapter = new SpinnerArrayAdapter<String>(getApplication(),
				networks);
		networkAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		networkSpinner.setAdapter(networkAdapter);
		networkSpinner.setOnItemSelectedListener(this);

		// set spinner to current wifi config if connected to wifi
		WifiModel curWifi = WifiUtil.getCurrentWifiModel(this);
		if (curWifi != null) {
			for (int i = 0; i < networks.length; i++) {
				if (curWifi.getTrimmedSSID().equals(networks[i])) {
					networkSpinner.setSelection(i);
				}
			}
		}

		ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, WifiUtil.protocolStrings);
		protocolAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		protocolSpinner.setAdapter(protocolAdapter);
		protocolSpinner.setOnItemSelectedListener(this);
		protocolSpinner.setSelection(WifiUtil.DEFAULT_PROTOCOL);

		// Handle all of our received NFC intents in this activity.
		mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// Intent filters for writing to a tag
		IntentFilter tagDetected = new IntentFilter(
				NfcAdapter.ACTION_TAG_DISCOVERED);
		mWriteTagFilters = new IntentFilter[] { tagDetected };

		picIntent = new Intent(android.content.Intent.ACTION_SEND);
		picIntent.setType("image/*");
	}

	@Override
	protected void onResume() {
		super.onResume();
		// Check to see that the Activity started due to an Android Beam
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			processIntent(getIntent());
		}
	}

	@Override
	public void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		Tab curTab = getSupportActionBar().getSelectedTab();

		// save tab state to restore
		if (curTab != null) {
			outState.putString("tab", curTab.getText().toString());
		}
	}

	@Override
	public void onPause() {
		super.onPause();

		if (alert != null) {
			alert.dismiss();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onNewIntent(Intent intent) {
		// onResume gets called after this to handle the intent
		setIntent(intent);

		if (mWriteMode
				&& NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

			WifiModel selectedWifi = new WifiModel(networkSpinner
					.getSelectedItem().toString(), passwordField.getText()
					.toString(), protocolSpinner.getSelectedItemPosition());
			if (WifiUtil.isValidWifiModel(selectedWifi)) {
				NdefMessage wifiNdefMessage = NfcUtil.getWifiAsNdef(c,
						selectedWifi);
				if (NfcUtil.writeTag(wifiNdefMessage, detectedTag, this)) {
					Log.i(TAG, String.format("successfully wrote %s to tag",
							selectedWifi.getTrimmedSSID()));
					Util.longToast(this, getString(R.string.write_tag_success));
				} else {
					Util.longToast(this, getString(R.string.write_tag_fail));
					Log.e(TAG, "failed to write to tag, probably IOException");
				}
			} else {
				Util.shortToast(this, getString(R.string.write_tag_fail));
				Log.e(TAG, "invalid wifi model when writing tag");
			}
		}
	}

	protected void processIntent(Intent intent) {
		Parcelable[] rawMsgs = intent
				.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
		// only one message sent during the beam
		NdefMessage msg = (NdefMessage) rawMsgs[0];
		// record 0 contains the MIME type, record 1 is the AAR, if present
		String wifiString = new String(msg.getRecords()[0].getPayload());
		WifiUtil.processWifiUri(this, wifiString);
	}

	private CreateNdefMessageCallback beamPushSetup() {
		return new NfcAdapter.CreateNdefMessageCallback() {

			@Override
			public NdefMessage createNdefMessage(NfcEvent event) {
				WifiModel selectedWifi = new WifiModel(networkSpinner
						.getSelectedItem().toString(), passwordField.getText()
						.toString(), protocolSpinner.getSelectedItemPosition());

				if (WifiUtil.isValidWifiModel(selectedWifi)) {
					return NfcUtil.getWifiAsNdef(c, selectedWifi);
				} else {
					Util.longToast(c,
							"Error: could not get current wifi configurations");
					return null;
				}
			}
		};
	}

	private OnNdefPushCompleteCallback beamPushCompleteSetup() {
		return new NfcAdapter.OnNdefPushCompleteCallback() {

			@Override
			public void onNdefPushComplete(NfcEvent event) {
				// A handler is needed to send messages to the activity when
				// this
				// callback occurs, because it happens from a binder thread
				mHandler.obtainMessage(MESSAGE_SENT).sendToTarget();
			}
		};
	}

	/** This handler receives a message from onNdefPushComplete */
	private static final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_SENT:
				Util.longToast(c, c.getString(R.string.beam_success));
				break;
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.nfc, menu);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.share:
			if (Util.isNfcTabSelected()) {
				Intent intent = Util.buildAppShareIntent(c);
				startActivity(Intent.createChooser(intent, String
						.format(getString(R.string.app_share_dialog_title))));
				return true;
			} else {
				// let QrFragment handle R.id.share
				return false;
			}
		case R.id.add:
			if (WifiUtil.isWifiEnabled(this)) {
				AddNetworkDialog.show(this, c, networkSpinner);
			} else {
				WifiUtil.showWifiDialog(this,
						getString(R.string.show_wifi_msg_add),
						new WifiUtil.EnableWifiTaskListener() {

							@Override
							public void OnWifiEnabled() {
								AddNetworkDialog.show(NfcActivity.this, c,
										networkSpinner);
							}
						});
			}
			return true;
		case R.id.donate:
			startActivity(new Intent(NfcActivity.this, DonateActivity.class));
			return true;
		case R.id.help:
			startActivity(new Intent(NfcActivity.this, HelpActivity.class));
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private final View.OnClickListener mTagWriter = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if (Util.hasNfc(c)) {
				// adapter exists and is enabled.
				// Write to a tag for as long as the dialog is shown.
				enableTagWriteMode();

				alert = new AlertDialog.Builder(NfcActivity.this)
						.setTitle(getString(R.string.dialog_write_tag))
						.setOnCancelListener(
								new DialogInterface.OnCancelListener() {
									@Override
									public void onCancel(DialogInterface dialog) {
										disableTagWriteMode();
									}
								}).create();
				alert.show();
			}
		}
	};

	private final OnCheckedChangeListener mCheckBoxListener = new CompoundButton.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (isChecked) {
				passwordField.setTransformationMethod(null);
			} else {
				passwordField
						.setTransformationMethod(new PasswordTransformationMethod());
			}
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

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		switch (parent.getId()) {
		case R.id.network_spinner:
			WifiModel selectedNetwork = null;
			try {
				selectedNetwork = WifiUtil.getWifiModelFromSsid(this, parent
						.getItemAtPosition(pos).toString());
			} catch (PasswordNotFoundException e) {
				e.printStackTrace();
				Log.e(TAG, "did not find password on item selected");
			}

			if (selectedNetwork != null) {
				protocolSpinner.setSelection(selectedNetwork.getProtocol());

				passwordField.setText(Util.stripQuotes(selectedNetwork
						.getPassword()));
			}
			break;
		case R.id.protocol_spinner:
			if (protocolSpinner.getSelectedItemPosition() == WifiUtil.NONE) {
				passwordText.setVisibility(View.GONE);
				passwordField.setVisibility(View.GONE);
				revealPassword.setVisibility(View.GONE);
			} else {
				passwordText.setVisibility(View.VISIBLE);
				passwordField.setVisibility(View.VISIBLE);
				revealPassword.setVisibility(View.VISIBLE);
			}
			break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}
}
