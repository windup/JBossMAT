package org.jboss.mass.mat.xml.config;

public class Server {
	
	private String id;
	private String protocol;
	private String host;
	private String port;
	private String username;
	private String password;
	private String weblogic_version;
	private MBeans mbeans;
	
	public void Server(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public String getProtocol() {
		return protocol;
	}
	
	public String getHost() {
		return host;
	}
	
	public String getPort() {
		return port;
	}
	
	public String getUsername() {
		return username;
	}
	
	public String getPassword() {
		return password;
	}
	
	public String getWeblogic_version() {
		return weblogic_version;
	}
	
	public MBeans getMbeans() {
		return mbeans;
	}

}
