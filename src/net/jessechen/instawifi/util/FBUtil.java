package net.jessechen.instawifi.util;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;

import com.facebook.GraphObject;
import com.facebook.GraphObjectWrapper;
import com.facebook.HttpMethod;
import com.facebook.OpenGraphAction;
import com.facebook.Request;
import com.facebook.Response;
import com.facebook.Session;
import com.facebook.android.Util;

public class FBUtil {
	public static final String namespace = "instawifi";
	public static final String POST_ACTION_PATH = "me/" + namespace + ":connect";
	public static final String POST_OG_URL = "http://www.instawifi.jessechen.net/network.php";
	public static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	public static final int REAUTH_ACTIVITY_CODE = 100;

	public static boolean hasPublishPermission(Activity a, Session session) {
		List<String> permissions = session.getPermissions();
		if (!permissions.containsAll(PERMISSIONS)) {
			return false;
		}
		return true;
	}

	// assumes that publish_actions permission has already been granted
	public static void publishOG(final Context c, final String networkName) {
		AsyncTask<Void, Void, Response> task = new AsyncTask<Void, Void, Response>() {

			@Override
			protected Response doInBackground(Void... voids) {
				// Create an network action
				ConnectAction connectAction =
						GraphObjectWrapper.createGraphObject(ConnectAction.class);
				// Populate the action with the POST parameters
				populateOGAction(connectAction, networkName);
				// Set up a request with the active session, set up
				// an HTTP POST to the eat action endpoint
				Request request =
						new Request(Session.getActiveSession(), POST_ACTION_PATH, null,
								HttpMethod.POST);
				// Add the post parameter, the eat action
				request.setGraphObject(connectAction);
				// Execute the request synchronously in the background
				// and return the response.
				return request.executeAndWait();
			}

			private void populateOGAction(ConnectAction action, String networkName) {
				ConnectAction connectAction = action.cast(ConnectAction.class);
				NetworkGraphObject network =
						GraphObjectWrapper.createGraphObject(NetworkGraphObject.class);

				Bundle networkParams = new Bundle();
				networkParams.putString("ssid", networkName);
				String networkURL = POST_OG_URL + "?" + Util.encodeUrl(networkParams);
				network.setURL(networkURL);
				connectAction.setNetwork(network);
			}

			@Override
			protected void onPostExecute(Response response) {
				// When the task completes, process
				// the response on the main thread
				onPostActionResponse(c, response);
			}
		};

		// Execute the task
		task.execute();
	}

	private static void onPostActionResponse(Context c, Response response) {
		// Get the id of the response. If there is an error
		// the called method will show an error dialog
		String result = getIdFromResponseOrShowError(response);
		Toast.makeText(c, result, Toast.LENGTH_LONG).show();
	}

	private static String getIdFromResponseOrShowError(Response response) {
		// Cast the response as a PostResponse object
		// so you can extract its body and ID.
		PostResponse postResponse = response.getGraphObjectAs(PostResponse.class);

		String id = null;
		PostResponse.Body body = null;
		if (postResponse != null) {
			// Get the ID from the response
			id = postResponse.getId();
			// Get the body from the response
			body = postResponse.getBody();
		}

		String errorMsg = "";

		if (body != null && body.getError() != null) {
			// Check for errors in the body of the response
			errorMsg = body.getError().getMessage();
		} else if (response.getError() != null) {
			// Check for other types of errors
			errorMsg = response.getError().getLocalizedMessage();
		} else if (id != null) {
			// Return an ID if found
			return id;
		}
		return errorMsg;
	}

	private interface NetworkGraphObject extends GraphObject {
		public String getURL();

		public void setURL(String URL);

		public String getID();

		public void setID(String id);
	}

	private interface ConnectAction extends OpenGraphAction {
		// The network object
		public NetworkGraphObject getNetwork();

		public void setNetwork(NetworkGraphObject network);
	}

	private interface PostResponse extends GraphObject {
		Body getBody();

		String getId();

		// Get access to a body's error
		interface Body extends GraphObject {
			Error getError();
		}

		// Get access to an error's message
		interface Error extends GraphObject {
			String getMessage();
		}
	}
}
