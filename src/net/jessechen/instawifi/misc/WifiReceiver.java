package net.jessechen.instawifi.misc;

import net.jessechen.instawifi.InstaWifiHandler;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.SupplicantState;
import android.net.wifi.WifiManager;
import android.util.Log;

public class WifiReceiver extends BroadcastReceiver {
	
	private static final String TAG = WifiReceiver.class.getSimpleName();

	public WifiReceiver() {
		super();
	}

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		Log.d(TAG,
				"wifi state: "
						+ intent.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
		if (WifiManager.SUPPLICANT_STATE_CHANGED_ACTION.equals(action)) {
			InstaWifiHandler.handleConnectionChanged((SupplicantState) intent
					.getParcelableExtra(WifiManager.EXTRA_NEW_STATE));
		}
	}
}
