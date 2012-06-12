package net.jessechen.instawifi.misc;

import net.jessechen.instawifi.R;
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
	
	private static final String TAG = WifiReceiver.class.getSimpleName();

	public WifiReceiver(Context c, Activity wifiHandler, WifiManager wifiManager) {
		this.ctx = c;
		this.parent = wifiHandler;
		this.mWifiManager = wifiManager;
		this.triedAssociating = false;
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.d(TAG,
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
			
			Log.i(TAG, String.format("wifi connection completed on %s", ssid));
			Util.shortToast(ctx, String.format(
					ctx.getString(R.string.wifi_connect_success), ssid));
			Log.i(TAG, "finishing activity bye");
			
			parent.finish();
		} else if (SupplicantState.SCANNING.equals(state)) {
			Log.i(TAG, "scanning for a network");
		} else if (SupplicantState.ASSOCIATING.equals(state)) {
			Log.i(TAG, "associating to network");
			triedAssociating = true;
		} else if (SupplicantState.DISCONNECTED.equals(state)
				&& triedAssociating) {
			triedAssociating = false;
			
			Log.e(TAG, "wifi connection failed");
			Util.shortToast(ctx, ctx.getString(R.string.wifi_connect_fail));
		}
	}

}
