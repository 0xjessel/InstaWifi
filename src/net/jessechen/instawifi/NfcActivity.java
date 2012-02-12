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
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.Menu;
import android.support.v4.view.MenuItem;
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
import android.widget.ShareActionProvider;
import android.widget.Spinner;

/* 
 * TODO: droid x edittext is see through
 * TODO: droid x text is white
 * TODO: share app, share qr code
 * TODO: write to tag button
 * TODO: revealPassword on landscape looks bad
 */
public class NfcActivity extends FragmentActivity implements
		OnItemSelectedListener {
	private boolean mWriteMode = false;

	NfcAdapter mNfcAdapter;
	PendingIntent mNfcPendingIntent;
	IntentFilter[] mWriteTagFilters;

	AlertDialog alert;
	Button writeTag;
	Spinner networkSpinner;
	ArrayAdapter<String> networkAdapter;
	Spinner protocolSpinner;
	EditText passwordField;
	CheckBox revealPassword;

	private static final String TAG = NfcActivity.class.getSimpleName();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

		setContentView(R.layout.nfc_activity);

		writeTag = (Button) findViewById(R.id.b_write_tag);
		networkSpinner = (Spinner) findViewById(R.id.network_spinner);
		protocolSpinner = (Spinner) findViewById(R.id.protocol_spinner);
		passwordField = (EditText) findViewById(R.id.password_field);
		revealPassword = (CheckBox) findViewById(R.id.password_checkbox);

		writeTag.setOnClickListener(mTagWriter);

		revealPassword.setOnCheckedChangeListener(mCheckBoxListener);

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

		ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, WifiUtil.protocols);
		protocolAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		protocolSpinner.setAdapter(protocolAdapter);
		protocolSpinner.setOnItemSelectedListener(this);

		// Handle all of our received NFC intents in this activity.
		mNfcPendingIntent = PendingIntent.getActivity(this, 0, new Intent(this,
				getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

		// Intent filters for writing to a tag
		IntentFilter tagDetected = new IntentFilter(
				NfcAdapter.ACTION_TAG_DISCOVERED);
		mWriteTagFilters = new IntentFilter[] { tagDetected };

		boolean nfcTabSelected = true;
		if (savedInstanceState != null
				&& savedInstanceState.getString("tab").equals(
						getApplicationContext().getString(R.string.qr_tab))) {
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
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		outState.putString("tab", getSupportActionBar().getSelectedTab()
				.getText().toString());
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
		if (mWriteMode
				&& NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
			Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

			WifiModel selectedWifi = new WifiModel(networkSpinner
					.getSelectedItem().toString(), passwordField.getText()
					.toString(), protocolSpinner.getSelectedItem().toString());
			if (WifiUtil.isValidWifiModel(selectedWifi)) {
				NdefMessage wifiNdefMessage = NfcUtil.getWifiAsNdef(
						selectedWifi.getSSID(), selectedWifi.getPassword(),
						selectedWifi.getProtocol());
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

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.nfc, menu);

//		ShareActionProvider mShareActionProvider = (ShareActionProvider) menu
//				.findItem(R.id.share).getActionProvider();
//		Intent picIntent = new Intent(android.content.Intent.ACTION_SEND);
//		picIntent.setType("image/*");
//		mShareActionProvider.setShareIntent(picIntent);

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.add:
			buildDialog().show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private View.OnClickListener mTagWriter = new View.OnClickListener() {
		@Override
		public void onClick(View arg0) {
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
				protocolSpinner.setSelection(WifiUtil.protocols
						.indexOf(selectedNetwork.getProtocol()));
				passwordField.setText(Util.stripQuotes(selectedNetwork
						.getPassword()));
			}
			break;
		case R.id.protocol_spinner:
			if (protocolSpinner.getSelectedItem().toString()
					.equals(WifiUtil.OPEN)) {
				passwordField.setText("");
				passwordField.setEnabled(false);
				revealPassword.setEnabled(false);
			} else {
				passwordField.setEnabled(true);
				revealPassword.setEnabled(true);
			}
			break;
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
	}

	private Dialog buildDialog() {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		LayoutInflater inflator = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
		View layout = inflator.inflate(R.layout.add_dialog,
				(ViewGroup) findViewById(R.id.dialog_root));
		builder.setView(layout);

		final EditText newSsidField = (EditText) layout
				.findViewById(R.id.add_network_ssid);
		final EditText newPasswordField = (EditText) layout
				.findViewById(R.id.add_network_password);
		final Spinner newProtocolSpinner = (Spinner) layout
				.findViewById(R.id.add_protocol_spinner);

		ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(this,
				android.R.layout.simple_spinner_item, WifiUtil.protocols);
		protocolAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		newProtocolSpinner.setAdapter(protocolAdapter);
		newProtocolSpinner
				.setOnItemSelectedListener(new OnItemSelectedListener() {

					@Override
					public void onItemSelected(AdapterView<?> parent,
							View view, int pos, long id) {
						switch (parent.getId()) {
						case R.id.add_protocol_spinner:
							if (newProtocolSpinner.getSelectedItem().toString()
									.equals(WifiUtil.OPEN)) {
								newPasswordField.setText("");
								newPasswordField.setEnabled(false);
							} else {
								newPasswordField.setEnabled(true);
							}
							break;
						}
					}

					@Override
					public void onNothingSelected(AdapterView<?> arg0) {
					}
				});

		builder.setTitle(getString(R.string.add_new_network));

		builder.setPositiveButton(getString(R.string.add_button),
				new OnClickListener() {

					@Override
					public void onClick(DialogInterface dialog, int which) {
						WifiModel newWifiModel = new WifiModel(newSsidField
								.getText().toString(), newPasswordField
								.getText().toString(), newProtocolSpinner
								.getSelectedItem().toString());

						WifiManager mWm = (WifiManager) getSystemService(Context.WIFI_SERVICE);

						int netId = WifiUtil.addWifiNetwork(
								getApplicationContext(), newWifiModel, mWm);
						if (netId != -1) {
							// TODO: update network spinner on QR
							String[] updatedNetworks = WifiUtil
									.getConfiguredNetworks(getApplicationContext());
							networkAdapter = new SpinnerArrayAdapter<String>(
									getApplication(), updatedNetworks);
							networkAdapter
									.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
							networkSpinner.setAdapter(networkAdapter);

							// set spinner to the network just added
							networkSpinner
									.setSelection(updatedNetworks.length - 1);

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

		builder.setNegativeButton(getString(R.string.cancel), null);

		return builder.create();
	}
}
