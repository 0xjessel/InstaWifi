package net.jessechen.instawifi;

import net.jessechen.instawifi.util.FBUtil;
import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;

import com.facebook.Session;
import com.facebook.SessionDefaultAudience;
import com.facebook.SessionState;
import com.facebook.UiLifecycleHelper;
import com.facebook.widget.LoginButton;

public class HelpActivity extends Activity implements OnClickListener {
	ImageButton fbButton, twitterButton;
	LoginButton authButton;
	Button authOGButton;

	private UiLifecycleHelper uiHelper;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_activity);

		uiHelper = new UiLifecycleHelper(this, callback);
		uiHelper.onCreate(savedInstanceState);

		authButton = (LoginButton) findViewById(R.id.authButton);
		authButton.setApplicationId(getString(R.string.app_id));

		authOGButton = (Button) findViewById(R.id.publishOG);
		authOGButton.setOnClickListener(this);

		fbButton = (ImageButton) findViewById(R.id.fb_button);
		twitterButton = (ImageButton) findViewById(R.id.twitter_button);

		fbButton.setOnClickListener(this);
		twitterButton.setOnClickListener(this);
	}

	private void onSessionStateChange(Session session, SessionState state,
			Exception exception) {
		if (state.isOpened() && !FBUtil.hasPublishPermission(this, session)) {
			authOGButton.setVisibility(View.VISIBLE);
		} else if (FBUtil.hasPublishPermission(this, session)
				|| state.isClosed()) {
			authOGButton.setVisibility(View.GONE);
		}
	}

	private void authOG() {
		Session session = Session.getActiveSession();

		if (session == null || !session.isOpened()
				|| FBUtil.hasPublishPermission(this, session)) {
			return;
		}

		Session.NewPermissionsRequest newPermissionsRequest = new Session.NewPermissionsRequest(
				this, FBUtil.PERMISSIONS)
				.setDefaultAudience(SessionDefaultAudience.FRIENDS);
		session.requestNewPublishPermissions(newPermissionsRequest);
		return;
	}

	private Session.StatusCallback callback = new Session.StatusCallback() {
		@Override
		public void call(Session session, SessionState state,
				Exception exception) {
			onSessionStateChange(session, state, exception);
		}
	};

	@Override
	public void onClick(View v) {
		if (v == fbButton) {
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(getApplicationContext()
							.getString(R.string.fb_url)));
			startActivity(intent);
		} else if (v == twitterButton) {
			Intent intent = new Intent(Intent.ACTION_VIEW,
					Uri.parse(getApplicationContext().getString(
							R.string.twitter_url)));
			startActivity(intent);
		} else if (v == authOGButton) {
			authOG();
		}
	}

	@Override
	public void onResume() {
		super.onResume();
		uiHelper.onResume();

		com.facebook.Settings.publishInstallAsync(getApplicationContext(),
				getString(R.string.app_id));
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		uiHelper.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	public void onPause() {
		super.onPause();
		uiHelper.onPause();
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uiHelper.onDestroy();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		uiHelper.onSaveInstanceState(outState);
	}
}
