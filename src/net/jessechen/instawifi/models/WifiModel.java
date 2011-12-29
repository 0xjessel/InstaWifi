package net.jessechen.instawifi.models;

public class WifiModel {
	private String protocol;
	private String SSID;
	private String password;
	
	public WifiModel(String ssid, String pw, String pt) {
		protocol = pt;
		SSID = ssid;
		password = pw;
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
