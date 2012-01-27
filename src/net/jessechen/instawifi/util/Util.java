package net.jessechen.instawifi.util;


import android.app.ActionBar;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.widget.Toast;

public class Util {
	@SuppressWarnings("unused")
	private static final String TAG = Util.class.getName();

	public static void NfcActionBar(FragmentActivity fragActivity) {
		android.support.v4.app.ActionBar bar = fragActivity.getSupportActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
		
		bar.addTab(bar.newTab().setText("NFC"));
		bar.addTab(bar.newTab().setText("QR"));
		bar.setSelectedNavigationItem(0);
	}
	
	public static void shortToast(Context c, String msg) {
		Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
	}

	public static void longToast(Context c, String msg) {
		Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
	}

	public static String concatQuotes(String s) {
		return "\"".concat(s).concat("\"");
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
}
