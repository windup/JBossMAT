package org.jboss.mass.mat.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jboss.mass.exception.MassException;
import org.jboss.mass.mat.xml.config.*;
import org.jboss.mass.mat.xml.dependencyscriptor.Dependencies;
import org.jboss.mass.mat.xml.dependencyscriptor.Feature;
import org.jboss.mass.mat.xml.dependencyscriptor.Inbound;
import org.jboss.mass.mat.xml.dependencyscriptor.InboundConverter;
import org.jboss.mass.mat.xml.dependencyscriptor.Outbound;
import org.jboss.mass.mat.xml.dependencyscriptor.OutboundConverter;
import org.jboss.mass.mat.xml.dependencyscriptor.Package;
import org.jboss.mass.mat.xml.dependencyscriptor.Class;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

public class DependenciesScript {

	public static final Logger logger = Logger.getLogger(DependenciesScript.class);
	private Config configXml;
	private Map pathMap;
	private Map app_deps;   //a hash of all the results we obtain, key=application id, value=hash of class loading results
	public DependenciesScript(Config configXml, Map pathMap) {
		// TODO Auto-generated constructor stub
		this.configXml = configXml;
		this.pathMap = pathMap;
	}
	
	public void run() {
		
		String rda = configXml.getOptions().getRun_dependency_analysis();
		boolean rdaBoo = rda == null || !"false".equalsIgnoreCase(rda);
		
		if(!rdaBoo) {
			logger.info("Skipping dependency analysis");
		} else {
			logger.info("Crunching Dependencies data.....");
			try {
				boolean result = runClassMetrics();
				if(!result) {
					System.out.println("A non fatal error occured during running Class Metrics Analysis. Please review the log");
				}
			} catch(Throwable e) {
				System.err.println ("Something went wrong while running Class Metrics Analysis. Please see the log file for more details.");
				e.printStackTrace();
				logger.fatal ("Error While running class Metrics Analysis");
				logger.fatal(e);
			}
			
			try {
				runClassPathAndDependancyAnalysis();
				generateMetricsAndDependencies();
				//generateDependencySummaries();
			} catch(Throwable e) {
				e.printStackTrace();
			}
		}
	}
	
