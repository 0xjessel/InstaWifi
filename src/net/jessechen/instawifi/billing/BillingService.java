package net.jessechen.instawifi.billing;

import com.android.vending.billing.IMarketBillingService;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class BillingService extends Service implements ServiceConnection {
	private static final String TAG = BillingService.class.getSimpleName();
	private IMarketBillingService mService;

	@Override
	public void onCreate() {
		try {
			boolean bindResult = bindService(new Intent(
					Consts.MARKET_BILLING_SERVICE_ACTION), this,
					Context.BIND_AUTO_CREATE);
			if (bindResult) {
				Log.i(TAG, "Service bind success");
			} else {
				Log.e(TAG, "could not bind to MarketBillingService");
			}
		} catch (SecurityException e) {
			Log.e(TAG, "Security exception: " + e);
		} 
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
		Log.i(TAG, "MarketBillingService connected");
		mService = IMarketBillingService.Stub.asInterface(service);
	}

	@Override
	public void onServiceDisconnected(ComponentName name) {
		mService = null;
	}

	protected Bundle makeRequestBundle(String method) {
		Bundle request = new Bundle();
		request.putString(Consts.BILLING_REQUEST_METHOD, method);
		request.putInt(Consts.BILLING_REQUEST_API_VERSION, 1);
		request.putString(Consts.BILLING_REQUEST_PACKAGE_NAME, getPackageName());
		return request;
	}

}
