package net.jessechen.instawifi.util;

import android.content.Context;
import android.widget.Toast;

public class Util {
	public static String TAG = "instawifi";
	
	public static Toast shortToast(Context c, String msg) {
		return Toast.makeText(c, msg, Toast.LENGTH_SHORT);
	}
	
	public static Toast longToast(Context c, String msg) {
		return Toast.makeText(c, msg, Toast.LENGTH_LONG);
	}
}