	private boolean runClassPathAndDependancyAnalysis() throws MassException {
		
		Map<String, Map<String, Integer>> externals = new HashMap<String, Map<String, Integer>>();
		
		for(int i = 0 ; i < configXml.getApplications().getDataContent().size(); i++) {
			Application app = configXml.getApplications().getDataContent().get(i);
			String deXmlFile = Utility.constructPath((String)pathMap.get("DATA_DIR"), app.getId() + ".DependencyExtractor.xml");
			Dependencies dpdcy = createObjForDependcyScriptXML(deXmlFile);
			//XXXXX//
			List<Package> packageList = dpdcy.getPackages();
			for(int j = 0 ; j < packageList.size() ; j++) {
				Package pkg = (Package)packageList.get(j);
				List<Class> classList = pkg.getClasses();
				for(int k = 0 ; k < classList.size() ; k++) {
					Class cl = (Class)classList.get(k);
					List<Feature> featureList = cl.getFeatures();
					for(int m = 0 ; m < featureList.size() ; m++) {
						Feature fturs = (Feature)featureList.get(m);
						List<Outbound> outboundList = fturs.getOutbounds();
						for(int n = 0 ; n < outboundList.size() ; n++) {
							Outbound outbound = (Outbound)outboundList.get(n);
							if(outbound.getType() == "class") {
								String[] a = outbound.getValue().split("\\.");
								if(a.length > 3) {
									String key1 = a[0] + "." + a[1];
									String key2 = a[2] + "." + a[a.length - 1];
									if(key2.endsWith("[]")) {
										if(externals.containsKey(key1)) {
											if(externals.get(key1).containsKey(key2)) {
												Integer tmp = (Integer)externals.get(key1).get(key2) + 1;
												externals.get(key1).put(key2, tmp);
											} else {
												externals.get(key1).put(key2, 1);
											}
										} else {
											externals.put(key1, null);
											Map<String, Integer> tmp = new HashMap<String, Integer>();
											tmp.put(key2, 1);
											externals.put(key1, tmp);
										}
									}
								}
							}
						}
					}
				}
			}
			
			List<URL> urls = new ArrayList<URL>();
			List<ClassPath> cpList = app.getDataContent();
			for(int j = 0 ; j < cpList.size() ; j++) {
				ClassPath cp = (ClassPath)cpList.get(j);
				List<Directory> dirList = cp.getDataContent();
				for(int k = 0 ; k < dirList.size() ; k++) {
					Directory dir = (Directory)dirList.get(k);
					String path = deTokenize(dir.getPath());
					File f = new File(path);
					try {
						urls.add(f.toURL());
					} catch (MalformedURLException e) {
						// TODO Auto-generated catch block
						throw new MassException("URL path=" + path + " not found/error", e);
					}
					
					logger.debug("absolute Path = " + f.getAbsolutePath());
					int excIdx = f.getAbsolutePath().indexOf("!");
					logger.debug("excIdx = " + excIdx);
					if(f.isDirectory()) {
						File[] filenames = f.listFiles();
						for(int l = 0 ; l < filenames.length ; l++) {
							if(filenames[i].getAbsolutePath().endsWith(".jar")) {
								try {
									urls.add(filenames[i].toURL());
								} catch (MalformedURLException e) {
									// TODO Auto-generated catch block
									throw new MassException("URL jar file path=" + path + " not found/error", e);
								}
								
							} 
						}
						String recurse = dir.getRecurse();
						if(recurse == null) {
							for(int m = 0 ; m < filenames.length ; m++) {
								if(filenames[m].isDirectory()) {
									//recurse this subdirectory
									try {
										urls.add(filenames[m].toURL());
									} catch(MalformedURLException e) {
										throw new MassException("URL subdirectory path=" + path + " not found/error", e);
									}
									File[] subDirFilenames = filenames[m].listFiles();
									for(int n = 0 ; n < subDirFilenames.length ; n++) {
										if(subDirFilenames[n].getAbsolutePath().endsWith(".jar")) {
											try {
												urls.add(subDirFilenames[n].toURL());
											} catch(MalformedURLException e) {
												throw new MassException("URL sub-subdirectory path=" + path + " not found/error", e);											
											}
										}
									}								
								}
							}
						}
					} else if(f.exists() && (f.getName().endsWith(".ear") || f.getName().endsWith(".war"))) {
						de.schlichtherle.io.File file = new de.schlichtherle.io.File(f);
						File tmpDir = new File(new File(System.getProperty("java.io.tmpdir")), f.getName());
						if(f.getName().endsWith(".ear")) {
							File libDir = new File(tmpDir, "lib");
							File[] libDirList = libDir.listFiles();
							for(int m = 0 ; m < libDirList.length ; m++) {
								File tmpFile = libDirList[m];
								if(tmpFile.getName().endsWith(".jar")) {
									try {
										urls.add(tmpFile.toURL());
									} catch(MalformedURLException e) {
										throw new MassException("URL sub-subdirectory path=" + tmpFile.getPath() + " not found/error", e);																					
									}
								}
							}
						} else if(f.getName().endsWith(".war")) {
							File web_inf_lib = new File(tmpDir, "WEB-INF/lib");
							File[] webinflibList = web_inf_lib.listFiles();
							for(int m = 0 ; m < webinflibList.length ; m++) {
								File tmpFile = webinflibList[m];
								if(tmpFile.getName().endsWith(".jar")) {
									try {
										urls.add(tmpFile.toURL());
									} catch(MalformedURLException e) {
										throw new MassException("URL sub-subdirectory path=" + tmpFile.getPath() + " not found/error", e);																															
									}
								}
							}
						}
					} else if(!f.exists() && excIdx > 0 && (f.getAbsolutePath().substring(0, excIdx).endsWith(".war") || f.getAbsolutePath().substring(0, excIdx).endsWith(".ear"))) {
						de.schlichtherle.io.File file = new de.schlichtherle.io.File(f.getAbsolutePath().substring(0, excIdx));
						File tmpDir = new File(new File(System.getProperty("java.io.tmpdir")), file.getName());
						if(file.getName().endsWith(".ear")) {
							File libDir = new File(tmpDir, "lib");
							File[] libDirList = libDir.listFiles();
							for(int m = 0 ; m < libDirList.length ; m++) {
								File tmpFile = libDirList[m];
								if(tmpFile.getName().endsWith(".jar")) {
									try {
										urls.add(tmpFile.toURL());
									} catch(MalformedURLException e) {
										throw new MassException("URL sub-subdirectory path=" + tmpFile.getPath() + " not found/error", e);																															
									}
								} 
							}
						} else if(file.getName().endsWith(".war")) {
							File webinflibDir = new File(tmpDir, "WEB-INF/lib");
							File[] webinflibdirList = webinflibDir.listFiles();
							for(int m = 0 ; m < webinflibdirList.length ; m++) {
								File tmpFile = webinflibdirList[m];
								if(tmpFile.getName().endsWith(".jar")) {
									try {
										urls.add(tmpFile.toURL());
									} catch(MalformedURLException e) {
										throw new MassException("URL sub-subdirectory path=" + tmpFile.getPath() + " not found/error", e);																															
									}									
								}
							}
						}
						
					}														
				}
				logger.debug("*************************************************");
				logger.debug("built classpath URLs for cp.id=" + cp.getId());
				logger.debug("*************************************************");
				
				new File(Utility.constructPath((String)pathMap.get("REPORTS_DIR"), "classpaths")).mkdir();
				
				File cpFile = new File(Utility.constructPath((String)pathMap.get("REPORTS_DIR"), "classpaths", cp.getId() + ".html"));
				try {
					BufferedWriter cpWriter = new BufferedWriter(new FileWriter(cpFile));
					generateClassPathHtml(cpWriter);
					for(int k = 0 ; k < urls.size() ; k++) {
						cpWriter.write(urls.get(k) + "<br/>");
					}
					cpWriter.write("</body></html>");   //make sure cpWriter is still open
					cpWriter.close();
				} catch(IOException e) {
					throw new MassException("File Path=" + cpFile.getAbsolutePath() + " input stream close error." , e);	
				}
				
				URLClassLoader urlclassloader = new URLClassLoader((URL[])urls.toArray());
				String fname = Utility.constructPath((String)pathMap.get("REPORTS_DIR"), app.getId() + "." + cp.getId() + ".xml");
				try {
					BufferedWriter fnameWriter = new BufferedWriter(new FileWriter(new File(fname)));
					String output_xml = "<output>\n";
					output_xml = output_xml + "\n<application id=\"" + app.getId() + "\">\n";
					Set keyset = externals.keySet();
					Iterator keyIterator = keyset.iterator();
					while(keyIterator.hasNext()) {
						String key = (String)keyIterator.next();
						Integer sum = sumMapValue(externals.get(key));
						output_xml = output_xml + "\n<package name=\"" + key + "\", dependencycount=\"" + sum.toString() + "\">\n";
						Map map = externals.get(key);
						Set keySet1 = map.keySet();
						Iterator keyIterator1 = keySet1.iterator();
						while(keyIterator1.hasNext()) {
							String sKey = (String)keyIterator1.next();
							String classname = key + "." + sKey;
							boolean classloaded = false;
							Throwable classloadingexception = null;
							java.lang.Class _theclass = null;
							try {
								urlclassloader.loadClass(classname);
								classloaded = true;
							} catch(Throwable e) {
								classloadingexception = e;
								classloaded = false;
							}
							
							URL cpurl = null;
							String cpdir = null;
							if(classloaded) {
								try {
									cpurl = Utility.getClassLocation(_theclass);
								} catch(Throwable e) {
									cpurl = null;
								}
								if(cpurl != null && cpurl.toString().endsWith(".class")){
									try{
										int idx = cpurl.toString().replaceAll(System.getProperty("file.separator"), ".").indexOf(_theclass.getName());
										if(idx > -1)
											cpdir = cpurl.toString().substring(0,idx);
									}catch(Throwable e){}
								}
							}
							output_xml = output_xml + "<class name=\"" + classname + "\", dependencycount=\"" + map.get(sKey) + "\", classloaded=\"" + classloaded + "\", classpath-url=\"" + cpurl + "\", classpath-dir=\"" + cpdir + "\", classloadingexception\"" + classloadingexception + "\", shortname=\"" + sKey + "\"/>\n"; 
							output_xml = output_xml + "</package>\n";
						}
					}
					output_xml = output_xml + "</application>\n";
					output_xml = output_xml + "</output>\n";
				} catch(IOException e) {
					throw new MassException("File Path=" + fname + " input stream close error." , e);
				}
				
			}

		}
		//we are done with this application, so add it to the master hash
		app_deps = externals;
		return true;
		
	}
	
