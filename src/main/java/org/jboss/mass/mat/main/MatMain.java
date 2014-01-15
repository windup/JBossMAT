package org.jboss.mass.mat.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jboss.mass.exception.MassException;
import org.jboss.mass.mat.xml.config.*;
import com.thoughtworks.xstream.XStream;

/*
 * JBoss, Home of Professional Open Source
 * Copyright 2008, Red Hat, Inc., and others contributors as indicated
 * by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 * This copyrighted material is made available to anyone wishing to use,
 * modify, copy, or redistribute it subject to the terms and conditions
 * of the GNU Lesser General Public License, v. 2.1.
 * This program is distributed in the hope that it will be useful, but
 WITHOUT A
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 FITNESS FOR A
 * PARTICULAR PURPOSE.  See the GNU Lesser General Public License for
 more details.
 * You should have received a copy of the GNU Lesser General Public
 License,
 * v.2.1 along with this distribution; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA  02110-1301, USA.
 */

public class MatMain {

	/**
	 * @param args
	 */
	public final String env_vars[] = { Constants.MAT_HOME,
			Constants.JBOSS_HOME, Constants.WL_HOME, Constants.JAVA_HOME };
	public static final Logger logger = Logger.getLogger("Main");
	public static Config configXml;

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		MatMain mm = new MatMain();
		try {
			mm.processMain();
		} catch(MassException e) {
			e.printStackTrace();
			System.out.println(e.getTrace());
			System.exit(-1);
		}

	}

	private void processMain() throws MassException{

		if (!isEnvsAllSet()) {
			System.out
					.println("Error: Environment variable either MAT_HOME, JBOSS_HOME, WL_HOME, JAVA_HOME is/are not set.");
			System.exit(-1);
		}

		// Construct a map for the environment variable
		Map<String, String> envMap = constructEnvMap();

		// Construct a map for the directory path
		Map<String, String> pathMap = constructMATPath(envMap);
		
		prepareReportDir(pathMap);
		prepareForEachApplication(pathMap);
		DependenciesScript ds = new DependenciesScript(configXml, pathMap);
		ds.run();  //run dependecies script and generate html report for dependencies

	}

	// Prepare for every application in weblogic.
	private void prepareForEachApplication(Map pathMap) throws MassException {
	
		configXml = createObjForConfigXML();
		List htmlListForAppId = new ArrayList();
		for(int i = 0 ; i < configXml.getApplications().getDataContent().size() ; i++) {
			Application app = configXml.getApplications().getDataContent().get(i);
			//generate html file
			HtmlFile html = new HtmlFile(app.getId());
			html.setDataDirPath(Utility.constructPath((String)pathMap.get("DATA_DIR"), app.getId()));
			html.setReportDirPath(Utility.constructPath((String)pathMap.get("REPORTS_DIR"), app.getId()));
			html.createIndexHtmlById(Utility.constructPath((String)pathMap.get("REPORTS_DIR"), app.getId(), "index.html"));
			htmlListForAppId.add(html);
			
		}
		
		HtmlFile headingIndexHtml = new HtmlFile();
		headingIndexHtml.createHeadingIndexHtml(Utility.constructPath((String)pathMap.get("REPORTS_DIR"), "index.html"), configXml.getApplications().getDataContent(), configXml.getServers().getDataContent());
//		headingIndexHtml.createHeadingIndexHtmlContent(htmlListForAppId);
		
		System.out.println("Program end.");
	}
	
	//
	// Create a binded java object of the config.xml file.
	private Config createObjForConfigXML() throws MassException {
		XStream xstream = new XStream();
		
		xstream.alias("config", Config.class);
		xstream.alias("servers", Servers.class);
		xstream.alias("applications", Applications.class);
		xstream.alias("options", Options.class);   
		
		xstream.alias("server", Server.class);  //weblogic-version
		xstream.alias("mbeans", MBeans.class);
		xstream.alias("mbean", MBean.class);
		
		xstream.alias("application", Application.class);
		xstream.alias("directory", Directory.class);
		xstream.alias("classpath", ClassPath.class);
		
		xstream.addImplicitCollection(Application.class, "entries");
		xstream.addImplicitCollection(ClassPath.class, "entries");
		xstream.addImplicitCollection(Applications.class, "entries");
		xstream.addImplicitCollection(MBeans.class, "entries");
		xstream.addImplicitCollection(Servers.class, "entries");
		
		xstream.aliasField("weblogic-version", Server.class, "weblogic_version");
		xstream.aliasField("run-dependency-analysis", Options.class, "run_dependency_analysis");
		xstream.aliasField("run-server-analysis", Options.class, "run_server_analysis");
		
		xstream.useAttributeFor(ClassPath.class, "id");
		xstream.useAttributeFor(Application.class, "id");
		xstream.useAttributeFor(MBean.class, "type");
		xstream.useAttributeFor(MBean.class, "label");
		xstream.useAttributeFor(Server.class, "id");
		xstream.useAttributeFor(Directory.class, "path");
		xstream.useAttributeFor(Directory.class, "recurse");
		
		String configXMLPath = getConfigXml();
		try {
			FileInputStream instream = new FileInputStream(configXMLPath);
			Config config = (Config)xstream.fromXML(instream);
			instream.close();
			
			return config;
		} catch(FileNotFoundException e) {
			throw new MassException("XML File Path=" + configXMLPath + " not found", e);
		} catch(IOException e) {
			throw new MassException("XML File Path=" + configXMLPath + " input stream close error." , e);
		}

	}
	
	// Prepare the Report Directory
	private void prepareReportDir(Map pathMap) throws MassException {
		File reportDir = new File((String) pathMap.get("REPORTS_DIR"));
		reportDir.delete(); // Delete the directory
		reportDir.mkdir(); // re-create the directory
		

		try {
			FileUtils.copyFile(new File(Utility.constructPath((String) pathMap
					.get("ETC_DIR"), "style.css")), new File(Utility.constructPath(
							(String) pathMap.get("REPORTS_DIR"), "style.css")));
		} catch(IOException e) {
			throw new MassException(Utility.constructPath((String) pathMap.get("ETC_DIR"), "style.css") + " copy error.", e);
		}
		
		try {
			FileUtils.copyFile(new File(Utility.constructPath((String) pathMap
					.get("ETC_DIR"), "main.js")), new File(Utility.constructPath(
							(String) pathMap.get("REPORTS_DIR"), "main.js")));
		} catch(IOException e) {
			throw new MassException(Utility.constructPath((String) pathMap.get("ETC_DIR"), "main.js") + " copy error.", e);
		}
		
		try {
			FileUtils.copyFile(
					new File(Utility.constructPath((String) pathMap
							.get("DEPENDENCY_FINDER_HOME"), "etc",
							"dependency.style.css")), new File(Utility.constructPath(
									(String) pathMap.get("REPORTS_DIR"),"dependency.style.css")));
		} catch(IOException e) {
			throw new MassException(Utility.constructPath((String) pathMap.get("DEPENDENCY_FINDER_HOME"), "etc", "dependency.style.css") + " copy error.",e);
		}

	}

	// Construct MAT directory path from the MAT_HOME environment variable
	private Map<String, String> constructMATPath(Map<String, String> envMap) {

		Map<String, String> pathMap = new HashMap<String, String>();

		pathMap.put("DEPENDENCY_FINDER_HOME", Utility.constructPath(envMap
				.get("MAT_HOME"), "tools", "depfinder"));
		pathMap.put("DATA_DIR", Utility.constructPath(envMap.get("MAT_HOME"), "data"));
		pathMap.put("REPORTS_DIR", Utility.constructPath(envMap.get("MAT_HOME"),
				"reports"));
		pathMap.put("ETC_DIR", Utility.constructPath(envMap.get("MAT_HOME"), "etc"));
		pathMap.put("CONFIG_XML", Utility.constructPath(envMap.get("MAT_HOME"), "conf",
				"config.xml"));

		return pathMap;

	}

	// Construct a mapping of the environment variable.
	private Map<String, String> constructEnvMap() {
		Map<String, String> envMap = new HashMap<String, String>();

		for (int i = 0; i < env_vars.length; i++) {
			envMap.put(env_vars[i], System.getenv(env_vars[i]));
		}

		return envMap;
	}

	private boolean isEnvsAllSet() {

		for (int i = 0; i < env_vars.length; i++) {
//			System.out.println("env=" + env_vars[i]);
			String env_value = System.getenv(env_vars[i]);
			if (env_value == null) {
				return false;
			}
		}

		return true;
	}
	
	private String getConfigXml() {
//		logger.debug("config xml is ${System.getProperty(" + config.xml", "${USER_DIR}${SEP}conf${SEP}config.xml")}")
		String USER_DIR = System.getProperty("user.dir");
		String SEP = File.separator;
		
//		System.getProperty("config.xml", "${USER_DIR}${SEP}conf${SEP}config.xml");
		return System.getProperty("config.xml", USER_DIR + SEP + "conf" + SEP + "config.xml");
//		return System.getProperty("config.xml", "/home/benson/Project/mass/testing/config.xml");  // for testing only
	}

}
