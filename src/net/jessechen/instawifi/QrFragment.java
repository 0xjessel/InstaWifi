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
				.findViewById(R.id.security_spinner_qr);
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
		
		qrImage = (ImageView) view.findViewById(R.id.qr_code_image);
		qrImage.setImageBitmap(getSelectedWifiBitmap(view));

		return view;
	}

	private Bitmap getSelectedWifiBitmap(View view) {
		WifiModel selectedWifi = new WifiModel(networkSpinner_qr
				.getSelectedItem().toString(), protocolSpinner_qr
				.getSelectedItem().toString(), passwordField_qr.getText()
				.toString());

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
			qrImage.setImageBitmap(getSelectedWifiBitmap(view));
		}
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub

	}
}
