package org.jboss.mass.mat.xml.dependencyscriptor;

public class Outbound {

	private String type;
	private String confirmed;
	private String value;
	
	public Outbound(String type, String confirmed) {
		this.type = type;
		this.confirmed = confirmed;
	}
	
	public String getType() {
		return type;
	}
	
	public String getConfirmed() {
		return confirmed;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
}
