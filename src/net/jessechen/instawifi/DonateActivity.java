package net.jessechen.instawifi;

import net.jessechen.instawifi.billing.BillingService;
import net.jessechen.instawifi.billing.BillingService.RequestPurchase;
import net.jessechen.instawifi.billing.BillingService.RestoreTransactions;
import net.jessechen.instawifi.billing.PurchaseObserver;
import net.jessechen.instawifi.billing.ResponseHandler;
import net.jessechen.instawifi.util.BillingUtil;
import net.jessechen.instawifi.util.BillingUtil.PurchaseState;
import net.jessechen.instawifi.util.BillingUtil.ResponseCode;
import net.jessechen.instawifi.util.Util;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import com.actionbarsherlock.app.SherlockActivity;

public class DonateActivity extends SherlockActivity implements
		OnClickListener, RadioGroup.OnCheckedChangeListener {
	private static final String TAG = DonateActivity.class.getSimpleName();

	private RadioGroup donateOptions;
	private Button donateButton;

	private Handler mHandler;
	private DonatePurchaseObserver mDonatePurchaseObserver;
	private BillingService mBillingService;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.donate_activity);

		mHandler = new Handler();
		mDonatePurchaseObserver = new DonatePurchaseObserver(mHandler);
		mBillingService = new BillingService();
		mBillingService.setContext(this);

		ResponseHandler.register(mDonatePurchaseObserver);
		
		donateButton = (Button) findViewById(R.id.donate_button);
		donateOptions = (RadioGroup) findViewById(R.id.donate_options);

		RadioButton defaultOption = (RadioButton) donateOptions
				.findViewById(donateOptions.getCheckedRadioButtonId());
		donateButton.setText(String.format(getString(R.string.donate_button),
				defaultOption.getText().toString()));
		donateOptions.setOnCheckedChangeListener(this);

		donateButton.setOnClickListener(this);
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		mBillingService.unbind();
	}

	@Override
	public void onClick(View v) {
		if (v == donateButton) {
			// launch android billing service here
			Log.d(TAG, "donateButton pressed");
			mBillingService.requestPurchase("android.test.purchased",
					BillingUtil.ITEM_TYPE_INAPP, null);
			return;
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		RadioButton checkedRadioButton = (RadioButton) donateOptions
				.findViewById(checkedId);
		if (checkedRadioButton.isChecked()) {
			donateButton.setText(String.format(
					getString(R.string.donate_button), checkedRadioButton
							.getText().toString()));
		}
	}

	private class DonatePurchaseObserver extends PurchaseObserver {

		public DonatePurchaseObserver(Handler handler) {
			super(DonateActivity.this, handler);
		}

		@Override
		public void onBillingSupported(boolean supported, String type) {
			Log.i(TAG, "supported: " + supported);
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState,
				String itemId, int quantity, long purchaseTime,
				String developerPayload) {
			Log.i(TAG, "onPurchaseStateChange() itemId: " + itemId + " "
					+ purchaseState);
			if (PurchaseState.PURCHASED.equals(purchaseState)) {
				// dont send email if donateOption1
				if (itemId.equals("android.test.purchased")) {
					// map itemId to donateOption
					Intent intent = Util.buildDonateEmailIntent(getApplicationContext(), 6);
					startActivity(intent);
				}
			}
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request,
				ResponseCode responseCode) {
			Log.d(TAG, request.mProductId + ": " + responseCode);
			if (responseCode == ResponseCode.RESULT_OK) {
				Log.i(TAG, "purchase was successfully sent to server");
			} else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
				Log.i(TAG, "user canceled purchase");
			} else {
				Log.i(TAG, "purchase failed");
			}
		}

		@Override
		public void onRestoreTransactionsResponse(RestoreTransactions request,
				ResponseCode responseCode) {
			if (responseCode == ResponseCode.RESULT_OK) {
				Log.d(TAG, "completed RestoreTransactions request");
				// Update the shared preferences so that we don't perform
				// a RestoreTransactions again.
			} else {
				Log.d(TAG, "RestoreTransactions error: " + responseCode);
			}
		}
	}
}
