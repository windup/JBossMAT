/**
 * @author Reid Beckett, Middleware Connections
 * This script contains some generic utilities for other scripts to use 
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


import java.security.*
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

class Utils {
	static final Log log = LogFactory.getLog(Utils.class)
	static final String SEP = System.getProperty("file.separator")
	static final String USER_DIR = System.getProperty("user.dir")
	
	/**
	* Quick util to join paths. We seem to be doing this alot.
	*/
	private static PathJoin(... parts) {

		def a = []
		parts.each {x -> 
			if (x.endsWith(File.separator))  x = x.substring(0, x.size() -1)
			a << x
		}
		//log.debug("Joining ${a}")
		a.join(File.separator)
	}
	


	static String[] checkEnv(env_vars) {
		
		def errors 	= [] 
		env_vars.each {var -> 
			if (System.getenv (var) == null) {
				errors << "${var} is not set."
			}
		}
		return errors		
	}

	static String getConfigXml(){
		log.debug("config xml is ${System.getProperty("config.xml", "${USER_DIR}${SEP}conf${SEP}config.xml")}")
		return System.getProperty("config.xml", "${USER_DIR}${SEP}conf${SEP}config.xml")
	}
		
	static int runCommand(String cmd, Writer writer = null){
		try{
			log.info("Running process: ${cmd}")
			Process process = cmd.execute()
			
			if(writer != null) {
				process.in.eachLine { line ->
					writer.write(line)
					writer.write(System.getProperty("line.separator"))
				}
				writer.close()
			}else{
				process.consumeProcessOutput()
			}
			process.waitFor()
			log.info("Process exit value: ${process.exitValue()}")
			if(process.exitValue() != 0){
				log.error "Process failed: ${cmd}"
/*				log.error process.text*/
				log.error "Aborting execution."
				System.exit(0)
			}
			return process.exitValue()
		}catch(Exception e){
			log.error("error executing process: ${cmd}", e)	
			return -1
		}
	}
	
	
	
	/**
	 * see http://www.javaworld.com/javaworld/javaqa/2003-07/01-qa-0711-classsrc.html?page=1
	 * Given a Class object, attempts to find its .class location [returns null
	 * if no such definition can be found]. Use for testing/debugging only.
	 * 
	 * @return URL that points to the class definition [null if not found].
	 */
	static URL getClassLocation (final Class cls)
	{
	    if (cls == null) throw new IllegalArgumentException ("null input: cls");

	    URL result = null;
	    final String clsAsResource = cls.getName ().replace ('.', '/').concat (".class");

	    final ProtectionDomain pd = cls.getProtectionDomain ();
	    // java.lang.Class contract does not specify if 'pd' can ever be null;
	    // it is not the case for Sun's implementations, but guard against null
	    // just in case:
	    if (pd != null) 
	    {
	        final CodeSource cs = pd.getCodeSource ();
	        // 'cs' can be null depending on the classloader behavior:
	        if (cs != null) result = cs.getLocation ();

	        if (result != null)
	        {
	            // Convert a code source location into a full class file location
	            // for some common cases:
	            if ("file".equals (result.getProtocol ()))
	            {
	                try
	                {
	                    if (result.toExternalForm ().endsWith (".jar") ||
	                        result.toExternalForm ().endsWith (".zip")) 
	                        result = new URL ("jar:".concat (result.toExternalForm ())
	                            .concat("!/").concat (clsAsResource));
	                    else if (new File (result.getFile ()).isDirectory ())
	                        result = new URL (result, clsAsResource);
	                }
	                catch (MalformedURLException ignore) {}
	            }
	        }
	    }

	    if (result == null)
	    {
	        // Try to find 'cls' definition as a resource; this is not
	        // documented to be legal, but Sun's implementations seem to allow this:
	        final ClassLoader clsLoader = cls.getClassLoader ();

	        result = clsLoader != null ?
	            clsLoader.getResource (clsAsResource) :
	            ClassLoader.getSystemResource (clsAsResource);
	    }

	    return result;
	}


	static String deTokenize(String str){
		def javaVersion = Float.parseFloat(System.getProperty("java.version").split("\\.")[0..1].join("."))
		while(str != null && str.indexOf("\${") >= 0){
			int s = str.indexOf("\${")
			int e = str.substring(s).indexOf("}")
			def tok = str.substring(s+2,e)
			def var = null
			if(javaVersion >= 1.5){
				var = System.getenv().get(tok)
				if(var == null) var = ""
				str = str.replace("\${${tok}}", var)
			}else{
				var = System.getProperty(tok)
				//if(var == null) var = ""
				//str = str.replace("\${${tok}}", var)
			}
		}
		return str
	}
	
	static File expandCodebase(File codebaseDir){
		def idx = codebaseDir.absolutePath.indexOf("!")
		if(!codebaseDir.exists() && idx > 0 && (codebaseDir.absolutePath.substring(0,idx).endsWith(".war") || codebaseDir.absolutePath.substring(0,idx).endsWith(".ear"))){
			//location within a war or ear
			de.schlichtherle.io.File file = new de.schlichtherle.io.File(codebaseDir.absolutePath.substring(0, idx))
			def tmpDir = new File(new File(System.getProperty("java.io.tmpdir")), file.name)
			if(!tmpDir.exists()){
				if(file.copyAllTo(tmpDir)) {
					def innerDir = new File(tmpDir, codebaseDir.absolutePath.substring(idx+1))
					log.debug("1) returning ${innerDir}")
					return innerDir
				}else {
					log.debug("2) returning ${codebaseDir}")
					return codebaseDir
				}
			}else{
				def innerDir = new File(tmpDir, codebaseDir.absolutePath.substring(idx+1))
				log.debug("3) returning ${innerDir}")
				return innerDir
			}
		}else if(!codebaseDir.isDirectory()){
			//an ear or a war
			if(codebaseDir.name.endsWith(".ear") || codebaseDir.name.endsWith(".war")){
				//explode it to temp
				de.schlichtherle.io.File file = new de.schlichtherle.io.File(codebaseDir)
				//explode the ear...
				def tmpDir = new File(new File(System.getProperty("java.io.tmpdir")), codebaseDir.name)
				file.copyAllTo(tmpDir)
				log.debug("4) returning ${tmpDir}")
				return tmpDir
			}else{
				log.debug("5) returning ${codebaseDir}")
				return codebaseDir
			}
		}else{
			//a directory
			log.debug("6) returning ${codebaseDir}")
			return codebaseDir
		}
	}
	
	static boolean isWindows(){
		log.debug("os.name=${System.getProperty('os.name')}")
		return System.getProperty("os.name") != null && System.getProperty("os.name").indexOf("Windows") >= 0
	}
}