package net.jessechen.instawifi.util;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.facebook.HttpMethod;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.android.Util;
import com.facebook.model.GraphObject;
import com.facebook.model.OpenGraphAction;

public class FBUtil {
	public static final String NAMESPACE = "instawifi";
	public static final String POST_ACTION_PATH = "me/" + NAMESPACE
			+ ":connect";
	public static final String POST_OG_URL = "http://www.instawifi.jessechen.net/network.php?ssid=test";
	public static final List<String> PERMISSIONS = Arrays
			.asList("publish_actions");

	private static final String TAG = Util.class.getSimpleName();

	public static boolean hasPublishPermission(Activity a, Session session) {
		List<String> permissions = session.getPermissions();
		if (!permissions.containsAll(PERMISSIONS)) {
			return false;
		}
		return true;
	}

	// assumes that publish_actions permission has already been granted
	public static void publishOG(final Context c, final String networkName) {
		AsyncTask<Bundle, Void, Response> task = new AsyncTask<Bundle, Void, Response>() {

			@Override
			protected Response doInBackground(Bundle... paramsArray) {
				Bundle params = (paramsArray.length == 0) ? new Bundle()
						: paramsArray[0];
				ConnectAction connectAction = GraphObject.Factory
						.create(ConnectAction.class);
				Request request = new Request(Session.getActiveSession(),
						POST_ACTION_PATH, params, HttpMethod.POST);
				request.setGraphObject(connectAction);
				NetworkGraphObject network = GraphObject.Factory
						.create(NetworkGraphObject.class);
				network.setURL(POST_OG_URL);
				connectAction.setNetwork(network);
				return request.executeAndWait();
			}

			@Override
			protected void onPostExecute(Response response) {
				Log.e(TAG, response.toString());
			}
		};

		// Execute the task
		task.execute();
	}

	public interface ConnectAction extends OpenGraphAction {
		public NetworkGraphObject getNetwork();

		public void setNetwork(NetworkGraphObject naan);
	}

	public interface NetworkGraphObject extends GraphObject {
		public String getURL();

		public void setURL(String url);
	}

}
