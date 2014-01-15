/**
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


import java.util.zip.*

def downloadLocation = "http://www.wlshell.net/v2/wlshell-2.1.0.zip"
def patchLocation = "http://www.wlshell.net/v2/wlshell-2.1.0.1.jar"
def dest = Utils.PathJoin (System.getProperty("user.dir"), ,"..", "tools")

def round(num, prec){
	def divider = 10**prec
	return Math.round(num*divider)/divider
}

//add unzip capability to File meta class

File.metaClass.unzip = { File destFile ->
	def zipFile = new ZipFile(delegate)
	zipFile.entries().each { entry ->
		if(entry.isDirectory()){
			new File(destFile, entry.name).mkdir()
			return
		}
		//copy the stream
		def inStream = zipFile.getInputStream(entry)
		def outStream = new BufferedOutputStream(new FileOutputStream(new File(destFile, entry.name)))
	  byte[] buffer = new byte[1024];
	  int len;

	  while((len = inStream.read(buffer)) >= 0)
	    outStream.write(buffer, 0, len);

	  inStream.close();
	  outStream.close();
	}
	zipFile.close()
}

def download(String address, File destination)
{
		//def file = new File(address.tokenize("/")[-1])
		def out = new BufferedOutputStream(new FileOutputStream(destination))
		def conn = new URL(address).openConnection()
		Thread.start {
			while(destination.size() < conn.contentLength) {
				def pct = round((destination.size()/conn.contentLength)*100,0)
/*				println "${round(destination.size()/1024,1)}KB out of ${round(conn.contentLength/1024,1)}KB [${pct}%]"*/
				print "."
				Thread.sleep(500)
			}
			println "${round(destination.size()/1024,1)}KB out of ${round(conn.contentLength/1024,1)}KB [100%]"
		}
		out << conn.inputStream
    out.close()
		return destination
}


println "Downloading WL Shell..."

def file = download(downloadLocation, new File("wlshell.zip"))

def destFile = new File(dest)
println "Unpacking WL Shell to ${destFile.absolutePath} ..."
file.unzip(destFile)
file.delete()
//rename directory
println "Renaming directory: ${new File(destFile, 'wlshell-2.1.0').absolutePath} to ${new File(destFile, 'wlshell').absolutePath} ..."
println new File(destFile, "wlshell-2.1.0").renameTo(new File(destFile, "wlshell"))

//get the WL shell patch and overwrite the distribution's jar

download(patchLocation, new File(Utils.PathJoin (System.getProperty("user.dir"), "..", "tools", "wlshell", "lib", "wlshell-2.1.0.1.jar")))

//TODO: file mods....

File wlsh_dir = new File(destFile, "wlshell")
File wlsh_bin_dir = new File(wlsh_dir, "bin")
File wlsh_sh = new File(wlsh_bin_dir, "wlsh.sh")
wlsh_sh.text = '''
#!/bin/sh


# ------------------------------------------------------------------------
# File    : wlsh.sh
#
# Abstract: This batch file sets the appropriate environment
#           variables for wlshell and starts it.
#           Users should customize the ENVFILE variable in the first section,
#           as well as the two other environment files where appropriate:
#           env4wls.cmd and env4jmx.cmd.
#
# "Usage:   wlsh.sh [options]"
#
# ------------------------------------------------------------------------


# ------------------------------------------------------------------------
# uncomment to use wlshell with WebLogic, also specify the WebLogic version
#ENVFILE="env4wls.sh 8"
# uncomment to use wlshell with other JMX implementations
ENVFILE="env4jmx.sh"

. ${ENVFILE} > /dev/null

. ${WL_HOME}/common/bin/commEnv.sh > /dev/null
# ------------------------------------------------------------------------

${JAVA_HOME}/bin/java -cp ${CLASSPATH} ${MEM} ${PROPS} wlshell.Main -v WLSHDIR=${WLSHDIR} $@
'''

