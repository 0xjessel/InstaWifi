package net.jessechen.instawifi.asynctask;

import net.jessechen.instawifi.util.WifiUtil;
import net.jessechen.instawifi.util.WifiUtil.EnableWifiTaskListener;
import android.app.ProgressDialog;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.util.Log;

// use this for front-facing enabling wifi tasks
public class EnableWifiTask extends AsyncTask<Void, Void, Void> {
	private static final String TAG = EnableWifiTask.class.getSimpleName();

	Context c;
	ProgressDialog pd;
	EnableWifiTaskListener listener;

	public EnableWifiTask(Context c, EnableWifiTaskListener listener) {
		Log.e(TAG, "constructing enablewifitask");

		this.c = c;
		this.listener = listener;
	}

	@Override
	protected Void doInBackground(Void... params) {
		WifiManager mWm = (WifiManager) c
				.getSystemService(Context.WIFI_SERVICE);

		if (!WifiUtil.enableWifiAndWait(mWm)) {
			Log.e(TAG,
					"enabling wifi timed out in WifiUtil.EnableWifiTask.doInBackground");
		}

		return null;
	}

	@Override
	protected void onPreExecute() {
		pd = ProgressDialog.show(c, "WiFi", "Turning on..", true);
	}

	@Override
	protected void onPostExecute(Void result) {
		try {
			pd.dismiss();
			pd = null;
		} catch (Exception e) {
			// do nothing
		}

		if (listener != null) {
			listener.OnWifiEnabled();
		}
	}
}
