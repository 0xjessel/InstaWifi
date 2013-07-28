package net.jessechen.instawifi;

import net.jessechen.instawifi.billing.BillingService;
import net.jessechen.instawifi.billing.BillingService.RequestPurchase;
import net.jessechen.instawifi.billing.BillingService.RestoreTransactions;
import net.jessechen.instawifi.billing.PurchaseObserver;
import net.jessechen.instawifi.billing.ResponseHandler;
import net.jessechen.instawifi.util.BillingUtil;
import net.jessechen.instawifi.util.BillingUtil.DonateOption;
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
		mBillingService.checkBillingSupported();

		ResponseHandler.register(mDonatePurchaseObserver);

		donateButton = (Button) findViewById(R.id.donate_button);
		donateOptions = (RadioGroup) findViewById(R.id.donate_options);
		RadioButton rb1 = (RadioButton) findViewById(R.id.donate_rb1);
		rb1.setText(BillingUtil.donateOption1.toString());
		rb1.setTag(BillingUtil.donateOption1);
		RadioButton rb2 = (RadioButton) findViewById(R.id.donate_rb2);
		rb2.setText(BillingUtil.donateOption2.toString());
		rb2.setTag(BillingUtil.donateOption2);
		RadioButton rb3 = (RadioButton) findViewById(R.id.donate_rb3);
		rb3.setText(BillingUtil.donateOption3.toString());
		rb3.setTag(BillingUtil.donateOption3);
		RadioButton rb4 = (RadioButton) findViewById(R.id.donate_rb4);
		rb4.setText(BillingUtil.donateOption4.toString());
		rb4.setTag(BillingUtil.donateOption4);

		RadioButton defaultOption = (RadioButton) donateOptions
				.findViewById(donateOptions.getCheckedRadioButtonId());
		donateButton.setText(String.format(getString(R.string.donate_button),
				defaultOption.getText().toString()));

		donateOptions.setOnCheckedChangeListener(this);
		donateButton.setOnClickListener(this);
	}

	@Override
	public void onResume() {
		super.onResume();

		com.facebook.Settings.publishInstallAsync(getApplicationContext(),
				getString(R.string.app_id));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mBillingService.unbind();
	}

	@Override
	public void onClick(View v) {
		if (v == donateButton) {
			RadioButton selected = (RadioButton) donateOptions
					.findViewById(donateOptions.getCheckedRadioButtonId());
			mBillingService.requestPurchase((DonateOption) selected.getTag(),
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
			if (!supported) {
				Util.longToast(getApplicationContext(), getApplicationContext()
						.getString(R.string.donate_not_supported));
				donateButton.setEnabled(false);
			}
			if (BillingUtil.DEBUG) {
				Log.i(TAG, "supported: " + supported);
			}
		}

		@Override
		public void onPurchaseStateChange(PurchaseState purchaseState,
				String itemId, String orderId, long purchaseTime,
				String developerPayload) {
			if (BillingUtil.DEBUG) {
				Log.i(TAG, "onPurchaseStateChange() itemId: " + itemId + " "
						+ purchaseState);
			}
			if (PurchaseState.PURCHASED.equals(purchaseState)) {
				if (BillingUtil.itemId1.equals(itemId)) {
					Util.shortToast(
							getApplicationContext(),
							getApplicationContext().getString(
									R.string.donate_thank_you));
				} else {
					Util.shortToast(
							getApplicationContext(),
							getApplicationContext().getString(
									R.string.donate_thank_you_stickers));
					Intent intent = Util.buildDonateEmailIntent(
							getApplicationContext(), orderId,
							BillingUtil.map.get(itemId));
					startActivity(intent);
				}
			}
		}

		@Override
		public void onRequestPurchaseResponse(RequestPurchase request,
				ResponseCode responseCode) {
			if (BillingUtil.DEBUG) {
				Log.d(TAG, request.mProductId + ": " + responseCode);
			}
			if (responseCode == ResponseCode.RESULT_OK) {
				if (BillingUtil.DEBUG) {
					Log.i(TAG, "purchase was successfully sent to server");
				}
			} else if (responseCode == ResponseCode.RESULT_USER_CANCELED) {
				if (BillingUtil.DEBUG) {
					Log.i(TAG, "user canceled purchase");
				}
			} else {
				if (BillingUtil.DEBUG) {
					Log.i(TAG, "purchase failed");
				}
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
