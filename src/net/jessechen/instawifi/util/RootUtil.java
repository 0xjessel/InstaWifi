package net.jessechen.instawifi.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
import android.net.wifi.WifiConfiguration;
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
	public static String WPA_SUPPLICANT_CONF_PATH = "/data/misc/wifi/wpa_supplicant.conf";
	public static String DESTINATION_FILENAME = "wifipw.txt";

	public static String ABSOLUTE_DESTINATION_PATH(Context c) {
		return String.format("%s/%s", c.getFilesDir().getAbsolutePath(),
				DESTINATION_FILENAME);
	}

	public static String COPY_WIFI_CONF_FILE_COMMAND(Context c) {
		return String.format("dd if=%s of=%s", WPA_SUPPLICANT_CONF_PATH,
				ABSOLUTE_DESTINATION_PATH(c));
	}

	// permissions set so other users can read only
	public static String CHMOD_FILE_COMMAND(Context c) {
		return String.format("chmod 004 %s", ABSOLUTE_DESTINATION_PATH(c));
	}

	public static String getCurrentWifiPassword(final Context c,
			WifiConfiguration wc) throws PasswordNotFoundException {
		if (wc.SSID == null) {
			Log.e(Util.TAG,
					"could not get current wifi's password because ssid is null");
			throw new PasswordNotFoundException("ssid is null");
		}

		String password = null;

		// get wifi_supplicant.conf file if rooted
		if (ExecuteAsRootBase.canRunRootCommands()) {
			ExecuteAsRootBase su = new ExecuteAsRootBase() {

				@Override
				protected ArrayList<String> getCommandsToExecute() {
					ArrayList<String> commandsList = new ArrayList<String>();
					commandsList.add(COPY_WIFI_CONF_FILE_COMMAND(c));
					commandsList.add(CHMOD_FILE_COMMAND(c));
					return commandsList;
				}
			};

			if (su.execute()) {
				Log.i(Util.TAG, "sudo made me a sandwich");
				password = getPasswordFromFile(c, wc.SSID);
			} else {
				Log.e(Util.TAG, "sudo failed to make me a sandwich");
				throw new PasswordNotFoundException("su failed to execute");
			}
		}
		return password;
	}

	public static String getPasswordFromFile(Context c, String ssid)
			throws PasswordNotFoundException {
		HashMap<String, String> networkConfigs = getNetworkConfigs(c, ssid);

		String key_mgmt = networkConfigs.get("key_mgmt");
		if (key_mgmt != null) {
			if (key_mgmt.equals("WPA-PSK")) {
				// WPA protocol
				String pw = networkConfigs.get("psk");
				if (pw != null) {
					return pw;
				} else {
					Log.e(Util.TAG, "found WPA-PSK config with no PSK password");
					throw new PasswordNotFoundException(
							"WPA-PSK config with no PSK password");
				}
			} else if (key_mgmt.equals("NONE")) {
				// WEP or OPEN
				String pw = networkConfigs.get("wep_key0");
				if (pw == null) {
					// OPEN
					return null;
				} else {
					// WEP
					return pw;
				}
			}
		}

		Log.e(Util.TAG, "found no password from file");
		throw new PasswordNotFoundException("found no password from file");
	}

	private static HashMap<String, String> getNetworkConfigs(Context c,
			String ssid) {
		String fileContents = getFileThenDelete(c);
		String lines[] = fileContents.split("\\r?\\n");

		boolean found = false;
		ArrayList<String> rows = new ArrayList<String>();

		for (String dirtyLine : lines) {
			String line = dirtyLine.trim();
			if (!found) {
				if (line.equals(String.format("ssid=%s", ssid))) {
					found = true;
					rows.add(line);
				}
			} else {
				if (!line.equals("}")) {
					rows.add(line);
				} else {
					found = false;
					break;
				}
			}
		}

		if (rows.size() == 0) {
			return null;
		}

		HashMap<String, String> toReturn = new HashMap<String, String>();
		for (String row : rows) {
			String[] kv = row.split("=");
			if (kv.length != 2) {
				return null;
			}
			toReturn.put(kv[0], kv[1]);
		}
		return toReturn;
	}

	public static String getFileThenDelete(Context c) {
		String content = "";
		FileInputStream fis;
		try {
			fis = c.openFileInput(DESTINATION_FILENAME);
			byte[] buffer = new byte[1024];
			@SuppressWarnings("unused")
			int length;
			while ((length = fis.read(buffer)) != -1) {
				content += new String(buffer);
			}
		} catch (FileNotFoundException e) {
			Log.e(Util.TAG, "wifi passwords file not found");
			e.printStackTrace();
		} catch (IOException e) {
			Log.e(Util.TAG, "IOException while reading wifi passwords file");
			e.printStackTrace();
		}
		
		// do not retain wifi password file
		c.deleteFile(DESTINATION_FILENAME);
		
		return content;
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

	@SuppressWarnings("serial")
	public static class PasswordNotFoundException extends Exception {

		public PasswordNotFoundException(String msg) {
			super(msg);
		}

	}
}
