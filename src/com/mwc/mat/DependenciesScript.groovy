/**
 * @author Reid Beckett, Middleware Connections
 * @author Ahmed Talaat, Middleware Connections

 * This script leverages Jean Tessier's Dependency Finder tool (see http://depfind.sourceforge.net/) to analyze each of the codebases defined in config.xml
 *
 *    Copyright 2009 Middleware Connections, a division of Cloudware Connections Inc.
 * 
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0
 * 
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

/**
Revision History
RB - Initial version as one groovy script
AT - 
	- Refactored it into a class with sub methods. Makes changing things a bit easier with the amount of code in here.
	- Made all path references to the master config hash. 
**/

package com.mwc.mat

import java.lang.reflect.*
import groovy.util.*
import groovy.xml.*
import java.security.*
import org.apache.commons.io.FileUtils
import java.text.*
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.util.jar.*

public class DependenciesScript {
	
	static Log log = LogFactory.getLog(DependenciesScript.class)
	
	private static app_deps = [:] //a hash of all the results we obtain, key=application id, value=hash of class loading results
	
	
	
	/**
	* Main Entry Point
	**/
	public static run (config) {
		
		XmlSlurper xmlParser 	= new XmlSlurper()
		def xml 				= xmlParser.parse (Utils.getConfigXml())

		//read option to skip dependency analysis to speed things up
		def runDepsNode = xml.options.'run-dependency-analysis'
		boolean runDeps = runDepsNode == null || !"false".equalsIgnoreCase(runDepsNode.toString())

		if (!runDeps) {
			log.info "Skipping dependency analysis"
		} else {
			log.info "Crunching Dependencies data....."
			
			def boolean result;
			try {
				result = runClassMetrics(config, xml)
				if (!result) println ("A non fatal error occured during running Class Metrics Analysis. Please review the log")
			}
			catch (Throwable e) {
				System.err.println ("Something went wrong while running Class Metrics Analysis. Please see the log file for more details.")
				e.printStackTrace()
				log.fatal ("Error While running class Metrics Analysis")
				log.fatal(e)
			}
			
			try {
				runClassPathAndDependancyAnalysis(config, xml)
				generateMetricsAndDependencies(config, xml)
				generateDependencySummaries(config, xml)
			}
			catch (Throwable e) {
				e.printStackTrace();
			}
			
		}
		
		//for each application, run the DependencyFinder tool scripts - ClassMetrics, DependencyExtractor, and DependentsToHTML
	}
		
	
	private static boolean runClassMetrics (config, xml) {
		xml.applications.application.each { app ->

			//get the codebase - here we substitute env variables for tokens
			def codebaseStr = Utils.deTokenize(app.codebase.toString())

			//next, we expand the codebase if it is defined as an ear or war and repoint ourselves to a temp directory location
			File codebase = Utils.expandCodebase(new File(codebaseStr))

			// run ClassMetrics script
			def output 	= Utils.PathJoin (config['DATA_DIR'], "${app.'@id'}.ClassMetrics".toString())
			if (Utils.isWindows()) {
				def cmd = Utils.PathJoin (config['DEPENDENCY_FINDER_HOME'], "bin", "ClassMetrics.cmd")
				Utils.runCommand(cmd + " \"${codebase.absolutePath}\"" + " -out " +  "${output}")
			} else {	
				def cmd = Utils.PathJoin (config['DEPENDENCY_FINDER_HOME'], "bin", "ClassMetrics")
				Utils.runCommand(cmd + " ${codebase.absolutePath}" + " -out " +  "${output}")
			}

			//there should be some classes found in our codebase
			def path = Utils.PathJoin(config['DATA_DIR'], "${app.'@id'}.ClassMetrics")

			if (new File(path).text.indexOf ("0 class(es)") == 0) {
				log.error("!!! No classes found for codebase ${codebase.absolutePath} - check your configuration to ensure this path exists, it is readable, and that it contains some classes")
				return false;
			}
				
			// Now run the Dependancy Extractor
			output 	= Utils.PathJoin (config['DATA_DIR'], "${app.'@id'}.DependencyExtractor.xml".toString())
			if (Utils.isWindows()){
				def cmd = Utils.PathJoin (config['DEPENDENCY_FINDER_HOME'], "bin", "DependencyExtractor.cmd")
				Utils.runCommand(cmd + " \"${codebase.absolutePath}\"" + " -xml -out " +  "\"${output}\"")
			} else {
				def cmd = Utils.PathJoin (config['DEPENDENCY_FINDER_HOME'], "bin", "DependencyExtractor")
				Utils.runCommand(cmd + " ${codebase.absolutePath}" + " -xml -out " +  "${output}")
			}
			
			// Create an html report of the output of the dependency tool by running the DependentsToHTML script and wrapping the result in our html template

			
			def dpGraphFile 	= new File(Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}", "dependencyGraph.html"))
			def dpGraphWriter 	= dpGraphFile.newWriter(true)
			def dpGraphMarkup 	= new MarkupBuilder(dpGraphWriter)

			dpGraphMarkup.html {
				head {
					link(rel:"stylesheet", type:"text/css", href:"../style.css")
				}
				body {
					Markup.header(dpGraphMarkup, "../..")
					//run the DependentsToHTML script and stream the output into our markup
					if(Utils.isWindows()){
						def cmd = Utils.PathJoin(config['DEPENDENCY_FINDER_HOME'], "bin", "DependentsToHTML.cmd")
						output = Utils.PathJoin(config['DATA_DIR'], "${app.'@id'}.DependencyExtractor.xml")
						Utils.runCommand ("\"$cmd\"" + " -in " + "\"$output\"", dpGraphWriter)
					}else {
						def cmd = Utils.PathJoin(config['DEPENDENCY_FINDER_HOME'], "bin", "DependentsToHTML")
						Utils.runCommand (cmd + " -in " + Utils.PathJoin(config['DATA_DIR'], "${app.'@id'}.DependencyExtractor.xml"), dpGraphWriter)
					}
				}
			}

			//if we have defined packages in our config, break down the dependencies into specific package data
			//this data is not currently used, TODO is to use this somehow in future
/*			app.packages.each { pkg ->

				def in  = Utils.PathJoin(config['DATA_DIR'], "${app.'@id'}.DependencyExtractor.xml")
				def out = Utils.PathJoin(config['DATA_DIR'], "${app.'@id'}.${pkg}.xml")

				if (Utils.isWindows()){

					def cmd = Utils.PathJoin(config['DEPENDENCY_FINDER_HOME'], "bin", "c2c.cmd")
					Utils.runCommand("\"${cmd}\"" + " -scope-includes /${pkg}/" + "\"${in}\"" + " -xml -out " + "\"${out}\"")

				} else {
					def cmd = Utils.PathJoin(config['DEPENDENCY_FINDER_HOME'], "bin", "c2c")
					Utils.runCommand("${cmd}" + " -scope-includes /${pkg}/" + "${in}" + " -xml -out " + "${out}")

				}
			}			
*/			
		}
	}
	
