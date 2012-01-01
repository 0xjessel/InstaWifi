package net.jessechen.instawifi.util;


import android.content.Context;
import android.widget.Toast;

public class Util {
	public static Toast shortToast(Context c, String msg) {
		return Toast.makeText(c, msg, Toast.LENGTH_SHORT);
	}

	public static Toast longToast(Context c, String msg) {
		return Toast.makeText(c, msg, Toast.LENGTH_LONG);
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

	public static String TAG = "instawifi";
}
