/**
 * @author Reid Beckett, Middleware Connections
 * This class leverages Paco Gomez's wlshell tool to analyze the WebLogic server configuration defined in config.xml
 * For each server defined, an extensive analysis of the MBeans and deployment configuration is done via JMX interrogation
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
import groovy.text.SimpleTemplateEngine
import org.apache.commons.io.FileUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

public class ServersScript {
	
	static Log log = LogFactory.getLog(ServersScript.class)
	static wlshell_templates = ["all.wlshell.template", "applications.wlshell.template"]
	
	public static boolean run (config) {
		
		
		//we have an option to skip running server analysis if we are only interested in class dependencies
		def xml = new XmlSlurper().parse(new File(Utils.getConfigXml()))
		def runDeps = xml.options.'run-server-analysis'

		if(runDeps!=null && "false".equalsIgnoreCase(runDeps.toString())){
			log.info "Skipping server analysis"
			System.exit(-1)
		}

		log.info ""
		log.info "Starting runtime analysis of servers..."
		log.info ""

		log.debug "Servers:"
		//collect a list of WL servers as defined in the config.xml
		def servers = []
		XmlSlurper config_xml = new XmlSlurper()
		config_xml.parse(new File(Utils.getConfigXml())).servers.server.each { server ->
			log.debug server.'@id'
			servers << [id: server.'@id', protocol: server.protocol, host: server.host, port: server.port, username: server.username, password: server.password, 
				datadir: new File(config['DATA_DIR']).absolutePath, separator: File.separator, weblogicVersion: Float.parseFloat("${server.'weblogic-version'}")]
		}


		generateScripts (config, servers)
		

		//For each server, here were are executing wlshell against
		/** some of the MBean attribute values give us problems with character sets, so we don't try to read those **/
		def SKIP_LIST = ["(error", "Salt", "EncryptedSecretkey", "EncryptedSecretKey"]
		servers.each { server -> 
			Float wlVersion = server.weblogicVersion

			if (wlVersion < 9) {
				runWLShellScript(config, server)
			}
			else { 
				def script_loc  = Utils.PathJoin(config['MAT_HOME'], "bin", "wlst", "all.${server.id}.py")
				if(Utils.isWindows()){
					def cmdLine = Utils.PathJoin(config['WL_HOME'], "common", "bin", "wlst.cmd")
					Utils.runCommand("${cmdLine} ${script_loc}")
				}else{
					def cmdLine = Utils.PathJoin(config['MAT_HOME'], "bin", "wlstWrapper.sh")
					Utils.runCommand("sh ${cmdLine} ${script_loc}")
				}
			}
		}

		//collect the types of mbeans that we want to report on in detail (comes from config.xml)
		def MBEAN_TYPES = []
		new XmlSlurper().parse(new File(Utils.getConfigXml())).servers.server.mbeans.mbean.each { mbean ->
			MBEAN_TYPES << [type: mbean.'@type'.toString(), label: mbean.'@label'.toString()]
		}

		//now parse the mbeans xml file and generate html report
		servers.each { server ->
			
			def mbeans = new XmlSlurper().parse(new File(Utils.PathJoin(config['DATA_DIR'], "${server.id}.xml")))
			
			//index.html
			new File(Utils.PathJoin(config['REPORTS_DIR'], "${server.id}")).mkdir()
			
			def serverFile = new File(Utils.PathJoin(config['REPORTS_DIR'], "${server.id}", "index.html"))
			log.debug "Generating file ${serverFile.absolutePath}"
			
			def writer = serverFile.newWriter()
			def mu = new MarkupBuilder(writer)

			mu.html {
				head{
					title("MBeans for server ${server.id}")
					link(rel:"stylesheet", type:"text/css", href:"../style.css")
					script(type:"text/javascript", src: "../main.js","")
				}
				body {
					Markup.header(mu, "../..")
					Markup.serverNav(mu, server, MBEAN_TYPES)
					h2("MBeans")
					div(class:"mbean_search"){
						form(onsubmit:'searchMbeans();return false;') {
							input(type:'text',id:'q', name:'q',size:'40')
							input(type:'button', onclick:"document.getElementById('q').value='';searchMbeans();", value:'Clear')
							input(type:'button', onclick:"searchMbeans();", value:'Filter')
							div(id:'status')
						}
					}
					div(id:"wrapper") {
						mbeans.mbean.eachWithIndex { mbean, i ->
							Markup.mbean(mu, mbean, i+1)
						}

					}
				}
			}

			if(server.weblogicVersion < 9){
				//generate html file for applications
				
				def apps_result = new XmlSlurper().parse(new File(Utils.PathJoin(config['DATA_DIR'], "${server.id}.applications.xml")))
				def appsFile 	= new File(Utils.PathJoin(config['REPORTS_DIR'], "${server.id}", "applications.html"))
				
				def appswriter = appsFile.newWriter()
				def appsmu = new MarkupBuilder(appswriter)
				appsmu.html {
					head{
						title("Applications for server ${server.id}")
						link(rel:"stylesheet", type:"text/css", href:"../style.css")
						script(type:"text/javascript", src: "../main.js","")
					}
					body {
						Markup.header(appsmu, "../..")
						Markup.serverNav(appsmu, server, MBEAN_TYPES)
						h2("Applications")
						apps_result.application.each { app ->
							a(href: "${app.name.toString().toLowerCase()}.html", app.name)
							br()
						}
					}
				}

				apps_result.application.each { app ->
					def apps_html_loc = Utils.PathJoin(config['REPORTS_DIR'], "${server.id}", "${app.name.toString().toLowerCase()}.html")
					log.debug "${apps_html_loc}"
					def appFile = new File(apps_html_loc)

					def appwriter = appFile.newWriter()
					def appmu = new MarkupBuilder(appwriter)
					def app_mbeans = mbeans.mbean.findAll {
						it.Application == app.ObjectName
					}
					appmu.html {
						head{
							title("${app.name}")
							link(rel:"stylesheet", type:"text/css", href:"../style.css")
							script(type:"text/javascript", src: "../main.js","")
						}
						body {
							Markup.header(appmu, "../..")
							Markup.serverNav(appmu, server, MBEAN_TYPES)
							h2(app.name)
							h3("Application Config")
							mbeans.mbean.findAll { mbean ->
								mbean.Name == app.name && mbean.Type=="ApplicationConfig"
							}.eachWithIndex { mb, i ->
								Markup.mbean(appmu, mb, "appconf${i+1}")
							}
							h3("Application Component Config")
							mbeans.mbean.findAll { mbean ->
								mbean.ApplicationConfig==app.Name
							}.eachWithIndex {mb, i ->
								Markup.mbean(appmu, mb, "appcompconf${i+1}")
							}
							if(app_mbeans.size() > 0){
								h3("Application Components")
								app_mbeans.eachWithIndex { mb, i ->
									Markup.mbean(appmu, mb, "appcomp${i+1}")
								}
							}
							h3("Application Attributes")
							appmu.div {
								app.children().each { child ->
									div(class:"mbean_data"){
										div(class:"mbean_key", child.name())
										div(class:"mbean_value", child.text())
									}
								}
							}
						}
					}
				}
			}


			//generate html file for each specific type of mbean we are interested in
			MBEAN_TYPES.each { mbean_type ->
				def mbean_file_loc = Utils.PathJoin(config['REPORTS_DIR'], "${server.id}", "${mbean_type.label.replaceAll(' ','').toLowerCase()}.html")
				def mbFile = new File(mbean_file_loc)

				log.debug "Generating file ${mbFile.absolutePath}"

				def swriter = mbFile.newWriter()
				def smu = new MarkupBuilder(swriter)

				smu.html {
					head{
						title("${mbean_type.label} for server ${server.id}")
						link(rel:"stylesheet", type:"text/css", href:"../style.css")
						script(type:"text/javascript", src: "../main.js","")
					}
					body {
						Markup.header(smu, "../..")
						Markup.serverNav(smu, server, MBEAN_TYPES)
						h2(mbean_type.value)
						div(id:"wrapper") {
							mbeans.mbean.findAll{it.Type==mbean_type.type}.eachWithIndex { mbean, i ->
								Markup.mbean(smu, mbean, i+1)
							}

						}
					}
				}
			}



		}

		log.info "Done analysis of servers."
		
	}
	private static generateScripts (config, servers) {
		/** 
		The wlshell scripts are run once per server config...the following section reads the templates and creates scripts for wlshell to execute 
		the wlshell scripts live in templates...in this section we loop thru each template and for each server we create a wlshell script 
		for the server to execute
		**/

		servers.each { server ->
			Float wlVersion = Float.parseFloat("${server.weblogicVersion}")
			if(wlVersion < 9){
				//create WLSHELL scripts
				wlshell_templates.each { template_path ->
					def prefix = template_path.substring(0, template_path.indexOf("."))
					def engine = new SimpleTemplateEngine()
			
					def template = engine.createTemplate(new File(Utils.PathJoin (config['ETC_DIR'], "template_path")).text).make(server)
					def wlshell_script = new File(Utils.PathJoin(config['MAT_HOME'], "bin", "wlshell", "${prefix}.${server.id}")).newWriter()

					wlshell_script.write(template.toString())
					wlshell_script.close()
				}
			}else{
				//create WLST scripts
				def engine = new SimpleTemplateEngine()
				if(Utils.isWindows()){
					server['allpyout'] = "${server.datadir}/${server.id}.xml".replaceAll('\\\\','/')
				}else{
					server['allpyout'] = Utils.PathJoin("${server.datadir}", "${server.id}.xml")
				}


				def template = engine.createTemplate(new File(Utils.PathJoin(config['ETC_DIR'], "all.py")).text).make(server)
				def wlst_script = new File(Utils.PathJoin(config['MAT_HOME'], "bin", "wlst", "all.${server.id}.py")).newWriter()

				wlst_script.write(template.toString())
				wlst_script.close()
			}
		}	
	}
	
	private static runWLShellScript (config, server) {
		def wlproc_cmds = wlshell_templates.collect { template_path ->
			def prefix = template_path.substring(0, template_path.indexOf("."))
			def script_loc = Utils.PathJoin(config['MAT_HOME'], "bin", "wlshell", "${prefix}.${server.id}")
			if(Utils.isWindows()){
				return "wlsh.cmd -f ${script_loc}"	
			}else{
				return "./wlsh.sh -f ${script_loc}"	
			}
		}

		log.debug(wlproc_cmds)

		//run the wlshell scripts - these will output to a specific file
		wlproc_cmds.each { cmd ->
			Utils.runCommand(cmd)
		}

		//reparse the output of wlshell scripts and create structured xml out of it to use later
		//first collect the beans into array of key values
		def mbeansa = []
		
		new File(Utils.PathJoin(config['DATA_DIR'], "wls_all${server.id}")).eachLine { line ->
			if(line.indexOf("MBEAN:")==0){
				def mbean = [:]
				line.substring(7).split(",").each { tok ->
					def toks = tok.split("=")
					mbean[toks[0]] = toks[1]
				}
				mbeansa.add(mbean)
			}else if (mbeansa.size() > 0){
				if(SKIP_LIST.findAll{ line.indexOf(it) == 0}.size() == 0){
					def toks = line.split(/\|=\|/)
					if(toks.size() >= 2)
						mbeansa[mbeansa.size()-1][toks[0].trim()] = toks[1].trim()
				}
			}
		}

		//There should be some mbeans discovered - if not, something has gone wrong
		if(mbeansa.size()==0){
			log.error "!!! No MBEANS found for server ${server.id}"
			log.error "!!! Perhaps the tool had a problem connecting.  Please check the connection configuration for this server in conf/config.xml"
	 		println "!!! No MBEANS found for server ${server.id}"
			println "!!! Perhaps the tool had a problem connecting.  Please check the connection configuration for this server in conf/config.xml"
			System.exit(0)
		}

		//now output the array to an xml file
		
		def xmlOut = new File(Utils.PathJoin(config['DATA_DIR'], "${server.id}.xml")).newWriter()
		def xml = new MarkupBuilder(xmlOut)
		xmlOut.write("<?xml version=\"1.0\" encoding=\"iso-8859-1\"	?>\n")
		xml.mbeans {
			mbeansa.each { _mbean ->
				mbean {
					_mbean.keySet().asList().each { key ->
						"${key}"(_mbean[key])
					}
				}
			}
		}

		//here we are pulling out the applications we found by running the wlshell script for applications
		def appsarray = []
		
		new File(Utils.PathJoin(config['DATA_DIR'], "${server.id}.applications")).eachLine { line ->
			if(line.indexOf("APPLICATION")==0){
				def app = [:]
				appsarray.add(app)
			}else if (appsarray.size() > 0){
				if(appsarray[appsarray.size()-1].size()==0){
					appsarray[appsarray.size()-1]['name'] = line
				}
				if(SKIP_LIST.findAll{ line.indexOf(it) == 0}.size() == 0){
					def toks = line.split(/\|=\|/)
					if(toks.size() >= 2)
						appsarray[appsarray.size()-1][toks[0].trim()] = toks[1].trim()
				}
			}
		}

		//for each app, we reprocess the wlshell output and collect it as xml for processing further down
		
		def appXmlOut = new File(Utils.PathJoin(config['DATA_DIR'], "${server.id}.applications.xml")).newWriter()
		def appsXml = new MarkupBuilder(appXmlOut)
		appXmlOut.write("<?xml version=\"1.0\" encoding=\"iso-8859-1\"	?>\n")
		appsXml.applications {
			appsarray.each { _app ->
				application {
					_app.keySet().asList().each { key ->
						"${key}"(_app[key])
					}
				}
			}
		}
		
	}
	private static runWLSTScript (config, server) {
	}
}