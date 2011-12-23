package net.jessechen.instawifi.models;

public class WifiModel {
	private String protocol;
	private String SSID;
	private String password;
	
	public WifiModel(String id, String pw, String pt) {
		protocol = pt;
		SSID = id;
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
