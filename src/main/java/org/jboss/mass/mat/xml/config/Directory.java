package org.jboss.mass.mat.xml.config;

public class Directory {
	
	private String path;
	private String recurse;
	
	public void Directory(String path, String recurse) {
		this.path = path;
		this.recurse = recurse;
	}
	
	public String getPath() {
		return path;
	}
	
	public String getRecurse() {
		return recurse;
	}
}