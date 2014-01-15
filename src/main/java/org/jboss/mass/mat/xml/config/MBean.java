package org.jboss.mass.mat.xml.config;

public class MBean {

	private String type;
	private String label;
	
	public void MBean(String type, String label) {
		this.type = type;
		this.label = label;
	}
	
	public String getType() {
		return type;
	}
	
	public String getLabel() {
		return label;
	}
}