File wlsh_cmd = new File(wlsh_bin_dir, "wlsh.cmd")
wlsh_cmd.text = '''
@ECHO OFF
SETLOCAL


REM ------------------------------------------------------------------------
REM File    : wlsh.cmd
REM
REM Abstract: This batch file sets the appropriate environment
REM           variables for wlshell and starts it.
REM           Users should customize the ENVFILE variable in the first section,
REM           as well as the two other environment files where appropriate:
REM           env4wls.cmd and env4jmx.cmd.
REM
REM "Usage:   wlsh [options]"
REM
REM ------------------------------------------------------------------------


REM ------------------------------------------------------------------------
REM uncomment to use wlshell with WebLogic, also specify the WebLogic version
REM set ENVFILE=env4wls.cmd 10
REM uncomment to use wlshell with other JMX implementations
set ENVFILE=env4jmx.cmd

call %ENVFILE% > nul
call %WL_HOME%\\common\\bin\\commEnv.cmd > nul
REM ------------------------------------------------------------------------


"%JAVA_HOME%\\bin\\java" %MEM% %PROPS% wlshell.Main -v WLSHDIR=%WLSHDIR% %*%


ENDLOCAL
'''

File env4jmx_sh = new File(wlsh_bin_dir, "env4jmx.sh")
env4jmx_sh.text = '''
#!/bin/sh


# ------------------------------------------------------------------------
# File    : env4jmx.sh
#
# Abstract: This batch file sets the appropriate environment
#           variables for wlshell using a JMX implementation.
#           Users should customize variables in the first section,
#           specially JAVA_HOME, JMXPROVIDER, WLSHVER, WLSHDIR
#
# "Usage:   env4jmx.sh"
#
# ------------------------------------------------------------------------


# ------------------------------------------------------------------------
# Variables for customization
PROPS=""
CLASSPATH=""

# uncomment and configure to use wlshell with SUN Java SE JMX
#JAVA_HOME="/usr/java"
# uncomment and configure to use wlshell with MX4J
#JAVA_HOME="/opt/j2sdk1.4.2_06"


# uncomment to use wlshell with SUN Java SE JMX
JMXPROVIDER=SUN

# uncomment to use wlshell with MX4J
#JMXPROVIDER=MX4J
#JMXHOME=/opt/mx4j-2.0.1
#CLASSPATH=\${JMXHOME}/lib/mx4j.jar:\${JMXHOME}/lib/mx4j-remote.jar
# ------------------------------------------------------------------------


# ------------------------------------------------------------------------
# Set PATH
PATH=\${JAVA_HOME}/bin:\${PATH}
# ------------------------------------------------------------------------


# ------------------------------------------------------------------------
# Set wlshell home directory
WLSHVER="2.1.0"
#WLSHDIR="/opt/wlshell-\${WLSHVER}"
WLSHDIR=".."
# ------------------------------------------------------------------------


# ------------------------------------------------------------------------
# Set classpath
CLASSPATH=\${WLSHDIR}/lib/xbean.jar:\${CLASSPATH}
CLASSPATH=\${WLSHDIR}/lib/xconfig-2.0.jar:\${CLASSPATH}
CLASSPATH=\${WLSHDIR}/lib/forms-1.0.2.jar:\${CLASSPATH}
CLASSPATH=\${WLSHDIR}/lib/looks-1.1.2.jar:\${CLASSPATH}
CLASSPATH=\${WLSHDIR}/lib/log4j-1.2.8.jar:\${CLASSPATH}
CLASSPATH=\${WLSHDIR}/lib/wlshell-\${WLSHVER}.jar:\${CLASSPATH}
CLASSPATH=\${WL_HOME}/server/lib/weblogic.jar:\${CLASSPATH}
CLASSPATH=\${WLSHDIR}/lib/jmx-client.jar:\${CLASSPATH}
CLASSPATH=\${WLSHDIR}/lib/jmx-invoker-adaptor-client.jar:\${CLASSPATH}
CLASSPATH=\${WL_HOME}/server/lib/webserviceclient.jar:\${CLASSPATH}
CLASSPATH=\${WLSHDIR}/lib/wlshell-2.1.0.1.jar:\${CLASSPATH}

# ------------------------------------------------------------------------


# ------------------------------------------------------------------------
# Set properties
# uncomment when using SUN Java SE JMX
# to enable the Remote JMX Agent
PROPS="\${PROPS} -Dcom.sun.management.jmxremote.port=1098"
PROPS="\${PROPS} -Dcom.sun.management.jmxremote.authenticate=false"
PROPS="\${PROPS} -Dcom.sun.management.jmxremote.ssl=false"
# ------------------------------------------------------------------------


# ------------------------------------------------------------------------
# Set JVM parameters
# modify memory parameters as needed
MEM="-Xms64m -Xmx128m"
# ------------------------------------------------------------------------


# ------------------------------------------------------------------------
MESSAGE="wlshell - environment for JMX \${JMXPROVIDER}"
echo "\${MESSAGE} set."
# ------------------------------------------------------------------------
'''

