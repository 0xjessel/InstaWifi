package net.jessechen.instawifi.util;

import java.io.File;

import net.jessechen.instawifi.R;
import net.jessechen.instawifi.util.BillingUtil.DonateOption;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.os.AsyncTask;
import android.os.Build;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

public class Util {
	public static final String NFC = "NFC";
	public static final String QR = "QR";

	public static String curTab = NFC;

	@SuppressWarnings("unused")
	private static final String TAG = Util.class.getSimpleName();

	public static void shortToast(Context c, String msg) {
		Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
	}

	public static void longToast(Context c, String msg) {
		Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
	}

	@TargetApi(Build.VERSION_CODES.GINGERBREAD)
	public static boolean hasNfc(Context c) {
		return c.getPackageManager().hasSystemFeature(
				PackageManager.FEATURE_NFC);
	}

	@TargetApi(10)
	public static boolean isNfcEnabled(Context c) {
		NfcManager manager = (NfcManager) c
				.getSystemService(Context.NFC_SERVICE);
		NfcAdapter adapter = manager.getDefaultAdapter();
		if (adapter != null && adapter.isEnabled()) {
			return true;
		}
		return false;
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

	public static boolean isNfcTabSelected() {
		return Util.NFC.equals(Util.curTab);
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

	public static Intent buildAppShareIntent(Context c) {
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT,
				c.getString(R.string.app_share_subject));
		intent.putExtra(Intent.EXTRA_TEXT, c.getString(R.string.app_share_text));
		return intent;
	}

	public static Intent buildDonateEmailIntent(Context c, String orderId,
			DonateOption donateOption) {
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		if (donateOption == null || donateOption.numNfcStickers == 0) {
			return null;
		}

		String subject;
		if (donateOption.numNfcStickers > 1) {
			subject = String.format(
					c.getString(R.string.donate_email_subject_multi), orderId);
		} else {
			subject = String.format(
					c.getString(R.string.donate_email_subject_one), orderId);
		}

		String message = String.format(c.getString(R.string.donate_email_body),
				donateOption.amount, donateOption.numNfcStickers);

		String uriText = String.format(
				c.getString(R.string.donate_email_template), subject, message);
		uriText.replace(" ", "%20");
		Uri uri = Uri.parse(uriText);
		intent.setData(uri);
		return intent;
	}

	/**
	 * Wrapper function to run in parallel when device > Honeycomb, otherwise
	 * run serially
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	public static <T> void startMyTask(AsyncTask<T, ?, ?> asyncTask,
			T... params) {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
			asyncTask.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, params);
		else
			asyncTask.execute(params);
	}
}
