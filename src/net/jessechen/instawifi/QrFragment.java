package net.jessechen.instawifi;

import java.io.File;
import java.io.FileOutputStream;

import net.jessechen.instawifi.misc.PasswordEditText;
import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.QrUtil;
import net.jessechen.instawifi.util.QrUtil.QrImageSize;
import net.jessechen.instawifi.util.RootUtil.PasswordNotFoundException;
import net.jessechen.instawifi.util.Util;
import net.jessechen.instawifi.util.WifiUtil;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.DisplayMetrics;
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
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.gridlayout.GridLayout;

public class QrFragment extends SherlockFragment implements
		OnItemSelectedListener {
	private static final String TAG = QrFragment.class.getSimpleName();

	GridLayout gridlayout_qr;
	Button qwriteTag_qr;
	static Spinner networkSpinner_qr;
	static Spinner protocolSpinner_qr;
	TextView passwordText_qr;
	static PasswordEditText passwordField_qr;
	CheckBox revealPassword_qr;
	static ImageView qrImage;

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

		qrImage = (ImageView) view.findViewById(R.id.qr_code_image);
		gridlayout_qr = (GridLayout) view.findViewById(R.id.gridlayout_qr);
		networkSpinner_qr = (Spinner) view
				.findViewById(R.id.network_spinner_qr);
		protocolSpinner_qr = (Spinner) view
				.findViewById(R.id.protocol_spinner_qr);
		passwordText_qr = (TextView) view.findViewById(R.id.password_text_qr);
		passwordField_qr = (PasswordEditText) view
				.findViewById(R.id.password_field_qr);
		passwordField_qr.init(getActivity(), qrImage);
		revealPassword_qr = (CheckBox) view
				.findViewById(R.id.password_checkbox_qr);

		revealPassword_qr.setOnCheckedChangeListener(mCheckBoxListener);

		// test point to see if device can get configured networks w/o wifi
		final String[] test = WifiUtil.getConfiguredNetworks(getActivity());
		if (test.length == 0) {
			WifiUtil.showWifiDialog(getActivity(),
					getString(R.string.show_wifi_msg_default),
					new WifiUtil.EnableWifiTaskListener() {

						@Override
						public void OnWifiEnabled() {
							setupQrView();
						}
					});
		} else {
			setupQrView();
		}

		return view;
	}

	private void setupQrView() {
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
				WifiUtil.protocolStrings);
		protocolAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		protocolSpinner_qr.setAdapter(protocolAdapter);
		protocolSpinner_qr.setOnItemSelectedListener(this);
		protocolSpinner_qr.setSelection(WifiUtil.DEFAULT_PROTOCOL);

		setQrImage(getActivity());
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
			shareQrImage();
			break;
		case R.id.add:
			// buildDialog().show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	public static void setQrImage(Activity a) {
		QrImageSize size;

		DisplayMetrics metrics = new DisplayMetrics();
		a.getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		switch (metrics.densityDpi) {
		case DisplayMetrics.DENSITY_LOW:
			size = QrImageSize.SMALL;
			break;
		case DisplayMetrics.DENSITY_MEDIUM:
			size = QrImageSize.SMALL;
			break;
		case DisplayMetrics.DENSITY_HIGH:
			size = QrImageSize.SMALL;
			break;
		case DisplayMetrics.DENSITY_XHIGH:
			size = QrImageSize.LARGE;
			break;
		default:
			size = QrImageSize.SMALL;
		}

		Bitmap bmp = getSelectedWifiBitmap(size);
		if (bmp != null) {
			// remove default placeholder
			qrImage.setBackgroundDrawable(null);
			// set qr code
			qrImage.setImageBitmap(bmp);
		}
	}

	private static Bitmap getSelectedWifiBitmap(QrImageSize size) {
		// spinner adapters might be null, which throws a npe
		try {
			String ssid = networkSpinner_qr.getSelectedItem().toString();
			String pw = passwordField_qr.getText().toString();
			int protocol = protocolSpinner_qr.getSelectedItemPosition();

			WifiModel selectedWifi = new WifiModel(ssid, pw, protocol);

			return QrUtil.generateQrCode(selectedWifi, size);
		} catch (Exception e) {
			Log.e(TAG,
					"spinner adapters are null, wifi is probably not enabled");
		}
		return null;
	}

	private void shareQrImage() {
		String selectedSsid = "";
		Bitmap bitmap = null;
		try {
			// spinner might be empty
			selectedSsid = networkSpinner_qr.getSelectedItem().toString();
			bitmap = getSelectedWifiBitmap(QrImageSize.EMAIL);
		} catch (Exception e) {
			// do nothing
		}

		File file = null;
		String filename = "";
		try {
			filename = QrUtil.getQrFilename(selectedSsid);
			file = new File(getActivity().getExternalFilesDir(null), filename);

			FileOutputStream fos = new FileOutputStream(file);
			bitmap.compress(CompressFormat.JPEG, 100, fos);

			fos.flush();
			fos.close();

			Intent picIntent = Util.buildQrShareIntent(getActivity(), file,
					selectedSsid);
			startActivity(Intent.createChooser(picIntent, String.format(
					getString(R.string.qr_share_dialog_title), selectedSsid)));
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, getString(R.string.qr_share_fail));
			Util.shortToast(getActivity(), getString(R.string.qr_share_fail));
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
				protocolSpinner_qr.setSelection(selectedNetwork.getProtocol());
				passwordField_qr.setText(Util.stripQuotes(selectedNetwork
						.getPassword()));
			}
			break;
		case R.id.protocol_spinner_qr:
			if (protocolSpinner_qr.getSelectedItemPosition() == WifiUtil.NONE) {
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

		setQrImage(getActivity());
	}

	@Override
	public void onNothingSelected(AdapterView<?> parent) {
	}

	// update network spinner after add network dialog
	public void updateNetworkSpinner(ArrayAdapter<String> networkAdapter) {
		if (networkSpinner_qr != null) {
			networkSpinner_qr.setAdapter(networkAdapter);
			networkSpinner_qr.setSelection(networkAdapter.getCount() - 1);
		}
	}
}
