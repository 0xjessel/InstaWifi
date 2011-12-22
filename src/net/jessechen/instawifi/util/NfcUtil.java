package net.jessechen.instawifi.util;

import net.jessechen.instawifi.R;
import android.content.Context;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.util.Log;

public class NfcUtil {
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
			 Log.i(Util.TAG, "Unknown intent.");
			// finish();
		}
		return msgs;
	}

	/*
	 * Writes an NdefMessage to a NFC tag
	 */
	public static boolean writeTag(NdefMessage message, Tag tag, Context c) {
		int size = message.toByteArray().length;
		try {
			Ndef ndef = Ndef.get(tag);
			if (ndef != null) {
				ndef.connect();
				if (!ndef.isWritable()) {
					Util.shortToast(c, c.getString(R.string.tag_read_only))
							.show();
					return false;
				}
				if (ndef.getMaxSize() < size) {
					Util.shortToast(c, c.getString(R.string.tag_too_small))
							.show();
					return false;
				}
				ndef.writeNdefMessage(message);
				return true;
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
									c.getString(R.string.tag_format_fail))
									.show();
						}
					} catch (Exception e) {
						Util.shortToast(c,
								c.getString(R.string.tag_format_connect_fail))
								.show();
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
			Log.e(Util.TAG, "Exception when writing tag", e);
			return false;
		}
		return false;
	}
}
