package org.jboss.mass.mat.main;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.CodeSource;
import java.security.ProtectionDomain;

import org.apache.log4j.Logger;
import org.jboss.mass.exception.MassException;

public class Utility {
	
	public static final Logger logger = Logger.getLogger(Utility.class);
	// Construct a directory path
	static public String constructPath(String... path) {

		StringBuffer resultingPath = new StringBuffer("");

		for (int i = 0; i < path.length; i++) {
			String tmpStr = path[i];
			if (!tmpStr.endsWith(File.separator)) {
				tmpStr = tmpStr + File.separator;
			}
			resultingPath.append(tmpStr);
		}

		return resultingPath.toString();
	}
	
	static public boolean isWindows() {
		return System.getProperty("os.name") != null && System.getProperty("os.name").indexOf("Windows") >= 0;
	}
	
	
	static public int runCommand(String cmd, Writer writer) {
		try {
			logger.info("Running process: " + cmd);
			Process process = Runtime.getRuntime().exec(cmd);

			if(writer != null) {
				InputStream instream = process.getInputStream();
				BufferedReader br = new BufferedReader(new InputStreamReader(instream));
				String line = br.readLine();
				while(line != null) {
					writer.write(line);
					writer.write(System.getProperty("line.separator"));		
					line = br.readLine();
				}
//				writer.close();				
			} else {
				//XXXXXXXXXXXXXXprocess.consumeProcessOutput()XXXXXXXXXXXXXXXXXXXXX//
				InputStream instream = process.getInputStream();
				InputStream errstream = process.getErrorStream();
				
				BufferedReader br = new BufferedReader(new InputStreamReader(instream));
				BufferedReader err = new BufferedReader(new InputStreamReader(errstream));
				
				String brData = "";
				String line = br.readLine();
				while(line != null) {
					brData = brData + line;
					brData = brData + System.getProperty("line.separator");
					line = br.readLine();
				}
				
				String errData = "";
				line = err.readLine();
				while(line != null) {
					errData = errData + line;
					errData = errData + System.getProperty("line.separator");
					line = err.readLine();
				}

				
//				System.out.println("brData=" + brData);
//				System.out.println("errData=" + errData);
			}
			process.waitFor();
			int exitValue = process.exitValue();
			logger.info("Process exit value: " + exitValue);
			if(exitValue != 0) {
				logger.error("Process failed: " + cmd);
				logger.error("Aborting execution");
				System.exit(0);
			}
			
			return exitValue;
		} catch(IOException e) {
			logger.error("'" + cmd + "'" + " execution error.", e);
			return -1;
		} catch(InterruptedException e) {
			logger.error("'" + cmd + "'" + " execution error(waitFor).", e);
			return -1;
		}
	}
	
	static public URL getClassLocation(Class clz) throws Throwable {
		if(clz == null ) {
			throw new IllegalArgumentException("null input: cls");
		}
		
		URL result = null;
		final String clzAsResource = clz.getName ().replace ('.', '/').concat (".class");
	    final ProtectionDomain pd = clz.getProtectionDomain ();
	    
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
	                            .concat("!/").concat (clzAsResource));
	                    else if (new File (result.getFile ()).isDirectory ())
	                        result = new URL (result, clzAsResource);
	                }
	                catch (MalformedURLException ignore) {}
	            }
	        }
	    }

	    if (result == null)
	    {
	        // Try to find 'cls' definition as a resource; this is not
	        // documented to be legal, but Sun's implementations seem to allow this:
	        final ClassLoader clsLoader = clz.getClassLoader ();

	        result = clsLoader != null ?
	            clsLoader.getResource (clzAsResource) :
	            ClassLoader.getSystemResource (clzAsResource);
	    }

	    return result;
	}
	


}
