package net.jessechen.instawifi.util;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

public class BillingUtil {
	public static final String itemId1 = "thankyou_s";
	public static final String itemId2 = "thankyou_m";
	public static final String itemId3 = "thankyou_l";
	public static final String itemId4 = "thankyou_xl";
	public static final DonateOption donateOption1 = new DonateOption(2, 0);
	public static final DonateOption donateOption2 = new DonateOption(4, 1);
	public static final DonateOption donateOption3 = new DonateOption(6, 2);
	public static final DonateOption donateOption4 = new DonateOption(10, 4);

	public static class DonateOption {
		public int amount;
		public int numNfcStickers;

		public DonateOption(int amount, int numNfcStickers) {
			this.amount = amount;
			this.numNfcStickers = numNfcStickers;
		}

		@Override
		public String toString() {
			return amount + "$";
		}
	}

	@SuppressWarnings("serial")
	public static HashMap<String, DonateOption> map = new HashMap<String, DonateOption>() {
		{
			put(itemId1, donateOption1);
			put(itemId2, donateOption2);
			put(itemId3, donateOption3);
			put(itemId4, donateOption4);
		}
	};

	public static String getItemId(DonateOption donateOption) {
		for (Entry<String, DonateOption> entry : map.entrySet()) {
			if (donateOption.equals(entry.getValue())) {
				return entry.getKey();
			}
		}
		return null;
	}

	// The response codes for a request, defined by Android Market.
	public enum ResponseCode {
		RESULT_OK, RESULT_USER_CANCELED, RESULT_SERVICE_UNAVAILABLE, RESULT_BILLING_UNAVAILABLE, RESULT_ITEM_UNAVAILABLE, RESULT_DEVELOPER_ERROR, RESULT_ERROR;

		// Converts from an ordinal value to the ResponseCode
		public static ResponseCode valueOf(int index) {
			ResponseCode[] values = ResponseCode.values();
			if (index < 0 || index >= values.length) {
				return RESULT_ERROR;
			}
			return values[index];
		}
	}

	// The possible states of an in-app purchase, as defined by Android Market.
	public enum PurchaseState {
		// Responses to requestPurchase or restoreTransactions.
		PURCHASED, // User was charged for the order.
		CANCELED, // The charge failed on the server.
		REFUNDED; // User received a refund for the order.

		// Converts from an ordinal value to the PurchaseState
		public static PurchaseState valueOf(int index) {
			PurchaseState[] values = PurchaseState.values();
			if (index < 0 || index >= values.length) {
				return CANCELED;
			}
			return values[index];
		}
	}

	/** This is the action we use to bind to the MarketBillingService. */
	public static final String MARKET_BILLING_SERVICE_ACTION =
			"com.android.vending.billing.MarketBillingService.BIND";

	// Intent actions that we send from the BillingReceiver to the
	// BillingService. Defined by this application.
	public static final String ACTION_CONFIRM_NOTIFICATION =
			"net.jessechen.instawifi.CONFIRM_NOTIFICATION";
	public static final String ACTION_GET_PURCHASE_INFORMATION =
			"net.jessechen.instawifi.GET_PURCHASE_INFORMATION";
	public static final String ACTION_RESTORE_TRANSACTIONS =
			"net.jessechen.instawifi.RESTORE_TRANSACTIONS";

	// Intent actions that we receive in the BillingReceiver from Market.
	// These are defined by Market and cannot be changed.
	public static final String ACTION_NOTIFY = "com.android.vending.billing.IN_APP_NOTIFY";
	public static final String ACTION_RESPONSE_CODE = "com.android.vending.billing.RESPONSE_CODE";
	public static final String ACTION_PURCHASE_STATE_CHANGED =
			"com.android.vending.billing.PURCHASE_STATE_CHANGED";

	// These are the names of the extras that are passed in an intent from
	// Market to this application and cannot be changed.
	public static final String NOTIFICATION_ID = "notification_id";
	public static final String INAPP_SIGNED_DATA = "inapp_signed_data";
	public static final String INAPP_SIGNATURE = "inapp_signature";
	public static final String INAPP_REQUEST_ID = "request_id";
	public static final String INAPP_RESPONSE_CODE = "response_code";

	// These are the names of the fields in the request bundle.
	public static final String BILLING_REQUEST_METHOD = "BILLING_REQUEST";
	public static final String BILLING_REQUEST_API_VERSION = "API_VERSION";
	public static final String BILLING_REQUEST_PACKAGE_NAME = "PACKAGE_NAME";
	public static final String BILLING_REQUEST_ITEM_ID = "ITEM_ID";
	public static final String BILLING_REQUEST_ITEM_TYPE = "ITEM_TYPE";
	public static final String BILLING_REQUEST_DEVELOPER_PAYLOAD = "DEVELOPER_PAYLOAD";
	public static final String BILLING_REQUEST_NOTIFY_IDS = "NOTIFY_IDS";
	public static final String BILLING_REQUEST_NONCE = "NONCE";

	public static final String BILLING_RESPONSE_RESPONSE_CODE = "RESPONSE_CODE";
	public static final String BILLING_RESPONSE_PURCHASE_INTENT = "PURCHASE_INTENT";
	public static final String BILLING_RESPONSE_REQUEST_ID = "REQUEST_ID";
	public static long BILLING_RESPONSE_INVALID_REQUEST_ID = -1;

	// These are the types supported in the IAB v2
	public static final String ITEM_TYPE_INAPP = "inapp";
	public static final String ITEM_TYPE_SUBSCRIPTION = "subs";

	public static final boolean DEBUG = false;
	
	private static final SecureRandom RANDOM = new SecureRandom();
	private static final HashSet<Long> sKnownNonces = new HashSet<Long>();

	public static long generateNonce() {
		long nonce = RANDOM.nextLong();
		sKnownNonces.add(nonce);
		return nonce;
	}

	public static void removeNonce(long nonce) {
		sKnownNonces.remove(nonce);
	}

	public static boolean isNonceKnown(long nonce) {
		return sKnownNonces.contains(nonce);
	}
}
