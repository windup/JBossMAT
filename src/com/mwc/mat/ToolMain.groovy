package com.mwc.mat

import groovy.util.*
import groovy.xml.*
import java.lang.reflect.*
import java.security.*
import java.text.*
import org.apache.commons.io.FileUtils
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory


public class ToolMain {
	
	public static final env_vars 	= ['MAT_HOME', 'JBOSS_HOME', 'WL_HOME', 'JAVA_HOME']
	public static Log log = LogFactory.getLog("Main")
	
	public static void main(String[] args) {
		
		def env_errors = Utils.checkEnv(env_vars)
		if (env_errors.size() > 0) {
			env_errors.each {e -> println e}
			System.exit(-1) // Exit if the required env variables are not set
		}
		
		def config = [:]
		env_vars.each {v -> config[v] = System.getenv(v)} // put all env vars inside our master config object to pass around



		config['DEPENDENCY_FINDER_HOME'] = Utils.PathJoin (config['MAT_HOME'], 'tools', 'depfinder')
		config['DATA_DIR'] 				 = Utils.PathJoin (config['MAT_HOME'], 'data')
		config['REPORTS_DIR'] 			 = Utils.PathJoin (config['MAT_HOME'], 'reports')
		config['ETC_DIR']				 = Utils.PathJoin (config['MAT_HOME'], 'etc')
		config['CONFIG_XML']		     = Utils.PathJoin (config['MAT_HOME'], 'conf', 'config.xml')

		
		try {
			prepareReportsDest			(config)
			prepareForEachApplication	(config)
			DependenciesScript.run		(config)
			DescriptorsScript.run		(config)
			ServersScript.run			(config)
		}
		catch (Throwable e) {
			System.err.println ("Fatal Error. Please review the log file.")
			log.fatal "Something Went wrong during initialization!"
			log.fatal "${e}"
		}
	}
	
	/**
	* copy files into destination directories that applies globably
	**/
	private static prepareReportsDest (config) {

		FileUtils.deleteDirectory (new File (config['REPORTS_DIR']))
		new File(config['REPORTS_DIR']).mkdir()
		
		FileUtils.copyFile (new File (Utils.PathJoin (config['ETC_DIR'], 'style.css')), 
							new File (Utils.PathJoin (config['REPORTS_DIR'], 'style.css')))

		
		FileUtils.copyFile (new File (Utils.PathJoin (config['ETC_DIR'], 'main.js')), 
						    new File (Utils.PathJoin (config['REPORTS_DIR'], 'main.js')))

		FileUtils.copyFile (new File (Utils.PathJoin (config['DEPENDENCY_FINDER_HOME'], 'etc', 'dependency.style.css')), 
							new File (Utils.PathJoin (config['REPORTS_DIR'], 'dependency.style.css')))
		
	}
	
	private static prepareForEachApplication (config) {
		def result = new XmlSlurper().parse(Utils.getConfigXml())

		result.applications.application.each { app ->
			log.debug "Preparing: " + "${app.'@id'}"
			def app_id = "${app.'@id'}".toString()			
			
			new File(Utils.PathJoin (config['DATA_DIR'], 		app_id)).mkdir()
			new File(Utils.PathJoin (config['REPORTS_DIR'], 	app_id)).mkdir()
			
			def indexHtmlFile = new File(Utils.PathJoin (config['REPORTS_DIR'], app_id, "index.html"))
			
			def html = new MarkupBuilder(indexHtmlFile.newWriter())
			log.debug "Generating file ${indexHtmlFile.absolutePath}"
			html.html {
				head{
					link(rel:"stylesheet", type:"text/css", href:"../style.css")
				}
				body {
					Markup.header(html, "../..")
					h1(app.'@id')
					a(href:"summary.html", "Applications Summary")
					br()
					a(href:"descriptors.html", "Application Descriptors")
					br()
					a(href:"dependencySummary.html", "Class Dependency Summary")
					br()
					a(href:"dependencies.html", "Class Dependencies")
					br()
					a(href:"metrics.html", "Class Metrics")
					br()
					a(href:"dependencyGraph.html", "Dependency Graph")
					br()
				}
			}
		}
		
		
		//Generate the index page for all the reports
		def indexFile = new File(Utils.PathJoin (config['REPORTS_DIR'], "index.html"))
		log.debug "Generating file ${indexFile.absolutePath}"
		
		def indexWriter = indexFile.newWriter()
		def index = new MarkupBuilder(indexWriter)
		
		index.html {
			head{
				link(rel:"stylesheet", type:"text/css", href:"style.css")
			}
			body {
				Markup.header(index, "..")
				h2("Servers")
				result.servers.server.each { server -> 
					a(href:"${server.'@id'}/index.html", server.'@id')
					br()
				}
				h2("Applications")
				br()
				result.applications.application.each { app ->
					a(href: Utils.PathJoin ("${app.'@id'}".toString(), "index.html"), app.'@id')
					br()
				}
			}
		}
		
		
	}
	
}
