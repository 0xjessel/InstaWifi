package net.jessechen.instawifi.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.util.Log;

/*
 * root docs
 * http://www.addictivetips.com/mobile/how-to-view-passwords-for
 * -wi-fi-access-points-saved-on-your-android-device/
 * http://muzikant-android
 * .blogspot.com/2011/02/how-to-get-root-access-and-execute.html
 * http://www
 * .stealthcopter.com/blog/2010/01/android-requesting-root-access
 * -in-your-app/
 */
public class RootUtil {
	public static String DESTINATION_FILENAME = "wifipw.txt";

	public static String COPY_WIFI_CONF_FILE_COMMAND(String dir) {
		return String.format("dd if=/data/misc/wifi/wpa_supplicant.conf of=%s/%s",
				dir, DESTINATION_FILENAME);
	}

	public static abstract class ExecuteAsRootBase {
		public static boolean canRunRootCommands() {
			boolean retval = false;
			Process suProcess;
			try {
				suProcess = Runtime.getRuntime().exec("su");

				DataOutputStream os = new DataOutputStream(
						suProcess.getOutputStream());
				DataInputStream osRes = new DataInputStream(
						suProcess.getInputStream());

				if (null != os && null != osRes) {
					// Getting the id of the current user to check if this is
					// root
					os.writeBytes("id\n");
					os.flush();

					String currUid = osRes.readLine();
					boolean exitSu = false;
					if (null == currUid) {
						retval = false;
						exitSu = false;
						Log.i(Util.TAG,
								"Can't get root access or denied by user");
					} else if (true == currUid.contains("uid=0")) {
						retval = true;
						exitSu = true;
						Log.i(Util.TAG, "Root access granted");
					} else {
						retval = false;
						exitSu = true;
						Log.i(Util.TAG, "Root access rejected: " + currUid);
					}

					if (exitSu) {
						os.writeBytes("exit\n");
						os.flush();
					}
				}
			} catch (Exception e) {
				// Can't get root !
				// Probably broken pipe exception on trying to write to output
				// stream after su failed, meaning that the device is not rooted

				retval = false;
				Log.i(Util.TAG, "Root access rejected ["
						+ e.getClass().getName() + "] : " + e.getMessage());
			}

			return retval;
		}

		public final boolean execute() {
			boolean retval = false;

			try {
				ArrayList<String> commands = getCommandsToExecute();
				if (null != commands && commands.size() > 0) {
					Process suProcess = Runtime.getRuntime().exec("su");

					DataOutputStream os = new DataOutputStream(
							suProcess.getOutputStream());

					// Execute commands that require root access
					for (String currCommand : commands) {
						os.writeBytes(currCommand + "\n");
						os.flush();
					}

					os.writeBytes("exit\n");
					os.flush();

					try {
						int suProcessRetval = suProcess.waitFor();
						if (255 != suProcessRetval) {
							// Root access granted
							retval = true;
						} else {
							// Root access denied
							retval = false;
						}
					} catch (Exception ex) {
						Log.e(Util.TAG, "Error executing root action");
					}
				}
			} catch (IOException ex) {
				Log.w(Util.TAG, "Can't get root access", ex);
			} catch (SecurityException ex) {
				Log.w(Util.TAG, "Can't get root access", ex);
			} catch (Exception ex) {
				Log.w(Util.TAG, "Error executing internal operation", ex);
			}

			return retval;
		}

		protected abstract ArrayList<String> getCommandsToExecute();
	}
}
