package net.jessechen.instawifi.asynctask;

import net.jessechen.instawifi.models.WifiModel;
import net.jessechen.instawifi.util.WifiUtil;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Spinner;

public class GetCurrentWifiModel extends AsyncTask<Context, Void, WifiModel> {
	private static final String TAG = GetCurrentWifiModel.class.getSimpleName();

	private String[] networks;
	private Spinner networkSpinner;

	public GetCurrentWifiModel(String[] networks, Spinner networkSpinner) {
		Log.e(TAG, "constructing getcurrentwifimodel");

		this.networks = networks;
		this.networkSpinner = networkSpinner;
	}

	@Override
	protected WifiModel doInBackground(Context... params) {
		Context c = params[0];
		WifiConfiguration wc = WifiUtil.getCurrentWifiConfig(c);
		if (wc != null) {
			String SSID = wc.SSID;
			int protocol = WifiUtil.getWifiProtocol(wc);
			protocol = (protocol == -1) ? 1 : protocol;
			String password = WifiUtil.fetchWifiPasswordNow(c, SSID);
			return new WifiModel(SSID, password, protocol);
		} else {
			return null;
		}
	}

	@Override
	protected void onPostExecute(WifiModel curWifi) {
		if (curWifi != null) {
			for (int i = 0; i < networks.length; i++) {
				if (curWifi.getTrimmedSSID().equals(networks[i])) {
					networkSpinner.setSelection(i);
				}
			}
		}
	}
}
