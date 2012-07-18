package net.jessechen.instawifi;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;

public class DonateActivity extends Activity implements OnClickListener,
		RadioGroup.OnCheckedChangeListener {
	private RadioGroup donateOptions;
	private Button donateButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		donateButton = (Button) findViewById(R.id.donate_button);
		donateOptions = (RadioGroup) findViewById(R.id.donate_options);


		setContentView(R.layout.donate_activity);
	}

	@Override
	public void onClick(View v) {
		if (v == donateButton) {
			// launch android billing service here
		}
	}

	@Override
	public void onCheckedChanged(RadioGroup group, int checkedId) {
		RadioButton checkedRadioButton = (RadioButton) donateOptions.findViewById(checkedId);
		if (checkedRadioButton.isChecked()) {
			donateButton.setText(String.format(getString(R.string.donate_button),
					checkedRadioButton.toString()));
		}
	}

}
