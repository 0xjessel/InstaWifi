package net.jessechen.instawifi;

import net.jessechen.instawifi.util.FBUtil;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;

import com.facebook.FacebookActivity;
import com.facebook.LoginButton;
import com.facebook.Session;
import com.facebook.SessionState;

public class HelpActivity extends FacebookActivity implements OnClickListener {
	ImageButton fbButton, twitterButton;
	LoginButton authButton;
	Button authOGButton;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_activity);

		authButton = (LoginButton) findViewById(R.id.authButton);
		authButton.setApplicationId(getString(R.string.app_id));

		authOGButton = (Button) findViewById(R.id.publishOG);
		authOGButton.setOnClickListener(this);

		fbButton = (ImageButton) findViewById(R.id.fb_button);
		twitterButton = (ImageButton) findViewById(R.id.twitter_button);

		fbButton.setOnClickListener(this);
		twitterButton.setOnClickListener(this);

		this.openSession();
	}

	@Override
	protected void onSessionStateChange(SessionState state, Exception exception) {
		if (state.isOpened() && !FBUtil.hasPublishPermission(this, Session.getActiveSession())) {
			authOGButton.setVisibility(View.VISIBLE);
		}
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == FBUtil.REAUTH_ACTIVITY_CODE) {
			Session.getActiveSession().onActivityResult(this, requestCode, resultCode, data);
			authOGButton.setVisibility(View.GONE);
		}
	}

	private void authOG() {
		Session session = Session.getActiveSession();

		if (session == null || !session.isOpened()) {
			return;
		}

		Session.ReauthorizeRequest reauthRequest =
				new Session.ReauthorizeRequest(this, FBUtil.PERMISSIONS)
						.setRequestCode(FBUtil.REAUTH_ACTIVITY_CODE);
		session.reauthorizeForPublish(reauthRequest);
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
		} else if (v == authOGButton) {
			authOG();
		}
	}
}
