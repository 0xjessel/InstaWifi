package net.jessechen.instawifi;

import net.jessechen.instawifi.util.Util;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiReceiver extends BroadcastReceiver {
	Context ctx;
	Activity parent;
	WifiManager mWifiManager;
	boolean triedAssociating;

	public WifiReceiver(Context c, Activity wifiHandler, WifiManager wifiManager) {
		this.ctx = c;
		this.parent = wifiHandler;
		this.mWifiManager = wifiManager;
		this.triedAssociating = false;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.d(Util.TAG,
				"wifi state: "
						+ intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
		if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
			handleConnectionChanged((SupplicantState) intent
					.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
		}
	}

	private void handleConnectionChanged(SupplicantState state) {
		if (SupplicantState.COMPLETED.equals(state) && triedAssociating) {
			// wifi success
			String ssid = mWifiManager.getConnectionInfo().getSSID();
			
			Log.i(Util.TAG, String.format("wifi connection completed on %s", ssid));
			Util.shortToast(ctx, String.format(
					ctx.getString(R.string.wifi_connect_success), ssid));
			Log.i(Util.TAG, "finishing activity bye");
			
			parent.finish();
		} else if (SupplicantState.SCANNING.equals(state)) {
			triedAssociating = true;
			Log.i(Util.TAG, "scanning for a network");
			// TODO: probably needs a timer to check if wifi state is still
			// scanning/disconnected to determine if it failed
		} else if (SupplicantState.DISCONNECTED.equals(state)
				&& triedAssociating) {
			triedAssociating = false;
			
			Log.e(Util.TAG, "wifi connection failed");
			Util.shortToast(ctx, ctx.getString(R.string.wifi_connect_fail));
		}
	}

}
