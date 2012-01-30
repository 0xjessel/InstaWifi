package net.jessechen.instawifi;

import android.app.ActionBar;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActionBar.Tab;
import android.support.v4.app.ActionBar.TabListener;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;

public class QrActivity extends FragmentActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.qr_activity);

		android.support.v4.app.ActionBar bar = getSupportActionBar();
		bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		bar.addTab(bar.newTab().setText(getString(R.string.nfc_tab))
				.setTabListener(new TabListener() {

					@Override
					public void onTabUnselected(Tab tab, FragmentTransaction ft) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onTabSelected(Tab tab, FragmentTransaction ft) {
						startActivity(new Intent(getApplicationContext(),
								NfcActivity.class));
						finish();
					}

					@Override
					public void onTabReselected(Tab tab, FragmentTransaction ft) {
						// TODO Auto-generated method stub

					}
				}));
		bar.addTab(bar.newTab().setText(getString(R.string.qr_tab))
				.setTabListener(new TabListener() {

					@Override
					public void onTabUnselected(Tab tab, FragmentTransaction ft) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onTabSelected(Tab tab, FragmentTransaction ft) {
						// TODO Auto-generated method stub

					}

					@Override
					public void onTabReselected(Tab tab, FragmentTransaction ft) {
						// TODO Auto-generated method stub

					}
				}));
	}
}
