package org.jboss.mass.mat.main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import org.jboss.mass.exception.MassException;
import org.jboss.mass.mat.xml.config.*;


public class HtmlFile {

	private File dataDirPath;
	private File reportDirPath;
	private File indexHtmlById;
	private File indexHtml;
	private String id;
	public HtmlFile(String id) {
		// TODO Auto-generated constructor stub
		this.id = id;
	}
	
	public HtmlFile() {};
	
	public void setDataDirPath(String dataDirPathName) {
		dataDirPath = new File(dataDirPathName);
		
		//create the directory path
		dataDirPath.mkdir();
	}
	
	public void setReportDirPath(String reportDirPathName) {
		reportDirPath = new File(reportDirPathName);

		//create the directory path
		reportDirPath.mkdir();
	}
	
	public String getId() {
		return id;
	}
	
	
	public void createIndexHtmlById(String fullPathName) throws MassException {
		indexHtmlById = new File(fullPathName);		
		String content = createIndexHtmlContentById();
		try {
			FileOutputStream outStream = new FileOutputStream(indexHtmlById);
			outStream.write(content.getBytes());
			outStream.close();
		} catch(FileNotFoundException e) {
			throw new MassException(fullPathName + " creation error.", e);
		} catch(IOException e) {
			throw new MassException(fullPathName + " creation error.", e);
		}
	}
	
	public void createHeadingIndexHtml(String fullPathName, List<Application> apps, List<Server> servers) throws MassException {
		indexHtml = new File(fullPathName);
		String content = createHeadingIndexHtmlContent(apps, servers);
		try {
			FileOutputStream outStream = new FileOutputStream(indexHtml);
			outStream.write(content.getBytes());
			outStream.close();
		} catch(FileNotFoundException e) {
			throw new MassException(fullPathName + " creation error.", e);
		} catch(IOException e) {
			throw new MassException(fullPathName + " creation error.", e);
		}
	}

	private String createIndexHtmlContentById() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		String currentDateStr = sdf.format(new Date());
		
		String htmlContent = "<html>" +
						"<head><link ref=\"stylesheet\" type=\"text/css\" href=\"../style.css\"></head>" +
						"<body>" +
						"<div class=\"header\">" +
						"<div class=\"header_left\">" +
						"<a href=\"http://www.jboss.org/mass/MAT.html\">" +
						"<img src=\"http://www.jboss.org/theme/images/banners/mass-banner.png\" border=\"0\" width=\"240\" height=\"44\" />" +
						"</a>" +
						"</div>" +
						"<div class=\"header_right\">" +
						"WebLogic to JBoss Conversion Estimation Tool - " + currentDateStr + "</div>" +
						"</div>" +
						"<h1>" + id + "</h1>" +
						"<a href=\"summary.html\">Applications Summary</a>" +
						"<br>" +
						"<a href=\"dependencySummary.html\">Class Dependency Summary</a>" +
						"<br>" +
						"<a href=\"dependencies.html\">Class Dependencies</a>" +	
						"<br>" +
						"<a href=\"metrics.html\">Class Metrics</a>" +			
						"<br>" +
						"<a href=\"dependencyGraph.html\">Dependency Graph</a>" +	
						"<br>" +
						"</body></html>";
		
		return htmlContent;
		
	}
	
	private String createHeadingIndexHtmlContent(List<Application> apps, List<Server> servers) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		String currentDateStr = sdf.format(new Date());
		
		
		String htmlContent = "<html>" +
						"<head><link ref=\"stylesheet\" type=\"text/css\" href=\"../style.css\"></head>" +
						"<body>" +
						"<div class=\"header\">" +
						"<div class=\"header_left\">" +
						"<a href=\"http://www.jboss.org/mass/MAT.html\">" +
						"<img src=\"http://www.jboss.org/theme/images/banners/mass-banner.png\" border=\"0\" width=\"240\" height=\"44\" />" +
						"</a>" +
						"</div>" +
						"<div class=\"header_right\">" +
						"WebLogic to JBoss Conversion Estimation Tool - " + currentDateStr + "</div>" +
						"</div>" +
						"<h2>Servers</h2>";
		

		String htmlServers = "";
		for(int i = 0 ; i < servers.size() ; i++) {
			String srvId = ((Server)servers.get(i)).getId();
			htmlServers = htmlServers + 
						"<a href=\"" + srvId + "/index.html\">" + srvId + "</a><br>";
		}
		
		String htmlApplications = "";
		for(int i = 0 ; i < apps.size() ; i++) {
			String appId = ((Application)apps.get(i)).getId();
			htmlApplications = htmlApplications + 
						"<a href=\"" + appId + "/index.html\">" + appId + "</a><br>";
		}
		
		htmlContent = htmlContent + "<br>" + htmlServers +
		              "<h2>Applications</h2><br>" + htmlApplications +
		              "</body></html>";

		return htmlContent;		
	}
	
	

}