	public static boolean runClassPathAndDependancyAnalysis (config, xml) {
		//for each application, parse the xml dependency data and test the classpath information
		//this section of code gets a little bit tricky!
		
		//loop thru each application
		xml.applications.application.each { app ->
			def externals = [:]

			// parse the dependency "raw" data
			def de_xml_file = Utils.PathJoin(config['DATA_DIR'], "${app.'@id'}.DependencyExtractor.xml")
			def depresult = new XmlSlurper().parse(new File(de_xml_file))

			depresult."package".each { pkg ->
				pkg.class.each {clz ->
					//for each class, get each outbound dependencies
					clz.feature.outbound.findAll{it.@type=='class'}.each { o ->
						//here we are breaking down class name into "parent" packages
						//to break down the dependency counts by main package name (i.e. "org.jboss"), i.e. org.jboss.client.MyClass - we are pulling out the "org.jboss"
						def a = o.toString().split("\\.")
						if(a.length >= 3) {
							def key1 = o.toString().split("\\.")[0..1].join(".") //i.e. org.jboss
							def a2 = o.toString().split("\\.")
							def key2 = a2[2..a2.length-1].join(".") //i.e. client.MyClass
							if(!key2.endsWith("[]")){
								//add the parent package to the map, and keep a count running
								if(externals.containsKey(key1)){
									if(externals[key1].containsKey(key2)){
										externals[key1][key2] = externals[key1][key2] + 1
									}else{
										externals[key1][key2] = 1
									}
								}else{
									externals[key1] = [:]
									externals[key1][key2] = 1
								}
							}
						}
					}
				}
			}	

			/** BUILD CLASSPATH LOCATION...**/
			//In this section, we are building a class loader based on the classpath defined in config.xml
			//we'll use this class loader to test whether each class in our codebase can be loaded and from where it loads the class

			//loop thru each classpath defined, building a list of URL's
			app.classpath.each { cp ->
				def urls = []
				cp.directory.each { dir ->
					//get each directory path
					def path = Utils.deTokenize(dir.'@path'.toString())
					File f = new File(path)
					//add the directory itself
					urls << new File(path).toURL()
					log.debug("f.absolutePath=${f.absolutePath}")
					int excIdx = f.absolutePath.indexOf("!")
					log.debug("excIdx=${excIdx}")
					if(f.isDirectory()){
						//for a directory, get all the jar files within its subdirectories
						f.eachFile {
							if (it =~ /^.*\.jar$/) //is it a jar file?
								urls << it.toURL()
						}
						//we have the option to NOT recurse into subdirectories, but by default we will recurse through
						if(!"false".equalsIgnoreCase(dir.'@recurse'?.toString())){
							//this handy groovy method command loops thru subdirectories recursively
							//again, we add the directory and all the jar files within
							f.eachDirRecurse { directory ->
								urls << directory.toURL()
								directory.eachFile {
									if (it =~ /^.*\.jar$/) //is it a jar file?
										urls << it.toURL()
								}
							}
						}
					}else if(f.exists() && (f.name.endsWith(".ear") || f.name.endsWith(".war"))) {
						//if this is a file that exists and it is an ear or a war, then we want to expand it to a temp directory and
						//expand the archive to temp and add to classfile location
						//explode it to temp
						de.schlichtherle.io.File file = new de.schlichtherle.io.File(f)
						//explode the ear/war...
						def tmpDir = new File(new File(System.getProperty("java.io.tmpdir")), f.name)
						if(f.name.endsWith(".ear")){
							//in an ear, get all the jars in the lib/ directory
							//TODO: what about jars in root of ear
							new File(tmpDir, "lib").eachFile { jarFile ->
								if(jarFile =~ /^.*\.jar$/)
									urls << jarFile.toURL()
							}
						}else if(f.name.endsWith(".war")){
							//in a war, get all the jars in the WEB-INF/lib directory
							//TODO: what about WEB-INF/classes
							new File(tmpDir, "WEB-INF/lib").eachFile { jarFile ->
								if(jarFile =~ /^.*\.jar$/)
									urls << jarFile.toURL()
							}
						}
					}else if(!f.exists() && excIdx > 0 && (f.absolutePath.substring(0, excIdx).endsWith(".war") ||  f.absolutePath.substring(0, excIdx).endsWith(".ear"))){
						//an advanced usage option: we can also define a classpath as a location within a war or ear by naming it something like:
						//mywar.war!WEB-INF/classes
						//in this case, our war/ear would be the section before the !
						de.schlichtherle.io.File file = new de.schlichtherle.io.File(f.absolutePath.substring(0, excIdx))
						def tmpDir = new File(new File(System.getProperty("java.io.tmpdir")), file.name)
						if(file.name.endsWith(".ear")){
							//in an ear, get all the jars in the lib/ directory
							//TODO: what about jars in root of ear
							new File(tmpDir, "lib").eachFile { jarFile ->
								if(jarFile =~ /^.*\.jar$/)
									urls << jarFile.toURL()
							}
						}else if(file.name.endsWith(".war")){
							//in a war, get all the jars in the WEB-INF/lib directory
							//TODO: what about WEB-INF/classes
							new File(tmpDir, "WEB-INF/lib").eachFile { jarFile ->
								if(jarFile =~ /^.*\.jar$/)
									urls << jarFile.toURL()
							}
						}
					}
				}

				//Done building the classpath, now we create an html file simply listing the classpath of the class loader we have built
				/** CREATE THE CLASSPATH LISTING FILE **/
				log.debug("*************************************************")
				log.debug("built classpath URLs for cp.id=${cp.'@id'}:")
				log.debug(urls)
				log.debug("*************************************************")


				new File(Utils.PathJoin (config['REPORTS_DIR'], "classpaths")).mkdir()


				def cpFile = new File(Utils.PathJoin(config['REPORTS_DIR'], "classpaths", "${cp.'@id'}.html"))

				def cpWriter = cpFile.newWriter()
				def cpMarkup = new MarkupBuilder(cpWriter)
				cpMarkup.html {
					head{
						link(rel:"stylesheet", type:"text/css", href:"../style.css")
					}
					body {
						Markup.header(cpMarkup, "../..")
						urls.each {
							cpMarkup.yield it
							br()
						}
					}
				}

				/** FOR EACH CLASSPATH, WRITE THE XML OUT TO FILE **/
				//now we create the class loader and do the test of each class to see if we can load it
				//as we go, we are creating the xml data file...we will circle back on this xml data later on to make an html report of it
				URLClassLoader urlclassloader = new URLClassLoader(urls as URL[])

				def fname = Utils.PathJoin (config['REPORTS_DIR'], "${app.'@id'}.${cp.'@id'}.xml")
				def markup = new MarkupBuilder(new File(fname).newWriter())

				markup.output {
					markup.application(id: app.'@id') {
						externals.keySet().each { key ->
							markup."package"(name:key, dependencycount: externals[key].values().sum()){
								externals[key].each { skey ->
									//loop thru the hash of dependent classes and pull out the class name of each
									def clz = "${key}.${skey.key}"
									def classloaded=false
									def classloadingexception = null
									Class _theclass = null
									try{
										//attempt to load the class
										_theclass = urlclassloader.loadClass(clz)
										classloaded = true
									}catch(Throwable e){
										classloadingexception = e
										classloaded = false
									}
									//where was the class loaded from?
									URL cpurl = null
									String cpdir = null
									if(classloaded) {
										try{
											 cpurl = Utils.getClassLocation(_theclass)
										}catch(Throwable e){
											cpurl = null
										}
										//if the class was loaded from a .class file, this becomes way too much information so here we are trying to determine
										//the parent directory of the .class file by looking at the class name and walking back up the directory structure
										if(cpurl != null && cpurl.toString().endsWith(".class")){
											try{
												def idx = cpurl.toString().replaceAll(System.getProperty("file.separator"), ".").indexOf(_theclass.name)
												if(idx > -1)
													cpdir = cpurl?.toString().substring(0,idx)
											}catch(Throwable e){}
										}
									}
									//finally create the xml node with all this data
									markup."class"(name:clz, dependencycount: skey.value, classloaded:classloaded, "classpath-url": cpurl, "classpath-dir":cpdir, "classloadingexception": classloadingexception, shortname:skey.key)
								}
							}

						}
					}
				}//end of markup.output

			}

			//we are done with this application, so add it to the master hash
			app_deps[app.'@id'] = externals
		}
		
	}
	
