package net.jessechen.instawifi.misc;

import net.jessechen.instawifi.QrFragment;
import net.jessechen.instawifi.R;
import net.jessechen.instawifi.util.Util;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

// TODO: edittextwatcher for invalid passwords (wep/wpa pw restrictions)
public class PasswordEditText extends EditText {
	private Context c;
	private ImageView qrImage;
	private String pw = "";
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
		this.setOnEditorActionListener(mOnEditorActionListener);
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		String curPw = this.getText().toString();
		Log.e(TAG, curPw + " " + pw);
		if (keyCode == KeyEvent.KEYCODE_BACK
				&& event.getAction() == KeyEvent.ACTION_UP && !curPw.equals(pw)) {
			pw = curPw;
			PasswordEditTextFinishedCallback(c, qrImage);
			return true;
		}
		return false;
	}

	// redraw qr code and hide keyboard
	public void PasswordEditTextFinishedCallback(Context c, ImageView qrImage) {
		// hide keyboard
		InputMethodManager inputManager = (InputMethodManager) c
				.getSystemService(Context.INPUT_METHOD_SERVICE);
		inputManager.hideSoftInputFromWindow(this.getWindowToken(),
				InputMethodManager.HIDE_NOT_ALWAYS);

		Log.i(TAG, "done with pw changes, redrawing qr image");
		Util.shortToast(c, c.getString(R.string.updated_qr_code));

		QrFragment.setQrImage();
	}

	private TextView.OnEditorActionListener mOnEditorActionListener = new TextView.OnEditorActionListener() {

		@Override
		public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
			if (actionId == EditorInfo.IME_ACTION_DONE) {
				pw = v.getText().toString();
				PasswordEditTextFinishedCallback(c, qrImage);
				return true;
			}
			return false;
		}
	};
}
