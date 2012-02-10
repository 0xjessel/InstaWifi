package net.jessechen.instawifi.util;

import net.jessechen.instawifi.QrFragment;
import net.jessechen.instawifi.R;
import android.content.Context;
import android.content.Intent;
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
	
	public static boolean hasQuotes(String s) {
		if (s.startsWith("\"") && s.endsWith("\"")) {
			return true;
		} else {
			return false;
		}
	}
	
	public static String concatQuotes(String s) {
		return "\"".concat(s).concat("\"");
	}

	public static String stripQuotes(String s) {
		return s.replaceAll("^\"|\"$", "");
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
	
	public static void initNetworkSpinner(Context c, Spinner spinner, OnItemSelectedListener listener) {
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
				android.R.layout.simple_spinner_item, WifiUtil.protocols);
		protocolAdapter
				.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		spinner.setAdapter(protocolAdapter);
	}

	public static class TabListener implements
			android.support.v4.app.ActionBar.TabListener {
		private String tag;
		private Context a;

		public TabListener(Context a, String tag) {
			this.tag = tag;
			this.a = a;
		}

		@Override
		public void onTabReselected(android.support.v4.app.ActionBar.Tab tab,
				android.support.v4.app.FragmentTransaction ft) {
			// TODO Auto-generated method stub

		}

		@Override
		public void onTabSelected(android.support.v4.app.ActionBar.Tab tab,
				android.support.v4.app.FragmentTransaction ft) {
			Util.shortToast(a, tag + " selected!");
			if (tag.equals(a.getString(R.string.qr_tab))) {
				a.startActivity(new Intent(a, QrFragment.class));
			}
		}

		@Override
		public void onTabUnselected(android.support.v4.app.ActionBar.Tab tab,
				android.support.v4.app.FragmentTransaction ft) {
			// TODO Auto-generated method stub

		}
	}

}