	private static generateMetricsAndDependencies (config, xml) {
		xml.applications.application.each { app ->

			//generate the metrics output by reading the output of the ClassMetrics script run above
			//essentially, just wrapping the output into our template

			
			def metricsFile = new File(Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}", "metrics.html"))

			log.debug "Generating file ${metricsFile.absolutePath}"

			def metricsOut 	= metricsFile.newWriter()
			def metrics 	= new MarkupBuilder(metricsOut)

			metrics.html {
				head{
					link(rel:"stylesheet", type:"text/css", href:"../style.css")
				}
				body {
					Markup.header(metrics, "../..")
					h2("${app.'@id'} Codebase Metrics")
					pre("") {
						new File(Utils.PathJoin(config['MAT_HOME'], "data", "${app.'@id'}.ClassMetrics")).eachLine {
							metrics.yield "${it}\n"
						}
					}
				}
			}

			// generate the dependencies html
			
			def depsFile = new File(Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}", "dependencies.html"))

			log.debug "Generating file ${depsFile.absolutePath}"
			def deps = new MarkupBuilder(depsFile.newWriter())

			deps.html {
				head{
					link(rel:"stylesheet", type:"text/css", href:"../style.css")
				}
				body {
					Markup.header(deps, "../..")
					h2("${app.'@id'} Dependencies")
					br()

					app.classpath.each { cp ->
						a(href:"../classpaths/${cp.'@id'}.html"){
							h2("${cp.'@id'}")
						}
						
						// read the class loading data from the xml we generated previously and generate a table
						
						def depres = new XmlSlurper().parse(new File(Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}.${cp.'@id'}.xml")))
						table {
							tr {
								td(class:"dep_header", "Package")
								td(class:"dep_header", "Number of References")
								td(class:"dep_header", "Classes Successfully Loaded From")
								td(class:"dep_header", "Number of Missing classes")
							}
							depres.application."package".each { pkg ->
								tr {
									td(class:"dep_data", "${pkg.'@name'}")
									td(class:"dep_data", "${pkg.'@dependencycount'}")
									HashSet classload = new HashSet()
									def missingclassload = []
									pkg.class.each { clz ->
										if("true".equalsIgnoreCase(clz.'@classloaded'.toString())){
											def resource = clz.'@classpath-dir' != "" ? clz.'@classpath-dir'.toString() : clz.'@classpath-url'.toString()
											if(resource.indexOf("!") > 0) resource = resource.substring(0,resource.indexOf("!"))
											else if(resource.startsWith("file:/")){
													cp.directory.each { dir ->
														if(resource.startsWith("file:${dir.'@path'}")){
															resource = "${dir.'@path'}"
														}
													}
											}
											classload.add(resource)
										}else{
											missingclassload << clz.'@name'
										}
									}
									td(class:"dep_data"){
										classload.each{
											yield it
											br()
										}
									}
									td(class:"dep_data"){
										if(missingclassload.size() == 0){
											deps.yield "${missingclassload.size()}"
										}else{
											a(href:"${pkg.'@name'}/missing.html", "${missingclassload.size()}")
										}
									}

									//generate the html for the missing classes for this package
									if(missingclassload.size() > 0){
										
										new File(Utils.PathJoin (config['REPORTS_DIR'], "${app.'@id'}", "${pkg.'@name'}")).mkdir()

										def missingFileLocation = Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}", "${pkg.'@name'}", "missing.html")
										def missingFile = new File(missingFileLocation)
										
										log.debug "Generating file ${missingFile.absolutePath}"
										def missing = new MarkupBuilder(missingFile.newWriter())
										
										missing.html {
											missing.head{
												missing.link(rel:"stylesheet", type:"text/css", href:"../../style.css")
											}
											missing.body {
												Markup.header(missing, "../../..")
												missing.div {
													missing.h2("Codebase ${app.'@id'} against classpath ${cp.'@id'} failed to load these classes:")
												}
												missingclassload.each {
													missing.yield "${it}"
													missing.br()
												}
											}
										}//end of missing html markup
									}
								}
							}
						}
					}
				}//end of body
			}//end of deps.html markup


	}//end of applications loop
		
	}
	
