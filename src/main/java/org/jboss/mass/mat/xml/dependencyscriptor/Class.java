package org.jboss.mass.mat.xml.dependencyscriptor;

import java.util.List;

public class Class {
	
	private String confirmed;
	private String name;
	private List<Feature> features;
	private List<Inbound> inbounds;
	private List<Outbound> outbounds;
	
	public Class(String confirmed) {
		this.confirmed = confirmed;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<Feature> getFeatures() {
		return features;
	}
	
	public List<Inbound> getInbounds() {
		return inbounds;
	}
	
	public List<Outbound> getOutbounds() {
		return outbounds;
	}
	

}
