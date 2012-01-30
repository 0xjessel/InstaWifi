package net.jessechen.instawifi.util;

import net.jessechen.instawifi.QrActivity;
import net.jessechen.instawifi.R;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class Util {
	@SuppressWarnings("unused")
	private static final String TAG = Util.class.getName();

	public static void shortToast(Context c, String msg) {
		Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
	}

	public static void longToast(Context c, String msg) {
		Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
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
				a.startActivity(new Intent(a, QrActivity.class));
			}
		}

		@Override
		public void onTabUnselected(android.support.v4.app.ActionBar.Tab tab,
				android.support.v4.app.FragmentTransaction ft) {
			// TODO Auto-generated method stub

		}
	}

}
