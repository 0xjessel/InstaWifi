package net.jessechen.instawifi.billing;

import net.jessechen.instawifi.util.BillingUtil;
import net.jessechen.instawifi.util.BillingUtil.ResponseCode;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class BillingReceiver extends BroadcastReceiver {
	private static final String TAG = BillingReceiver.class.getSimpleName();

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (BillingUtil.ACTION_PURCHASE_STATE_CHANGED.equals(action)) {
			String signedData = intent.getStringExtra(BillingUtil.INAPP_SIGNED_DATA);
			String signature = intent.getStringExtra(BillingUtil.INAPP_SIGNATURE);
			purchaseStateChanged(context, signedData, signature);
		} else if (BillingUtil.ACTION_NOTIFY.equals(action)) {
			String notifyId = intent.getStringExtra(BillingUtil.NOTIFICATION_ID);
			notify(context, notifyId);
		} else if (BillingUtil.ACTION_RESPONSE_CODE.equals(action)) {
			long requestId = intent.getLongExtra(BillingUtil.INAPP_REQUEST_ID, -1);
			int responseCodeIndex = intent.getIntExtra(
					BillingUtil.INAPP_RESPONSE_CODE,
					ResponseCode.RESULT_ERROR.ordinal());
			checkResponseCode(context, requestId, responseCodeIndex);
		} else {
			Log.w(TAG, "unexpected action: " + action);
		}
	}

	private void purchaseStateChanged(Context context, String signedData,
			String signature) {
		Intent intent = new Intent(BillingUtil.ACTION_PURCHASE_STATE_CHANGED);
		intent.setClass(context, BillingService.class);
		intent.putExtra(BillingUtil.INAPP_SIGNED_DATA, signedData);
		intent.putExtra(BillingUtil.INAPP_SIGNATURE, signature);
		context.startService(intent);
	}

	private void notify(Context context, String notifyId) {
		Intent intent = new Intent(BillingUtil.ACTION_GET_PURCHASE_INFORMATION);
		intent.setClass(context, BillingService.class);
		intent.putExtra(BillingUtil.NOTIFICATION_ID, notifyId);
		context.startService(intent);
	}

	private void checkResponseCode(Context context, long requestId,
			int responseCodeIndex) {
		Intent intent = new Intent(BillingUtil.ACTION_RESPONSE_CODE);
		intent.setClass(context, BillingService.class);
		intent.putExtra(BillingUtil.INAPP_REQUEST_ID, requestId);
		intent.putExtra(BillingUtil.INAPP_RESPONSE_CODE, responseCodeIndex);
		context.startService(intent);
	}
}
