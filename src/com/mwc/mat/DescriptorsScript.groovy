/**
 * @author Reid Beckett, Middleware Connections
 * This script searches recursively through the codebase defined in config.xml, searching for application descriptors
 * For each descriptor found, analysis on the files is done and a report is generated
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

package com.mwc.mat

import groovy.util.*
import groovy.xml.*
import org.apache.commons.io.FileUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import java.util.zip.*

public class DescriptorsScript {
	static Log log = LogFactory.getLog(DescriptorsScript.class)
	
	/**
	* Main Entry Point
	**/
	public static run (config) {

		def stats = [:] //a hash of hashes: key=appId; value=hash of keys to counts for 'jsp','html','servlet'

		//loop thru each application
		new XmlSlurper().parse(Utils.getConfigXml()).applications.application.each { app ->
			
			stats[app.'@id'.toString()] = [:]
			
			//read the codebase...the codebase that we analyze for dependencies can be different than the base directory for our application
			//for example, for an exploded webapp named myapp, we would use /path/myapp as the "basedir" and /path/myapp/WEB-INF/classes as the "codebase"
			//the reason for this is to avoid analyzing a huge codebase (when all the WEB-INF/lib jars are included) for dependencies
			def codebase = app."@basedir" != "" ? Utils.deTokenize(app."@basedir".toString()) : Utils.deTokenize(app.codebase.toString())
			log.debug("codebase=${codebase}")

			def appXmls = []
			//if we are looking at a war/ear, expand it and look at the exploded temp directory
			def codebaseDir = Utils.expandCodebase(new File(codebase))
			log.debug("codebaseDir=${codebaseDir}")

			//collect all the application.xml files (ejb descriptors)
			if(codebaseDir.isDirectory()){
				codebaseDir.eachDirRecurse { dir ->
					if(dir.name=="META-INF"){
						appXmls += dir.listFiles().findAll{ it.name == "application.xml"}
					}
				}
			}
			log.debug("appXmls=${appXmls}")

			//collect all the web.xml files (web-app descriptors)
			def webXmls = []
			if(codebaseDir.isDirectory()){
				codebaseDir.eachDirRecurse { dir ->
					if(dir.name=="WEB-INF"){
						webXmls += dir.listFiles().findAll{ it.name == "web.xml" }
					}
				}
			}
			log.debug("webXmls=${webXmls}")

			/** WEB XMLS **/
			//Process the web applications, running counts of jsps, html files, and servlets
			
			new File(Utils.PathJoin (config['REPORTS_DIR'], "${app.'@id'}", "webapps")).mkdir()

			webXmls.each { webXml ->
				def appKey = webXml.parentFile.parentFile.name.toLowerCase()
				//copy the descriptor to reports directory
				def jsps = 0
				def htmls = 0
				webXml.parentFile.parentFile.eachFileRecurse { f ->
					if (f =~ /^.*\.jsp.*$/) jsps++
					if (f =~ /^.*\.htm.*$/) htmls++
				}
				def webapp = new XmlSlurper().parse(webXml)
				stats[app.'@id'.toString()][appKey] = [jsp: jsps, html: htmls, servlet: webapp.servlet.size()]
				//copy the actual web.xml to the reports directory
				def dest_file = Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}", "webapps", "${appKey}_web.xml")
				FileUtils.copyFile(webXml, new File(dest_file))

				//generate HTML report...
				def htmlFileLoc = Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}", "webapps", "${appKey}.html")
				def html = new File(htmlFileLoc)
				
				log.debug("creating file ${html}")
				
				def writer = html.newWriter()
				def htmlMu = new MarkupBuilder(writer)
				
				htmlMu.html{
					head{
						link(rel:"stylesheet", type:"text/css", href:"../../style.css")
					}
					body {
						Markup.header(htmlMu, "../..")
						h2(webXml.parentFile.parentFile.name)
						h3("Statistics")
						table {
							tr{
								td("# of JSP files")
								td(stats[app.'@id'.toString()][appKey]['jsp'])
							}
							tr{
								td("# of HTML files")
								td(stats[app.'@id'.toString()][appKey]['html'])
							}
							tr{
								td("# of Servlets")
								td(webapp.servlet.size())
							}
						}
						//describe each servlet
						h3("Servlets")
						table{
							tr{
								td("Name")
								td("Class")
								td("Mapping")
								td("Descriptor")
							}
							webapp.servlet.each { servlet ->
								tr{
									td(servlet."servlet-name")
									td(servlet."servlet-class")
									def mappings = webapp."servlet-mapping".findAll{ it."servlet-name" == servlet."servlet-name"}
									if(mappings.size()>0){
										td(mappings[0]."url-pattern")
									}else{
										td()
									}
									td{
										a(href:"${webXml.parentFile.parentFile.name.toLowerCase()}_web.xml", "web.xml")
									}
								}
							}
						}

					}

				}//end of markup
			}//end of webXmls loop



			/** Enterprise APP XMLS **/
			//Process the ejb applications, running counts of ejbs and looking for webapps within the ejb
			
			new File(Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}", "earapps")).mkdir()

			appXmls.each { appXml ->
				def appKey = appXml.parentFile.parentFile.name.toLowerCase()
				stats[app.'@id'.toString()][appKey] = [:]

				//copy the descriptor to reports directory
				def dest_app_xml = Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}", "earapps", "${appKey}_application.xml")
				FileUtils.copyFile(appXml, new File(dest_app_xml))

				def ejbapp = new XmlSlurper().parse(appXml)
				log.debug("ejbapp=${ejbapp}")

				//generate HTML report...
				def dest_html = Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}", "earapps", "${appKey}.html")
				def html = new File(dest_html)

				log.debug("creating file ${html}")

				def writer = html.newWriter()
				def htmlMu = new MarkupBuilder(writer)
				htmlMu.html{
					head{
						link(rel:"stylesheet", type:"text/css", href:"../../style.css")
					}
					body {
						Markup.header(htmlMu, "../..")
						h2(appXml.parentFile.parentFile.name)
						h3{
							a(href:"${appKey}_application.xml", "Application Descriptor")
						}
						h3("Statistics")
						table {
							tr{
								td("# of EJB's")
								stats[app.'@id'.toString()][appKey]['ejb'] = ejbapp.module.findAll{ it.ejb != "" }.size()
								td(ejbapp.module.findAll{ it.ejb != "" }.size())
							}
						}
						h3("EJB Applications")
						table{
							tr{
								td("Name")
							}
							ejbapp.module.findAll { it.ejb != "" }.each { module ->
								tr {
									td(module.ejb)
								}
							}
						}
						h3("Web Applications")
						table{
							tr{
								td("URI")
								td("Context Root")
								td("# of JSP Files")
								td("# of HTML Files")
								td("# of Servlets")
							}
							def tjsps = 0
							def thtmls = 0
							def tservlets = 0
							//find the web-apps defined within the ejb descriptor and run counts on its jsps, html files, and servlets
							ejbapp.module.findAll { it.web != "" }.each { module ->
								def key = module.web.'web-uri'.toString().toLowerCase()
								def njsps, nhtmls, nservlets
								if(stats.containsKey(key)){
									njsps = stats[key]['jsp']
									nhtmls = stats[key]['html']
									nservlets = stats[key]['servlet']
									tjsps += njsps
									thtmls += nhtmls
									tservlets += nservlets
								}else{
									njsps = ""
									nhtmls = ""
									nservlets = ""
								}
								tr {
									td{
										a(href:"../webapps/${key}.html", module.web."web-uri")
									}
									td(module.web."context-root")
									td(njsps)
									td(nhtmls)
									td(nservlets)
								}
							}
							tr{
								td()
								td("Total")
								td(tjsps)
								td(thtmls)
								td(tservlets)
							}
						}
					}
				} //end of markup
			}//end of appXmls loop



			/** create the index for the descriptors **/
			def dhtml_loc = Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}", "descriptors.html")
			def dhtml = new File(dhtml_loc)

			def dwriter = dhtml.newWriter()
			def dhtmlMu = new MarkupBuilder(dwriter)

			dhtmlMu.html{
				head{
					link(rel:"stylesheet", type:"text/css", href:"../style.css")
				}
				body {
					Markup.header(dhtmlMu, "../..")
					h2("${app.'@id'} Application Descriptors")
					h3("Enterprise Applications")
					appXmls.each { appXml ->
						a(href:"earapps/${appXml.parentFile.parentFile.name.toLowerCase()}.html", appXml.parentFile.parentFile.name)
						br()
					}
					h3("Web Applications")
					webXmls.each { webXml ->
						a(href:"webapps/${webXml.parentFile.parentFile.name.toLowerCase()}.html", webXml.parentFile.parentFile.name)
						br()
					}
				}
			}	

			//summary
			//create a summary of all the applications by totaling up all stats we collected, across the entire app
			def shtml_loc = Utils.PathJoin(config['REPORTS_DIR'], "${app.'@id'}", "summary.html")
			def shtml = new File(shtml_loc)

			def swriter = shtml.newWriter()
			def shtmlMu = new MarkupBuilder(swriter)

			shtmlMu.html{
				head{
					link(rel:"stylesheet", type:"text/css", href:"../style.css")
				}
				body {
					Markup.header(shtmlMu, "../..")
					h2("${app.'@id'} Applications Summary")
					h3("Enterprise Applications")
					table {
						tr{
							td("Number of Applications")
							td(appXmls.size())
						}
						tr{
							td("Number of EJB's")
							td(stats["${app.'@id'}"].values().sum{ it.ejb == null ? 0 : it.ejb })
						}
					}
					h3("Web Applications")
					table {
						tr{
							td("Number of Applications")
							td(webXmls.size())
						}
						tr{
							td("Number of JSP's")
							log.debug(stats)
							log.debug(stats["${app.'@id'}"])
							log.debug(stats["${app.'@id'}"].values())
							td(stats["${app.'@id'}"].values().sum { it.jsp == null ? 0 : it.jsp })
						}
						tr{
							td("Number of HTML pages")
							td(stats["${app.'@id'}"].values().sum { it.html == null ? 0 : it.html })
						}
						tr{
							td("Number of Servlets")
							td(stats["${app.'@id'}"].values().sum { it.servlet == null ? 0 : it.servlet })
						}
					}
				}
			}	

		}
		
	}

}