	private Integer sumMapValue(Map map) {
		Integer sum = 0;
		Set keySet = map.keySet();
		Iterator keyIterator = keySet.iterator();
		while(keyIterator.hasNext()) {
			String key = (String)keyIterator.next();
			sum = sum + (Integer)map.get(key);
		}
		
		return sum;
	}
	
	
	private org.jboss.mass.mat.xml.dependencyscriptor.Dependencies createObjForDependcyScriptXML(String deXmlFile) throws MassException {
		
		XStream xstream = new XStream(new DomDriver());
		
		xstream.alias("dependencies", Dependencies.class);
		xstream.alias("package", org.jboss.mass.mat.xml.dependencyscriptor.Package.class);
		xstream.alias("class", org.jboss.mass.mat.xml.dependencyscriptor.Class.class);
		xstream.alias("feature", Feature.class);
		xstream.alias("inbound", Inbound.class);
		xstream.alias("outbound", Outbound.class);
		
		xstream.registerConverter(new InboundConverter());
		xstream.registerConverter(new OutboundConverter());
		
		xstream.aliasField("inbound", org.jboss.mass.mat.xml.dependencyscriptor.Class.class, "inbounds");
		xstream.aliasField("outbound", org.jboss.mass.mat.xml.dependencyscriptor.Class.class, "outbounds");
		
		xstream.addImplicitCollection(Dependencies.class, "entries");
		xstream.addImplicitCollection(Package.class, "entries");
		xstream.addImplicitCollection(org.jboss.mass.mat.xml.dependencyscriptor.Class.class, "features", Feature.class);
		xstream.addImplicitCollection(org.jboss.mass.mat.xml.dependencyscriptor.Class.class, "inbounds", Inbound.class);
		xstream.addImplicitCollection(org.jboss.mass.mat.xml.dependencyscriptor.Class.class, "outbounds", Outbound.class);		
		xstream.addImplicitCollection(Feature.class, "inbounds", Inbound.class);
		xstream.addImplicitCollection(Feature.class, "outbounds", Outbound.class);
		
		xstream.useAttributeFor(Package.class, "confirmed");
		xstream.useAttributeFor(org.jboss.mass.mat.xml.dependencyscriptor.Class.class, "confirmed");
		xstream.useAttributeFor(Feature.class, "confirmed");
		xstream.useAttributeFor(Inbound.class, "type");
		xstream.useAttributeFor(Inbound.class, "confirmed");
		xstream.useAttributeFor(Outbound.class, "type");
		xstream.useAttributeFor(Outbound.class, "confirmed");
		
		try {
			FileInputStream instream = new FileInputStream(deXmlFile);
			Dependencies dpdcy = (org.jboss.mass.mat.xml.dependencyscriptor.Dependencies)xstream.fromXML(instream);
			instream.close();
			
			return dpdcy;
		} catch(FileNotFoundException e) {
			throw new MassException("XML File Path=" + deXmlFile + " not found", e);
		} catch(IOException e) {
			throw new MassException("XML File Path=" + deXmlFile + " input stream close error." , e);
		}
		
	}
	
	
	
	
	private boolean runClassMetrics() throws MassException {
		boolean result = false;
		
		for(int i = 0 ; i < configXml.getApplications().getDataContent().size() ; i++) {
			Application app = configXml.getApplications().getDataContent().get(i);
			
			String codebaseStr = deTokenize(app.getCodebase());
			File codebase = expandCodebase(new File(codebaseStr));
			String absolutePath = codebase.getAbsolutePath();
			String output = Utility.constructPath((String)pathMap.get("DATA_DIR"), app.getId() + ".ClassMetrics");
			if(Utility.isWindows()) {
				String cmd = Utility.constructPath((String)pathMap.get("DEPENDENCY_FINDER_HOME"), "bin", "ClassMetrics.cmd");
				Utility.runCommand(cmd + " \"" + absolutePath + "\"" + " -out " + output, null);
			} else {
				String cmd = Utility.constructPath((String)pathMap.get("DEPENDENCY_FINDER_HOME"), "bin", "ClassMetrics");
				Utility.runCommand(cmd + " " + absolutePath + " -out " + output, null);
			}
			
			try {
				FileInputStream instream = new FileInputStream(output);
				byte[] data = new byte[instream.available()];
				instream.read(data);
				String dataStr = new String(data);
				if(dataStr.indexOf("0 class(es)") == 0) {
					logger.error("!!! No classes found for codebase ${codebase.absolutePath} - check your configuration to ensure this path exists, it is readable, and that it contains some classes");
					return false;
				}				
			} catch(FileNotFoundException e) {
				throw new MassException("File Path=" + output + " not found", e);
			} catch(IOException e) {
				throw new MassException("File Path=" + output + " input stream close error." , e);				
			}	
			
			String dpdcyExtrtr = Utility.constructPath((String)pathMap.get("DATA_DIR"), app.getId() + ".DependencyExtractor.xml");
			if(Utility.isWindows()) {
				String cmd = Utility.constructPath((String)pathMap.get("DEPENDENCY_FINDER_HOME"), "bin", "DependencyExtractor.cmd");
				Utility.runCommand(cmd + " \"" + absolutePath + "\"" + " -xml -out " + "\"" + output + "\"", null);
			} else {
				String cmd = Utility.constructPath((String)pathMap.get("DEPENDENCY_FINDER_HOME"), "bin" + "DependencyExtractor");
				Utility.runCommand(cmd + " " + absolutePath + " -xml -out " + output, null);
			}
			
			File dpGraphFile = new File(Utility.constructPath((String)pathMap.get("REPORTS_DIR"), app.getId(), "dependencyGraph.html"));
			try {
				BufferedWriter dpGraphWriter = new BufferedWriter(new FileWriter(dpGraphFile, true));
				generateDependencyGraphHtml(dpGraphWriter);				
				if(Utility.isWindows()) {
					String cmd = Utility.constructPath((String)pathMap.get("DEPENDENCY_FINDER_NAME"), "bin", "DependentsToHTML.cmd");
					String outputPath = Utility.constructPath((String)pathMap.get("DATA_DIR"), app.getId() + ".DependencyExtractor.xml");
					Utility.runCommand("\"" + cmd + "\"" + " -in " + "\"" + outputPath + "\"", dpGraphWriter);
				} else {
					String cmd = Utility.constructPath((String)pathMap.get("DEPENDENCY_FINDER_NAME"), "bin", "DependentsToHTML");
					String outputPath = Utility.constructPath((String)pathMap.get("DATA_DIR"), app.getId() + ".DependencyExtractor.xml");
					Utility.runCommand(cmd + " -in " + outputPath, dpGraphWriter);
				}
				dpGraphWriter.write("</body></html>");   //make sure dpGrapWriter is still open
				dpGraphWriter.close();
			} catch(IOException e) {
				throw new MassException("File Path=" + dpGraphFile.getAbsolutePath() + " input stream close error." , e);	
			}
		}
		
		
		
		return result;
	}
	