	private static generateDependencySummaries(config, xml) {
		//dependency summaries
		//Here, we are again looping thru the class loading data we generated previously,
		//but this time we are summarizing the dependencies into a master list of jars/locations used
		//and the inspecting each jars manifest to report on those attributes
		xml.applications.application.each { app ->
			app.classpath.each { cp ->
				
				def xml_file = Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}.${cp.'@id'}.xml")
				def depres = new XmlSlurper().parse(new File(xml_file))
				
				def html_file = Utils.PathJoin (config['REPORTS_DIR'], "${app.'@id'}", "dependencySummary.html")
				def depsFile = new File(html_file)
				def deps = new MarkupBuilder(depsFile.newWriter())
				
				deps.html {
					head{
						link(rel:"stylesheet", type:"text/css", href:"../style.css")
					}
					body {
						Markup.header(deps, "../..")
						h2("${app.'@id'} vs. ${cp.'@id'} Dependency Summary")
						br()

						HashSet locations = new HashSet()
						depres.application."package".each { pkg ->
							pkg.class.each { clz ->
								if("true".equalsIgnoreCase(clz.'@classloaded'.toString())){
									def resource = clz.'@classpath-dir' != "" ? clz.'@classpath-dir'.toString() : clz.'@classpath-url'.toString()
									if(resource.indexOf("!") > 0) resource = resource.substring(0,resource.indexOf("!"))
									locations.add(resource)
								}
							}
						}

						table {
							tr {
								td(style:"font-weight:bold", "Class Location")
								td(style:"font-weight:bold", "Metainf Data")
							}

							locations.each { loc ->
								tr {
									td(valign:"top", loc)
									if(loc.indexOf("jar:")==0){
										JarFile jarFile = new JarFile(new File(new URI(loc.substring(4))))
										log.debug("jarFile=${jarFile}")
										td(valign:"top"){
											table{
												jarFile.manifest.mainAttributes.entrySet().each { entry ->
													tr {
														td(entry.key)
														td(entry.value)
													}
												}
											}
										}
									}
								}
							}


						}

					}
				}//end of markup closure
			}//end of classpaths loop
		}//end of applications loop
		
	}
}