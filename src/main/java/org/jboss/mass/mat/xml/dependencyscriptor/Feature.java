package org.jboss.mass.mat.xml.dependencyscriptor;

import java.util.List;

public class Feature {
	
	private String confirmed;
	private String name;
	private List<Inbound> inbounds;
	private List<Outbound> outbounds;
	
	public Feature(String confirmed) {
		this.confirmed = confirmed;
	}
	
	public String getConfirmed() {
		return this.confirmed;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<Inbound> getInbounds() {
		return inbounds;
	}
	
	public List<Outbound> getOutbounds() {
		return outbounds;
	}
	
}
