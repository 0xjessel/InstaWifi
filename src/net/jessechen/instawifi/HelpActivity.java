package net.jessechen.instawifi;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

import com.actionbarsherlock.app.SherlockActivity;

public class HelpActivity extends SherlockActivity implements OnClickListener {
	ImageButton fbButton, twitterButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_activity);

		fbButton = (ImageButton) findViewById(R.id.fb_button);
		twitterButton = (ImageButton) findViewById(R.id.twitter_button);

		fbButton.setOnClickListener(this);
		twitterButton.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v == fbButton) {
			Intent intent =
					new Intent(Intent.ACTION_VIEW, Uri.parse(getApplicationContext().getString(
							R.string.fb_url)));
			startActivity(intent);
		} else if (v == twitterButton) {
			Intent intent =
					new Intent(Intent.ACTION_VIEW, Uri.parse(getApplicationContext().getString(
							R.string.twitter_url)));
			startActivity(intent);
		}
	}
}
