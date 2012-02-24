package net.jessechen.instawifi;

import net.jessechen.instawifi.misc.MyTabListener;
import net.jessechen.instawifi.misc.SpinnerArrayAdapter;
import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.NfcUtil;
import net.jessechen.instawifi.util.RootUtil.PasswordNotFoundException;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
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

/* 
 * TODO: droid x edittext is see through
 * TODO: droid x text is white
 * TODO: share app
 * TODO: parse wifipw.txt for networks instead of using the wifi api
 * TODO: ridiculously long ssid breaks gridlayout, maybe when getting configured networks, automatically clip long ssids and append '...'
 * TODO: write to tag button
 */
public class NfcActivity extends FragmentActivity implements
		OnItemSelectedListener {
	boolean mWriteMode = false;

	NfcAdapter mNfcAdapter;
	PendingIntent mNfcPendingIntent;
	IntentFilter[] mWriteTagFilters;
	final int MESSAGE_SENT = 1;

	AlertDialog alert;
	Button writeTag;
	Spinner networkSpinner;
	ArrayAdapter<String> networkAdapter;
	Spinner protocolSpinner;
	TextView passwordText;
	EditText passwordField;
	CheckBox revealPassword;

	Intent picIntent;

	private static final String TAG = NfcActivity.class.getSimpleName();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mNfcAdapter = ((NfcManager) getSystemService(Context.NFC_SERVICE))
				.getDefaultAdapter();

		setContentView(R.layout.nfc_activity);

		if (Util.hasNfc(mNfcAdapter)) {
			// Android Beam setup
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				mNfcAdapter.setNdefPushMessageCallback(
						beamPushSetup(), this);
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

			// TODO: stupid getconfigurednetworks sometimes returns empty..
			String[] networks = WifiUtil.getConfiguredNetworks(this);
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

			ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(
					this, android.R.layout.simple_spinner_item,
					WifiUtil.protocolStrings);
			protocolAdapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			protocolSpinner.setAdapter(protocolAdapter);
			protocolSpinner.setOnItemSelectedListener(this);

			// Handle all of our received NFC intents in this activity.
			mNfcPendingIntent = PendingIntent.getActivity(this, 0,
					new Intent(this, getClass())
							.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

			// Intent filters for writing to a tag
			IntentFilter tagDetected = new IntentFilter(
					NfcAdapter.ACTION_TAG_DISCOVERED);
			mWriteTagFilters = new IntentFilter[] { tagDetected };

			picIntent = new Intent(android.content.Intent.ACTION_SEND);
			picIntent.setType("image/*");

			boolean nfcTabSelected = true;
			// restore QR tab if it was previously selected
			if (savedInstanceState != null
					&& savedInstanceState.getString("tab").equals(
							getString(R.string.qr_tab))) {
				nfcTabSelected = false;
			}

			android.support.v4.app.ActionBar bar = getSupportActionBar();
			bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

			bar.addTab(
					bar.newTab()
							.setText(getString(R.string.nfc_tab))
							.setTabListener(
									new MyTabListener(this,
											getSupportFragmentManager(),
											getString(R.string.nfc_tab))),
					nfcTabSelected);

			bar.addTab(
					bar.newTab()
							.setText(getString(R.string.qr_tab))
							.setTabListener(
									new MyTabListener(this,
											getSupportFragmentManager(),
											getString(R.string.qr_tab))),
					!nfcTabSelected);
		} else {
			// hide NFC layout
			View layout = findViewById(R.id.nfc_layout);
			layout.setVisibility(View.GONE);

			// set QR layout
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.fragment, QrFragment.getInstance()).commit();
		}
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
	protected void onSaveInstanceState(final Bundle outState) {
		android.support.v4.app.ActionBar.Tab curTab = getSupportActionBar()
				.getSelectedTab();

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
				NdefMessage wifiNdefMessage = NfcUtil
						.getWifiAsNdef(selectedWifi);
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
					return NfcUtil.getWifiAsNdef(selectedWifi);
				} else {
					Util.longToast(getApplicationContext(),
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
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case MESSAGE_SENT:
				Util.longToast(getApplicationContext(),
						getString(R.string.beam_success));
				// TODO: add finish();?
				break;
			}
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.nfc, menu);

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			// ShareActionProvider mShareActionProvider = (ShareActionProvider)
			// menu
			// .findItem(R.id.share).getActionProvider();
			// mShareActionProvider.setShareIntent(picIntent);
		}
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.share:
			break;
		case R.id.add:
			if (WifiUtil.isWifiEnabled(this)) {
				showAddNetworkDialog();
			} else {
				showWifiDialog();
			}
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private View.OnClickListener mTagWriter = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
			if (Util.hasNfc(mNfcAdapter)) {
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

	private OnCheckedChangeListener mCheckBoxListener = new CompoundButton.OnCheckedChangeListener() {

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
			if (protocolSpinner.getSelectedItemPosition() == WifiUtil.OPEN) {
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

	// dialog to prompt user to enable wifi
	private void showWifiDialog() {
		final Context c = this;
		AlertDialog.Builder builder = new AlertDialog.Builder(c);

		builder.setTitle(R.string.enable_wifi_for_add_title);
		builder.setMessage(R.string.enable_wifi_for_add);
		builder.setPositiveButton(R.string.enable, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				new WifiUtil.EnableWifiTask(c,
						new WifiUtil.EnableWifiTaskListener() {

							@Override
							public void OnWifiEnabled() {
								showAddNetworkDialog();
							}
						}).execute();
			}
		});

		builder.setNegativeButton(R.string.cancel, null);
		builder.create().show();
	}

	// add network dialog
	private void showAddNetworkDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		LayoutInflater inflator = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflator.inflate(R.layout.add_dialog,
				(ViewGroup) findViewById(R.id.dialog_root));
		builder.setView(layout);

		final Spinner newProtocolSpinner = (Spinner) layout
				.findViewById(R.id.add_protocol_spinner);
		final TextView newPasswordText = (TextView) layout
				.findViewById(R.id.add_password_text);
		final EditText newSsidField = (EditText) layout
				.findViewById(R.id.add_network_ssid);
		final EditText newPasswordField = (EditText) layout
				.findViewById(R.id.add_network_password);
		final CheckBox newRevealPassword = (CheckBox) layout
				.findViewById(R.id.add_password_checkbox);

		newRevealPassword
				.setOnCheckedChangeListener(new OnCheckedChangeListener() {

					@Override
					public void onCheckedChanged(CompoundButton buttonView,
							boolean isChecked) {
						if (isChecked) {
							newPasswordField.setTransformationMethod(null);
						} else {
							newPasswordField
									.setTransformationMethod(new PasswordTransformationMethod());
						}
					}
				});

		ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, WifiUtil.protocolStrings);
		protocolAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		newProtocolSpinner.setAdapter(protocolAdapter);

		builder.setTitle(getString(R.string.add_new_network));

		builder.setPositiveButton(R.string.add_button, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				WifiModel newWifiModel = new WifiModel(newSsidField.getText()
						.toString(), newPasswordField.getText().toString(),
						newProtocolSpinner.getSelectedItemPosition());

				WifiManager mWm = (WifiManager) getSystemService(Context.WIFI_SERVICE);

				int netId = WifiUtil.addWifiNetwork(getApplicationContext(),
						newWifiModel, mWm);
				if (netId != -1) {
					String[] updatedNetworks = WifiUtil
							.getConfiguredNetworks(getApplicationContext());
					networkAdapter = new SpinnerArrayAdapter<String>(
							getApplication(), updatedNetworks);
					networkAdapter
							.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

					// update NfcActivity's spinner iff device does not have NFC
					if (Util.hasNfc(mNfcAdapter)) {
						networkSpinner.setAdapter(networkAdapter);

						// set spinner to the network just added
						networkSpinner.setSelection(networkAdapter.getCount() - 1);
					}

					QrFragment qrFrag = (QrFragment) getSupportFragmentManager()
							.findFragmentById(R.id.fragment);
					if (qrFrag != null) {
						qrFrag.updateNetworkSpinner(networkAdapter);
					}

					Util.shortToast(getApplicationContext(),
							getString(R.string.success));
				} else {
					Util.shortToast(getApplicationContext(),
							getString(R.string.add_new_network_fail));
				}

				// hide keyboard after closing dialog
				InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(
						newSsidField.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
			}
		});

		builder.setNegativeButton(R.string.cancel, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// hide keyboard after closing dialog
				InputMethodManager inputManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(
						newSsidField.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
			}
		});

		final AlertDialog alertDialog = builder.create();
		alertDialog.show();

		final Button addButton = alertDialog
				.getButton(DialogInterface.BUTTON_POSITIVE);
		addButton.setEnabled(false);

		final EditTextWatcher mWatcher = new EditTextWatcher(alertDialog,
				newSsidField, newPasswordField, newProtocolSpinner);
		newSsidField.removeTextChangedListener(mWatcher);
		newPasswordField.removeTextChangedListener(mWatcher);
		newSsidField.addTextChangedListener(mWatcher);
		newPasswordField.addTextChangedListener(mWatcher);

		newProtocolSpinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int pos, long id) {
						switch (parent.getId()) {
						case R.id.add_protocol_spinner:
							// update positive button state when changing
							// protocol
							onEditTextChanged(newSsidField, newPasswordField,
									newProtocolSpinner, addButton);

							if (newProtocolSpinner.getSelectedItemPosition() == WifiUtil.OPEN) {
								newPasswordField.setVisibility(View.GONE);
								newPasswordText.setVisibility(View.GONE);
								newRevealPassword.setVisibility(View.GONE);
							} else {
								newPasswordField.setVisibility(View.VISIBLE);
								newPasswordText.setVisibility(View.VISIBLE);
								newRevealPassword.setVisibility(View.VISIBLE);
							}
							break;
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});
	}

	private void onEditTextChanged(EditText mSsidField, EditText mPwField,
			Spinner mProtocolSpinner, Button mAddButton) {
		String ssid = mSsidField.getText().toString();
		String password = mPwField.getText().toString();
		if (WifiUtil.isValidSsid(ssid)
				&& WifiUtil.isValidPassword(
						mProtocolSpinner.getSelectedItemPosition(), password)) {
			mAddButton.setEnabled(true);
		} else {
			mAddButton.setEnabled(false);
		}
	}

	private class EditTextWatcher implements TextWatcher {
		Button mAddButton;
		EditText mSsidField, mPwField;
		Spinner mProtocolSpinner;

		public EditTextWatcher(AlertDialog dialog, EditText field1,
				EditText field2, Spinner protocolSpinner) {
			mAddButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
			mSsidField = field1;
			mPwField = field2;
			mProtocolSpinner = protocolSpinner;
		}

		@Override
		public void afterTextChanged(Editable s) {
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			onEditTextChanged(mSsidField, mPwField, mProtocolSpinner,
					mAddButton);
		}
	}
}
