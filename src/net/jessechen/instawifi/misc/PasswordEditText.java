package net.jessechen.instawifi.misc;

import net.jessechen.instawifi.QrFragment;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;

// TODO: edittextwatcher for invalid passwords (wep/wpa pw restrictions)
public class PasswordEditText extends EditText {
	private Context c;
	private ImageView qrImage;
	private static String TAG = PasswordEditText.class.getSimpleName();

	public PasswordEditText(Context context) {
		super(context);
	}

	public PasswordEditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public PasswordEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	public void init(Context c, ImageView qrImage) {
		this.c = c;
		this.qrImage = qrImage;
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_UP) {
			PasswordEditTextFinishedCallback(c, this, qrImage);
			return true;
		}
		return false;
	}

	// redraw qr code and hide keyboard
	public static void PasswordEditTextFinishedCallback(Context c,
			PasswordEditText p, ImageView qrImage) {
		// hide keyboard
		InputMethodManager inputManager = (InputMethodManager) c
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(p.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);
		
		Log.i(TAG, "done with pw changes, redrawing qr image");
		
		QrFragment.setQrImage();
	}
}
