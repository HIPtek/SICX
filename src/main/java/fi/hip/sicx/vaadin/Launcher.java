/*
 * Copyright (C) 2012 Helsinki Institute of Physics, University of Helsinki
 * All rights reserved. See the copyright.txt in the distribution for a full 
 * listing of individual contributors.
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *      http://www.apache.org/licenses/LICENSE-2.0
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 * 
 */

package fi.hip.sicx.vaadin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.jetty.server.Connector;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.server.handler.ResourceHandler;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.URIUtil;
import org.eclipse.jetty.webapp.WebAppClassLoader;
import org.eclipse.jetty.webapp.WebAppContext;

import fi.hip.sicx.testing.JWebBrowserTest;
import fi.hip.sicx.webdav.WebdavHandler;
 
/**
 * Simple jetty launcher, which launches the webapplication from the local
 * resources and reuses the projects classpath.
 * 
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class Launcher {
 
    /** run under root context */
    private static String contextPath = "/";
    /** location where resources should be provided from for VAADIN resources */
    private static String resourceBase = "/"; 
    /** port to listen on */
    private static int httpPort = 8081;
 
    private static String[] __dftConfigurationClasses =
    {
        "org.eclipse.jetty.webapp.WebInfConfiguration",
        "org.eclipse.jetty.webapp.WebXmlConfiguration",
        "org.eclipse.jetty.webapp.MetaInfConfiguration", 
        "org.eclipse.jetty.webapp.FragmentConfiguration",        
        "org.eclipse.jetty.plus.webapp.EnvConfiguration",
        "org.eclipse.jetty.webapp.JettyWebXmlConfiguration"
    } ;
 
    /**
     * Start the server, and keep waiting.
     */
    public static void main(String[] args) throws Exception {
 
    	boolean headless = false;
    	for (String s: args) {
    		if(s.equals("headless")==true) {
    			headless = true;
    		}
    	}

    	System.setSecurityManager( null );
    	
	System.setProperty("java.naming.factory.url","org.eclipse.jetty.jndi");
	System.setProperty("java.naming.factory.initial","org.eclipse.jetty.jndi.InitialContextFactory");
 
	System.out.println(System.getProperties());
	
	
	String warFileName = null;
	ClassLoader cl = ResourceAnchor.class.getClassLoader(); 
	try {
        File tmpFile = File.createTempFile( "app-web", ".war" );
        warFileName = tmpFile.getAbsolutePath();
        OutputStream warOut = new FileOutputStream( tmpFile );
        // TODO: War file name must be retrieved from somewhere (to get correct version)
        InputStream warIn = cl.getResourceAsStream( "web.war" );
            	
        if(warIn == null) {
        	warIn = new FileInputStream("web.war"); 
        }
        
        byte[] buf = new byte[1024];
        int len;
        while( (len = warIn.read( buf )) > 0 ) {
        	System.out.println("out = " + len);
            warOut.write( buf, 0, len );
        }
        warIn.close();
        warOut.close();
    } catch( FileNotFoundException ex ) {
        // TODO: Handle exception properly
        System.err.println( ex.getMessage() + " in the specified directory." );
    } catch( IOException e ) {
        // TODO: Handle exception properly
        System.err.println( e.getMessage() );
    }
	
	Server server = new Server(); //httpPort);
	
	// Here is Vaadin admin GUI
	SelectChannelConnector connector0 = new SelectChannelConnector();
    connector0.setPort(httpPort); //8080);
    
    // Initialise WebDAV interface - no, lets use the same port
    ResourceHandler publicDocs = new ResourceHandler();
    WebdavHandler webdavHandler = new WebdavHandler();
    webdavHandler.initHandler();
    publicDocs.setHandler(webdavHandler);

    server.setConnectors(new Connector[]{ connector0 });
    
    // This is handler for Vaadin admin GUI
    WebAppContext webapp = new WebAppContext();
    webapp.setWar( warFileName );
    webapp.setContextPath( URIUtil.SLASH );
    WebAppClassLoader loader = new WebAppClassLoader( Launcher.class.getClassLoader(), webapp );
    webapp.setClassLoader( loader );
    		
    String trustStoreDir = LocalProperties.getInstance().getProperty("trustStoreDir");
    if(trustStoreDir == null || trustStoreDir.trim() == ""){
        LocalProperties.getInstance().setProperty("trustStoreDir", webapp.getResource("/truststore").getURI().getPath());
    }

    System.out.println("Path: " + LocalProperties.getInstance().getProperty("trustStoreDir"));
    
    String tmpPath = LocalProperties.getInstance().getProperty("tmpPath");
    if(tmpPath == null || tmpPath.trim() == ""){
        LocalProperties.getInstance().setProperty("tmpPath", webapp.getResource("/").getURI().getPath());
    }
    System.out.println("Path /: " + LocalProperties.getInstance().getProperty("tmpPath"));
    
    HandlerList hl = new HandlerList();
    hl.setHandlers(new Handler[] { publicDocs, webapp });
    server.setHandler(hl);
    
    server.start();
     
    // Start browser if headless mode is not requested
    if(headless == false) {
	JWebBrowserTest.main(null);
    }
    
    server.join();
    }
}
