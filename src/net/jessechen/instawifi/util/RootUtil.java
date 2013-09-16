package net.jessechen.instawifi.util;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import android.content.Context;
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

	private static final String TAG = RootUtil.class.getSimpleName();

	/**
	 * gets the wifi password from the current connected network. requires root
	 * access on phone.
	 * 
	 * @param c
	 * @param wc
	 * @return null if no root access, or the wifi's password in a String.
	 * @throws PasswordNotFoundException
	 *             if an error occurs during the process
	 */
	public static String getWifiPassword(final Context c, String SSID)
			throws PasswordNotFoundException {
		if (SSID == null) {
			Log.e(TAG,
					"could not get wifi password because WifiModel is invalid");
			throw new PasswordNotFoundException("WifiModel is invalid");
		}

		String password = getPasswordFromFile(c, SSID);

		if (password == null) {
			if (!ExecuteAsRootBase.canRunRootCommands()) {
				Log.e(TAG, "cannot run root commands, throwing exception");
				throw new PasswordNotFoundException(
						"we do not have root permissions");
			}

			// get wifi_supplicant.conf file if rooted
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
				Log.i(TAG, "sudo made me a sandwich");
				password = getPasswordFromFile(c, SSID);
			} else {
				Log.e(TAG, "sudo failed to make me a sandwich");
				throw new PasswordNotFoundException("su failed to execute");
			}
		}

		if (password == null) {
			Log.i(TAG, "no root access, returning null password value");
			return null;
		} else {
			return password;
		}
	}

	public static String getPasswordFromFile(Context c, String ssid)
			throws PasswordNotFoundException {
		HashMap<String, String> networkConfigs = getNetworkConfigs(c, ssid);
		if (networkConfigs == null) {
			return null;
		}

		String pw = null;

		String key_mgmt = networkConfigs.get("key_mgmt");
		if (key_mgmt != null) {
			if (key_mgmt.equals("WPA-PSK")) {
				// WPA protocol
				pw = networkConfigs.get("psk");
				if (pw != null) {
					return pw;
				} else {
					Log.e(TAG, "found WPA-PSK config with no PSK password");
					throw new PasswordNotFoundException(
							"WPA-PSK config with no PSK password");
				}
			} else if (key_mgmt.equals("NONE")) {
				// WEP or OPEN
				pw = networkConfigs.get("wep_key0");
				if (pw == null) {
					// OPEN
					return "";
				} else {
					// WEP
					return pw;
				}
			}
		} else {
			// wild shot, try and grab psk value
			Log.i(TAG, "did not find key_mgmt value, guessing pw");

			pw = networkConfigs.get("psk");
			if (pw != null) {
				return pw;
			}
		}

		Log.e(TAG, "found no password from file");
		throw new PasswordNotFoundException("found no password from file");
	}

	private static HashMap<String, String> getNetworkConfigs(Context c,
			String ssid) {
		String fileContents = getFile(c);
		if (fileContents == null || fileContents.equals("")) {
			return null;
		}

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

	public static String getFile(Context c) {
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
			return null;
		} catch (IOException e) {
			return null;
		}

		return content;
	}

	public static String getFileThenDelete(Context c) {
		String content = getFile(c);

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

					@SuppressWarnings("deprecation")
					String currUid = osRes.readLine();
					boolean exitSu = false;
					if (null == currUid) {
						retval = false;
						exitSu = false;
						Log.i(TAG, "Can't get root access or denied by user");
					} else if (true == currUid.contains("uid=0")) {
						retval = true;
						exitSu = true;
						Log.i(TAG, "Root access granted");
					} else {
						retval = false;
						exitSu = true;
						Log.i(TAG, "Root access rejected: " + currUid);
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
				Log.i(TAG, "Root access rejected [" + e.getClass().getName()
						+ "] : " + e.getMessage());
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

					// process can get blocked if the output stream is full,
					// solution is to continually read from the process's input
					// stream to ensure it doesn't block
					BufferedInputStream in = new BufferedInputStream(
							suProcess.getInputStream());
					byte[] bytes = new byte[4096];
					while (in.read(bytes) != -1) {
						Log.e(TAG, "ughhhhh");
					}

					int suProcessRetval = suProcess.waitFor();
					if (255 != suProcessRetval) {
						// Root access granted
						retval = true;
					} else {
						// Root access denied
						retval = false;
					}
				}
			} catch (IOException ex) {
				Log.w(TAG, "Can't get root access");
			} catch (SecurityException ex) {
				Log.w(TAG, "Can't get root access");
			} catch (Exception ex) {
				Log.w(TAG, "Error executing internal operation");
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