File env4jmx_cmd = new File(wlsh_bin_dir, "env4jmx.cmd")
env4jmx_cmd.text = '''
@ECHO OFF


REM ------------------------------------------------------------------------
REM File    : env4jmx.cmd
REM
REM Abstract: This batch file sets the appropriate environment
REM           variables for wlshell using a JMX implementation.
REM           Users should customize variables in the first section,
REM           specially JAVA_HOME, JMXPROVIDER, WLSHVER, WLSHDIR
REM
REM "Usage:   env4jmx"
REM
REM ------------------------------------------------------------------------


REM ------------------------------------------------------------------------
REM Variables for customization
set PROPS=
set CLASSPATH=

REM uncomment and configure to use wlshell with SUN Java SE JMX
REM set JAVA_HOME="C:\\Program Files\\Java\\jre1.6.0_01"
REM uncomment and configure to use wlshell with MX4J
rem set JAVA_HOME="C:\\Program Files\\Java\\j2sdk1.4.2_06"


REM uncomment to use wlshell with SUN Java SE JMX
set JMXPROVIDER=SUN

REM uncomment to use wlshell with MX4J
rem set JMXPROVIDER=MX4J
rem set JMXHOME=C:\\mx4j-2.0.1
rem set CLASSPATH=%JMXHOME%\\lib\\mx4j.jar;%JMXHOME%\\lib\\mx4j-remote.jar
REM ------------------------------------------------------------------------


REM ------------------------------------------------------------------------
REM Set PATH
set PATH=%JAVA_HOME%\\bin;%PATH%
REM ------------------------------------------------------------------------


REM ------------------------------------------------------------------------
REM Set wlshell home directory
set WLSHVER=2.1.0
set WLSHDIR=..
REM ------------------------------------------------------------------------


REM ------------------------------------------------------------------------
REM Set classpath
set CLASSPATH=%WLSHDIR%\\lib\\xbean.jar;%CLASSPATH%
set CLASSPATH=%WLSHDIR%\\lib\\xconfig-2.0.jar;%CLASSPATH%
set CLASSPATH=%WLSHDIR%\\lib\\forms-1.0.2.jar;%CLASSPATH%
set CLASSPATH=%WLSHDIR%\\lib\\looks-1.1.2.jar;%CLASSPATH%
set CLASSPATH=%WLSHDIR%\\lib\\log4j-1.2.8.jar;%CLASSPATH%
set CLASSPATH=%WLSHDIR%\\lib\\wlshell-%WLSHVER%.jar;%CLASSPATH%
set CLASSPATH=%WL_HOME%\\server\\lib\\weblogic.jar;%CLASSPATH%
set CLASSPATH=%WLSHDIR%\\lib\\jmx-client.jar;%CLASSPATH%
set CLASSPATH=%WLSHDIR%\\lib\\jmx-invoker-adaptor-client.jar;%CLASSPATH%
set CLASSPATH=%WL_HOME%\\server\\lib\\webserviceclient.jar;%CLASSPATH%
set CLASSPATH=%WLSHDIR%\\lib\\wlshell-2.1.0.1.jar;%CLASSPATH%
REM ------------------------------------------------------------------------


REM ------------------------------------------------------------------------
REM Set properties
REM uncomment when using SUN Java SE JMX
REM to enable the Remote JMX Agent
set PROPS=%PROPS% -Dcom.sun.management.jmxremote.port=1090
set PROPS=%PROPS% -Dcom.sun.management.jmxremote.authenticate=false
set PROPS=%PROPS% -Dcom.sun.management.jmxremote.ssl=false
REM ------------------------------------------------------------------------


REM ------------------------------------------------------------------------
REM Set JVM parameters
REM modify memory parameters as needed
set MEM=-Xms64m -Xmx128m
REM ------------------------------------------------------------------------


REM ------------------------------------------------------------------------
set MESSAGE=wlshell - environment for JMX %JMXPROVIDER%
title %MESSAGE%
echo %MESSAGE% set.
REM ------------------------------------------------------------------------
'''
