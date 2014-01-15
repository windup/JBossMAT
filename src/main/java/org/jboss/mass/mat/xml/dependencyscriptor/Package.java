package org.jboss.mass.mat.xml.dependencyscriptor;

import java.util.List;

public class Package {

	public String confirmed;
	public String name;
	public List<Class> entries;
	
	public Package(String confirmed) {
		this.confirmed = confirmed;
	}
	
	public String getName() {
		return this.name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public List<Class> getClasses() {
		return entries;
	}
}