	private String deTokenize(String codebase) {
		String token[] = System.getProperty("java.version").split("\\.");
		token[0] = token[0] + ".";
		
		String javaVersion = token[0] + token[1];
		Float floatJavaVersion = Float.parseFloat(javaVersion);
		while(	(codebase != null) && (codebase.indexOf("\\${") >= 0)	) {
			int s = codebase.indexOf("${");
			int e = codebase.substring(s).indexOf("}");
			String tok = codebase.substring(s+2,e);
			String var = null;
			if(floatJavaVersion >= 1.5) {
				var = System.getenv().get(tok);
				if(var == null) {
					var = "";
				}
				codebase = codebase.replace("${${tok}}", var);
			} else {
				var = System.getProperty(tok);
			}
			
		}
		
		return codebase;
	}
	
	private File expandCodebase(File codebaseDir) {
		
		int index = codebaseDir.getAbsolutePath().indexOf("!");
		if(!codebaseDir.exists() && index > 0 && (codebaseDir.getAbsolutePath().substring(0,index).endsWith(".war") || codebaseDir.getAbsolutePath().substring(0,index).endsWith(".ear"))){
			de.schlichtherle.io.File file = new de.schlichtherle.io.File(codebaseDir.getAbsolutePath().substring(0, index));
			File tmpDir = new File(new File(System.getProperty("java.io.tmpdir")), file.getName());
			if(!tmpDir.exists()) {
				if(file.copyAllTo(tmpDir)) {
					File innerDir = new File(tmpDir, codebaseDir.getAbsolutePath().substring(index+1));
					return innerDir;
				} else {
					return codebaseDir;
				}
			} else {
				File innerDir = new File(tmpDir, codebaseDir.getAbsolutePath().substring(index+1));
				return innerDir;
			}
		} else if(!codebaseDir.isDirectory()) {
			if(codebaseDir.getName().endsWith(".ear") || codebaseDir.getName().endsWith(".war")) {
				de.schlichtherle.io.File file = new de.schlichtherle.io.File(codebaseDir);
				File tmpDir = new File(new File(System.getProperty("java.io.tmpdir")), codebaseDir.getName());
				file.copyAllTo(tmpDir);
				return tmpDir;
			} else {
				return codebaseDir;
			}
		} else {
			return codebaseDir;
		}
	}
	
