package net.jessechen.instawifi.models;

public class WifiModel {
	private String BSSID;
	private String SSID;
	private String password;
	private String protocol;
	
	public WifiModel(String bssid, String ssid, String pw, String pt) {
		BSSID = bssid;
		protocol = pt;
		SSID = ssid;
		password = pw;
	}
	
	public WifiModel(String ssid, String pw, String pt) {
		BSSID = null;
		protocol = pt;
		SSID = ssid;
		password = pw;
	}
	
	public String getBSSID() {
		return BSSID;
	}
	
	public String getProtocol() {
		return protocol;
	}

	public String getSSID() {
		return SSID;
	}

	public String getPassword() {
		return password;
	}
}
