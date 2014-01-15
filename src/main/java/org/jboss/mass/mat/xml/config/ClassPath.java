package org.jboss.mass.mat.xml.config;

import java.util.List;

public class ClassPath {
	
	private String id;
	private List<Directory> entries;
	
	public void ClassPath(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public List<Directory> getDataContent() {
		return entries;
	}
	

}
