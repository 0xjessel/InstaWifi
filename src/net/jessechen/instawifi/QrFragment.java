package net.jessechen.instawifi;

import java.io.FileOutputStream;

import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.RootUtil.PasswordNotFoundException;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.MenuItem;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

/*
 * TODO: update qrImage when changing password field
 */
public class QrFragment extends Fragment implements OnItemSelectedListener {
	private static final String TAG = QrFragment.class.getSimpleName();

	Button qwriteTag_qr;
	Spinner networkSpinner_qr;
	Spinner protocolSpinner_qr;
	TextView passwordText_qr;
	EditText passwordField_qr;
	CheckBox revealPassword_qr;
	ImageView qrImage;

	Intent picIntent;

	public static QrFragment getInstance() {
		QrFragment fragment = new QrFragment();

		return fragment;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.qr_activity, container, false);

		picIntent = new Intent(android.content.Intent.ACTION_SEND);
		picIntent.setType("image/*");

		networkSpinner_qr = (Spinner) view
				.findViewById(R.id.network_spinner_qr);
		protocolSpinner_qr = (Spinner) view
				.findViewById(R.id.protocol_spinner_qr);
		passwordText_qr = (TextView) view.findViewById(R.id.password_text_qr);
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

		// set spinner to current wifi config if connected to wifi
		WifiModel curWifi = WifiUtil.getCurrentWifiModel(getActivity()
				.getApplicationContext());
		if (curWifi != null) {
			for (int i = 0; i < networks.length; i++) {
				if (curWifi.getTrimmedSSID().equals(networks[i])) {
					networkSpinner_qr.setSelection(i);
				}
			}
		}

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
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.share:
			if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
				qrImage.setDrawingCacheEnabled(true);
				Bitmap bitmap = qrImage.getDrawingCache();
				String filename = "";
				try {
					filename = getQrFilename(networkSpinner_qr
							.getSelectedItem().toString());
					FileOutputStream fos = getActivity().openFileOutput(
							filename, Context.MODE_WORLD_READABLE);
					bitmap.compress(CompressFormat.JPEG, 100, fos);
					fos.close();
				} catch (Exception e) {
					e.printStackTrace();
					Log.e(TAG, "failed to generate qr image file");
				}

				Uri photoUri = Uri
						.parse(getActivity().getFilesDir() + filename);
				picIntent.setData(photoUri);
				picIntent.putExtra(Intent.EXTRA_STREAM, photoUri);

				startActivity(Intent.createChooser(picIntent,
						"Share QR Image via"));
			}
			break;
		case R.id.add:
			// buildDialog().show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	private String getQrFilename(String ssid) {
		if (ssid != null) {
			return "instawifi_" + ssid + "qr.jpg";
		} else {
			throw new NullPointerException("null ssid");
		}
	}

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
				passwordText_qr.setVisibility(View.GONE);
				passwordField_qr.setVisibility(View.GONE);
				revealPassword_qr.setVisibility(View.GONE);
			} else {
				passwordText_qr.setVisibility(View.VISIBLE);
				passwordField_qr.setVisibility(View.VISIBLE);
				revealPassword_qr.setVisibility(View.VISIBLE);
			}
			break;
		}

		qrImage.setImageBitmap(getSelectedWifiBitmap());
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
		// TODO Auto-generated method stub

	}

	// update network spinner after add network dialog
	public void updateNetworkSpinner(ArrayAdapter<String> networkAdapter) {
		if (networkSpinner_qr != null) {
			networkSpinner_qr.setAdapter(networkAdapter);
			networkSpinner_qr.setSelection(networkAdapter.getCount() - 1);
		}
	}
}
