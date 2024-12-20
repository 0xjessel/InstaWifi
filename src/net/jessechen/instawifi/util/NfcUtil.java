package net.jessechen.instawifi.util;

import java.nio.charset.Charset;

import net.jessechen.instawifi.R;
import net.jessechen.instawifi.models.WifiModel;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Build;
import android.os.Parcelable;
import android.util.Log;

@TargetApi(14)
public class NfcUtil {
	private static final String TAG = NfcUtil.class.getSimpleName();

	public static NdefMessage getWifiAsNdef(Context c, WifiModel wm) {
		byte[] url = wm.toWifiUri().getBytes(Charset.forName("US-ASCII"));

		NdefRecord record = new NdefRecord(NdefRecord.TNF_ABSOLUTE_URI, url,
				new byte[0], new byte[0]);

		NdefMessage msg;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			msg = new NdefMessage(new NdefRecord[] { record,
					NdefRecord.createApplicationRecord(c.getPackageName()) });
		} else {
			msg = new NdefMessage(new NdefRecord[] { record });
		}

		return msg;
	}

	public static NdefMessage[] getNdefMessages(Intent intent) {
		// Parse the intent
		NdefMessage[] msgs = null;
		String action = intent.getAction();
		if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
				|| NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
			Parcelable[] rawMsgs = intent
					.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
			if (rawMsgs != null) {
				msgs = new NdefMessage[rawMsgs.length];
				for (int i = 0; i < rawMsgs.length; i++) {
					msgs[i] = (NdefMessage) rawMsgs[i];
				}
			} else {
				// Unknown tag type
				byte[] empty = new byte[] {};
				NdefRecord record = new NdefRecord(NdefRecord.TNF_UNKNOWN,
						empty, empty, empty);
				NdefMessage msg = new NdefMessage(new NdefRecord[] { record });
				msgs = new NdefMessage[] { msg };
			}
		} else {
			Log.i(TAG, "Unknown intent.");
			// finish();
		}
		return msgs;
	}

	/*
	 * Writes an NdefMessage to a NFC tag
	 */
	public static boolean writeTag(NdefMessage message, Tag tag,
			boolean readOnly, Context c) {
		int size = message.toByteArray().length;
		try {
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();
				if (!ndef.isWritable()) {
					Util.shortToast(c, c.getString(R.string.tag_read_only));
					return false;
				}
				if (ndef.getMaxSize() < size) {
					Util.shortToast(c, c.getString(R.string.tag_too_small));
					return false;
				}

				ndef.writeNdefMessage(message);

				if (!readOnly) {
					return true;
				}

				// wants to make it read only, but unable to
				if (!ndef.canMakeReadOnly()) {
					Util.longToast(c,
							"Sorry, it is not possible to make this NFC tag read-only");
					return false;
				}

				// make it read only
				if (ndef.makeReadOnly()) {
					Util.longToast(c, "Successfully made tag read-only");
					return true;
				} else {
					// failed to make it read only
					Util.longToast(c, "Failed to make tag read-only");
					return false;
				}
			} else {
				NdefFormatable format = NdefFormatable.get(tag);
				if (format != null) {
					try {
						format.connect();
						try {
							format.format(message);
							return true;
						} catch (Exception e) {
							Util.shortToast(c,
									c.getString(R.string.tag_format_fail));
						}
					} catch (Exception e) {
						Util.shortToast(c,
								c.getString(R.string.tag_format_connect_fail));
						return false;
					} finally {
						format.close();
					}
				} else {
					Util.shortToast(c,
							c.getString(R.string.tag_ndef_not_supported));
					return false;
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Exception when writing tag", e);
			return false;
		}
		return false;
	}
}
