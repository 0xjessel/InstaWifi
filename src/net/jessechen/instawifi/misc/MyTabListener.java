package net.jessechen.instawifi.misc;

import net.jessechen.instawifi.QrFragment;
import net.jessechen.instawifi.R;
import net.jessechen.instawifi.util.Util;
import android.app.Activity;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

public class MyTabListener implements com.actionbarsherlock.app.ActionBar.TabListener {
	private String mTag;
	private Activity activity;
	private FragmentManager fm;
	private Fragment mFragment;

	public MyTabListener(Activity a,
			android.support.v4.app.FragmentManager fragmentManager, String tag) {
		this.activity = a;
		fm = fragmentManager;
		mTag = tag;
	}

	@Override
	public void onTabSelected(com.actionbarsherlock.app.ActionBar.Tab tab,
			FragmentTransaction ft) {
		View layout = activity.findViewById(R.id.nfc_layout);

		if (mTag.equals(Util.NFC)) {
			layout.setVisibility(View.VISIBLE);
			if (mFragment != null) {
				fm.beginTransaction().detach(mFragment).commit();
			}
			Util.curTab = Util.NFC;
		} else if (mTag.equals(Util.QR)) {
			layout.setVisibility(View.GONE);

			mFragment = QrFragment.getInstance();
			fm.beginTransaction().replace(R.id.fragment, mFragment).commit();
			tab.setTag(mTag);
			Util.curTab = Util.QR;
		}		
	}

	@Override
	public void onTabUnselected(com.actionbarsherlock.app.ActionBar.Tab tab,
			FragmentTransaction ft) {
		// detach QrFragment on unselect
		if (mTag.equals(Util.QR)) {
			if (mFragment != null) {
				fm.beginTransaction().detach(mFragment).commit();
			}
		}		
	}

	@Override
	public void onTabReselected(com.actionbarsherlock.app.ActionBar.Tab tab,
			FragmentTransaction ft) {
	}
}