	private void generateDependencyGraphHtml(BufferedWriter dpGraphWriter) throws IOException {
	
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		String currentDateStr = sdf.format(new Date());
		
		String html = "<html>" +
		"<head><link ref=\"stylesheet\" type=\"text/css\" href=\"../style.css\"></head>" +
		"<body>" +
		"<body>" +
		"<div class=\"header\">" +
		"<div class=\"header_left\">" +
		"<a href=\"http://www.jboss.org/mass/MAT.html\">" +
		"<img src=\"http://www.jboss.org/theme/images/banners/mass-banner.png\" border=\"0\" width=\"240\" height=\"44\" />" +
		"</a>" +
		"</div>" +
		"<div class=\"header_right\">" +
		"WebLogic to JBoss Conversion Estimation Tool - " + currentDateStr + "</div>" +
		"</div>";
		
		dpGraphWriter.write(html);
	}
	
	private void generateClassPathHtml(BufferedWriter cpWriter) throws IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		String currentDateStr = sdf.format(new Date());
		
		String html = "<html>" +
		"<head><link ref=\"stylesheet\" type=\"text/css\" href=\"../style.css\"></head>" +
		"<body>" +
		"<body>" +
		"<div class=\"header\">" +
		"<div class=\"header_left\">" +
		"<a href=\"http://www.jboss.org/mass/MAT.html\">" +
		"<img src=\"http://www.jboss.org/theme/images/banners/mass-banner.png\" border=\"0\" width=\"240\" height=\"44\" />" +
		"</a>" +
		"</div>" +
		"<div class=\"header_right\">" +
		"WebLogic to JBoss Conversion Estimation Tool - " + currentDateStr + "</div>" +
		"</div>";
		
