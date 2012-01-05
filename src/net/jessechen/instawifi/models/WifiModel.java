package net.jessechen.instawifi.models;


public class WifiModel {
	private String SSID;
	private String password;
	private String protocol;
	
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
