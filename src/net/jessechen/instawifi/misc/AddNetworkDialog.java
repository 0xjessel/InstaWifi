package net.jessechen.instawifi.misc;

import net.jessechen.instawifi.QrFragment;
import net.jessechen.instawifi.R;
import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.net.wifi.WifiManager;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
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

public class AddNetworkDialog {

	// add network dialog
	public static void show(final FragmentActivity a,
			final Context c, final Spinner networkSpinner) {
		AlertDialog.Builder builder = new AlertDialog.Builder(a);
		builder.setInverseBackgroundForced(true);
		
		LayoutInflater inflator = (LayoutInflater) a
				.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		View layout = inflator.inflate(R.layout.add_dialog,
				(ViewGroup) a.findViewById(R.id.dialog_root));
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

		ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(a,
				android.R.layout.simple_spinner_item, WifiUtil.protocolStrings);
		protocolAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		newProtocolSpinner.setAdapter(protocolAdapter);

		builder.setTitle(a.getString(R.string.add_new_network));

		builder.setPositiveButton(R.string.add_button, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				WifiModel newWifiModel = new WifiModel(newSsidField.getText()
						.toString(), newPasswordField.getText().toString(),
						newProtocolSpinner.getSelectedItemPosition());

				WifiManager mWm = (WifiManager) a
						.getSystemService(Context.WIFI_SERVICE);

				int netId = WifiUtil.addWifiNetwork(a, newWifiModel, mWm);
				if (netId != -1) {
					String[] updatedNetworks = WifiUtil
							.getConfiguredNetworks(a);
					SpinnerArrayAdapter<String> networkAdapter = new SpinnerArrayAdapter<String>(
							a, updatedNetworks);
					networkAdapter
							.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

					// update NfcActivity's spinner iff device does not have NFC
					if (Util.hasNfc(c)) {
						networkSpinner.setAdapter(networkAdapter);

						// set spinner to the network just added
						networkSpinner.setSelection(networkAdapter.getCount() - 1);
					}

					QrFragment qrFrag = (QrFragment) a
							.getSupportFragmentManager().findFragmentById(
									R.id.fragment);
					if (qrFrag != null) {
						qrFrag.updateNetworkSpinner(networkAdapter);
					}

					Util.shortToast(a, a.getString(R.string.success));
				} else {
					Util.shortToast(a,
							a.getString(R.string.add_new_network_fail));
				}

				// hide keyboard after closing dialog
				InputMethodManager inputManager = (InputMethodManager) a
						.getSystemService(Context.INPUT_METHOD_SERVICE);
				inputManager.hideSoftInputFromWindow(
						newSsidField.getWindowToken(),
						InputMethodManager.HIDE_NOT_ALWAYS);
			}
		});

		builder.setNegativeButton(R.string.cancel, new OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				// hide keyboard after closing dialog
				InputMethodManager inputManager = (InputMethodManager) a
						.getSystemService(Context.INPUT_METHOD_SERVICE);
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

	private static void onEditTextChanged(EditText mSsidField,
			EditText mPwField, Spinner mProtocolSpinner, Button mAddButton) {
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

	private static class EditTextWatcher implements TextWatcher {
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