		cpWriter.write(html);
	}
	
	private void generateMetricsAndDependencies() throws IOException {

		
		for(int i = 0 ; i < configXml.getApplications().getDataContent().size() ; i++) {
			Application app = configXml.getApplications().getDataContent().get(i);
			
			String filePath = Utility.constructPath((String)pathMap.get("REPORTS_DIR"), app.getId() + ".metrics.html");
			File metricsFile = new File(filePath);
			BufferedWriter metricsWriter = new BufferedWriter(new FileWriter(metricsFile, true));
			generateMetricsFile(metricsWriter);
			metricsWriter.write("<h2>" + app.getId() + " Codebase Metrics</h2>");
			metricsWriter.write("<pre>");
			String classMetricsFilePath = Utility.constructPath((String)pathMap.get("MAT_HOME"), "data", app.getId() + ".ClassMetrics");
			RandomAccessFile classMetricsFile = new RandomAccessFile(classMetricsFilePath, "rw");
			while(true) {
				String data = classMetricsFile.readLine();
				if(data != null) {
					metricsWriter.write(data + "\n");
				} else {
					classMetricsFile.close();
					break;
				}
			}
			metricsWriter.write("</pre><br/>");
			metricsWriter.write("</body></html>");
			
			String depsFilePath = Utility.constructPath((String)pathMap.get("REPORTS_DIR"), app.getId() + ".dependencies.html");
			File depsFile = new File(depsFilePath);
			BufferedWriter depsWriter = new BufferedWriter(new FileWriter(depsFile, true));
			generateHTMLFileHeader(depsWriter);
			depsWriter.write("<h2>" + app.getId() + " Dependencies");
			depsWriter.write("<br/>");
			for(int j = 0 ; j < app.getDataContent().size() ; j++) {
				ClassPath cp = app.getDataContent().get(j);
				depsWriter.write("<a href=\"../classpaths/" + cp.getId() + ".html\">" + "<h2>" + cp.getId() + "</h2></a>");
				
			}
			//XXXXXX
			
			
		}
	}
	
	private void generateMetricsFile(BufferedWriter metricsWriter) throws IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		String currentDateStr = sdf.format(new Date());
		
		String html = "<html>" +
		"<head><link ref=\"stylesheet\" type=\"text/css\" href=\"../style.css\"></head>" +
		"<body>" +
		"<body>" +
		"<div class=\"header\">" +
		"<div class=\"header_left\">" +
		"<a href=\"http://www.jboss.org/mass/MAT.html\">" +
		"<img src=\"http://www.jboss.org/theme/images/banners/mass-banner.png\" border=\"0\" width=\"240\" height=\"44\" />" +
		"</a>" +
		"</div>" +
		"<div class=\"header_right\">" +
		"WebLogic to JBoss Conversion Estimation Tool - " + currentDateStr + "</div>" +
		"</div>";
		
		metricsWriter.write(html);
		
		
	}
	
	private void generateHTMLFileHeader(BufferedWriter fileWriter) throws IOException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm");
		String currentDateStr = sdf.format(new Date());
		
		String html = "<html>" +
		"<head><link ref=\"stylesheet\" type=\"text/css\" href=\"../style.css\"></head>" +
		"<body>" +
		"<body>" +
		"<div class=\"header\">" +
		"<div class=\"header_left\">" +
		"<a href=\"http://www.jboss.org/mass/MAT.html\">" +
		"<img src=\"http://www.jboss.org/theme/images/banners/mass-banner.png\" border=\"0\" width=\"240\" height=\"44\" />" +
		"</a>" +
		"</div>" +
		"<div class=\"header_right\">" +
		"WebLogic to JBoss Conversion Estimation Tool - " + currentDateStr + "</div>" +
		"</div>";
		
		fileWriter.write(html);
		
		
	}	

}
