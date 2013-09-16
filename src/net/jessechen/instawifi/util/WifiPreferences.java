package net.jessechen.instawifi.util;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;

/**
 * singleton object to fetch wifi password from local preferences
 */
public class WifiPreferences {
	private static final String prefs_name = "WIFI_PW";
	private static SharedPreferences ssid_pw_map = null;

	/**
	 * save the SSID => Password in preferences
	 * 
	 * Writes it asynchronously if device version is greater than Froyo,
	 * otherwise launch an AsyncTask that does a synchronous write
	 */
	@SuppressLint("NewApi")
	public static void saveWifiPassword(Context c, String SSID, String pw) {
		if (ssid_pw_map == null) {
			ssid_pw_map = c.getSharedPreferences(prefs_name, 0);
		}

		if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.FROYO) {
			SharedPreferences.Editor editor = ssid_pw_map.edit();
			editor.putString(SSID, pw);
			editor.apply();
		} else {
			Util.startMyTask(new SaveWifiPasswordTask(), SSID, pw);
		}
	}

	// gets the wifi password from preferences given a SSID
	public static String getWifiPassword(Context c, String SSID) {
		if (ssid_pw_map == null) {
			ssid_pw_map = c.getSharedPreferences(prefs_name, 0);
		}
		return ssid_pw_map.getString(SSID, null);
	}

	/**
	 * this uses editor.commit() which is synchronous, use this for Android 2.2
	 * and lower
	 * 
	 * it assumes ssid_pw_map is already instantiated
	 */
	private static class SaveWifiPasswordTask extends
			AsyncTask<String, Void, Void> {
		@Override
		protected Void doInBackground(String... params) {
			String SSID = params[0];
			String pw = params[1];

			SharedPreferences.Editor editor = ssid_pw_map.edit();
			editor.putString(SSID, pw);
			editor.commit();

			return null;
		}
	}
}
