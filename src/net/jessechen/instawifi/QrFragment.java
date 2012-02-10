package net.jessechen.instawifi;

import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.RootUtil.PasswordNotFoundException;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;

/*
 * TODO: update qrImage when changing password field
 */
public class QrFragment extends Fragment implements OnItemSelectedListener {
	private static final String TAG = QrFragment.class.getSimpleName();

	Button qwriteTag_qr;
	Spinner networkSpinner_qr;
	Spinner protocolSpinner_qr;
	EditText passwordField_qr;
	CheckBox revealPassword_qr;
	ImageView qrImage;

	public static QrFragment getInstance() {
		QrFragment fragment = new QrFragment();

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.qr_activity, container, false);

		networkSpinner_qr = (Spinner) view
				.findViewById(R.id.network_spinner_qr);
		protocolSpinner_qr = (Spinner) view
				.findViewById(R.id.protocol_spinner_qr);
		passwordField_qr = (EditText) view.findViewById(R.id.password_field_qr);
		revealPassword_qr = (CheckBox) view
				.findViewById(R.id.password_checkbox_qr);

		revealPassword_qr.setOnCheckedChangeListener(mCheckBoxListener);

		String[] networks = WifiUtil.getConfiguredNetworks(getActivity());
		ArrayAdapter<String> networkAdapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_spinner_item, networks);
		networkAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		networkSpinner_qr.setAdapter(networkAdapter);
		networkSpinner_qr.setOnItemSelectedListener(this);

		ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(
				getActivity(), android.R.layout.simple_spinner_item,
				WifiUtil.protocols);
		protocolAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		protocolSpinner_qr.setAdapter(protocolAdapter);
		protocolSpinner_qr.setOnItemSelectedListener(this);

		qrImage = (ImageView) view.findViewById(R.id.qr_code_image);
		qrImage.setImageBitmap(getSelectedWifiBitmap());

		return view;
	}

	private Bitmap getSelectedWifiBitmap() {
		WifiModel selectedWifi = new WifiModel(networkSpinner_qr
				.getSelectedItem().toString(), passwordField_qr.getText()
				.toString(), protocolSpinner_qr.getSelectedItem().toString());

		return WifiUtil.generateQrImage(selectedWifi.getSSID(),
				selectedWifi.getProtocol(), selectedWifi.getPassword());
	}

	private OnCheckedChangeListener mCheckBoxListener = new CompoundButton.OnCheckedChangeListener() {

		@Override
		public void onCheckedChanged(CompoundButton buttonView,
				boolean isChecked) {
			if (isChecked) {
				passwordField_qr.setTransformationMethod(null);
			} else {
				passwordField_qr
						.setTransformationMethod(new PasswordTransformationMethod());
			}
		}
	};

	@Override
	public void onItemSelected(AdapterView<?> parent, View view, int pos,
			long id) {
		switch (parent.getId()) {
		case R.id.network_spinner_qr:
			WifiModel selectedNetwork = null;
			try {
				selectedNetwork = WifiUtil.getWifiModelFromSsid(getActivity(),
						parent.getItemAtPosition(pos).toString());
			} catch (PasswordNotFoundException e) {
				e.printStackTrace();
				Log.e(TAG, "did not find password on item selected");
			}

			if (selectedNetwork != null) {
				protocolSpinner_qr.setSelection(WifiUtil.protocols
						.indexOf(selectedNetwork.getProtocol()));
				passwordField_qr.setText(Util.stripQuotes(selectedNetwork
						.getPassword()));
			}
			break;
		case R.id.protocol_spinner_qr:
			if (protocolSpinner_qr.getSelectedItem().toString()
					.equals(WifiUtil.OPEN)) {
				passwordField_qr.setText("");
				passwordField_qr.setEnabled(false);
				revealPassword_qr.setEnabled(false);
			} else {
				passwordField_qr.setEnabled(true);
				revealPassword_qr.setEnabled(true);
			}
			break;
		}
		
		qrImage.setImageBitmap(getSelectedWifiBitmap());
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub

	}
}
