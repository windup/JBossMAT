package org.jboss.mass.mat.xml.config;

import java.util.List;

public class Application {
	
	private String id;
	private String codebase;
	private List<ClassPath> entries;
	
	public void Application(String id) {
		this.id = id;
	}
	
	public String getId() {
		return id;
	}
	
	public String getCodebase() {
		return codebase;
	}
	
	public List<ClassPath> getDataContent() {
		return entries;
	}

}
