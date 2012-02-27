package net.jessechen.instawifi.util;

import java.io.File;

import net.jessechen.instawifi.R;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class Util {
	@SuppressWarnings("unused")
	private static final String TAG = Util.class.getSimpleName();

	public static void shortToast(Context c, String msg) {
		Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
	}

	public static void longToast(Context c, String msg) {
		Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
	}

	public static boolean hasNfc(NfcAdapter adapter) {
		if (adapter != null && adapter.isEnabled()) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean hasQuotes(String s) {
		return (s != null) ? (s.startsWith("\"") && s.endsWith("\"")) : false;
	}

	public static String concatQuotes(String s) {
		return (s != null) ? "\"".concat(s).concat("\"") : "";
	}

	public static String stripQuotes(String s) {
		return (s != null) ? s.replaceAll("^\"|\"$", "") : "";
	}

	public static boolean isHexString(String s) {
		if (s == null) {
			return false;
		}
		int len = s.length();
		if (len != 10 && len != 26 && len != 58) {
			return false;
		}
		for (int i = 0; i < len; ++i) {
			char c = s.charAt(i);
			if ((c >= '0' && c <= '9') || (c >= 'a' && c <= 'f')
					|| (c >= 'A' && c <= 'F')) {
				continue;
			}
			return false;
		}
		return true;
	}

	public static void initNetworkSpinner(Context c, Spinner spinner,
			OnItemSelectedListener listener) {
		String[] networks = WifiUtil.getConfiguredNetworks(c);
		ArrayAdapter<String> networkAdapter = new ArrayAdapter<String>(c,
				android.R.layout.simple_spinner_item, networks);
		networkAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(networkAdapter);
		spinner.setOnItemSelectedListener(listener);
	}

	public static void initProtocolSpinner(Context c, Spinner spinner) {
		ArrayAdapter<String> protocolAdapter = new ArrayAdapter<String>(c,
				android.R.layout.simple_spinner_item, WifiUtil.protocolStrings);
		protocolAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(protocolAdapter);
	}

	public static Intent buildQrShareIntent(Context c, File file, String ssid) {
		Intent picIntent = new Intent(Intent.ACTION_SEND);
		picIntent.setType("image/*");
		Uri uri = Uri.fromFile(file);
		picIntent.putExtra(Intent.EXTRA_STREAM, uri);

		picIntent.putExtra(Intent.EXTRA_SUBJECT,
				String.format(c.getString(R.string.qr_share_subject), ssid));
		picIntent.putExtra(Intent.EXTRA_TEXT,
				String.format(c.getString(R.string.qr_share_text), ssid));

		return picIntent;
	}
}
