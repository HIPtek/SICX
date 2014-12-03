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

import java.util.Properties;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;

/**
 * LocalProperties
 *
 * A simple persistent properties maintainer. Clumsy, but gets the
 * job done.
 *
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Joakim Koskela <jookos@gmail.com>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 */
public class LocalProperties extends Properties {

    /**
     * 
     */
    private static final long serialVersionUID = -1188329485293714635L;
    // yes, more singletons.
    static private LocalProperties instance = new LocalProperties();
    
    public static LocalProperties getInstance() {
        return instance;
    }

    /** The file name of the persistent store */
    private String storeFileName = System.getProperty("user.home") + System.getProperty("file.separator") + ".sicx";

    private LocalProperties() {
        super();

        // try to load the properties file. don't worry if it doesn't
        // succeed (actually we should worry..)
        try {
        	File configFile = new File(storeFileName);
        	if(!configFile.exists()) {
        		generateNewSicxConfigFile();
        		System.out.println("New SICX config file created: " + storeFileName);
        		store(new FileWriter(configFile), "Automatically generated SICX config file.");
        	}else {
              FileInputStream fis = new FileInputStream(storeFileName);
              super.load(fis);
              fis.close();
        	}
        	
        } catch (Exception ex) {
            System.out.println("Could not load the properties from '" + storeFileName + "'");
        }
    }

    /** store the properties after each put */
    public synchronized Object setProperty(String key,
                              String value) {
        
        Thread.dumpStack();
        
        Object ret = super.setProperty(key, value);
        
        // TODO: remove writing to the file!
        // best effort..
        try {
//            File f = new File(storeFileName);
            FileOutputStream fos = new FileOutputStream(storeFileName);
            super.store(fos, "SICX local configuration");
            fos.close();
        } catch (Exception ex) {
            System.out.println("Could not save the properties to '" + storeFileName + "'");
        }
        return ret;
    }
    
    private boolean createDirIfNotExists(String strDirectory) {
    	boolean success = (new File(strDirectory)).mkdir();
    	if (success) {
    	  System.out.println("Directory: " + strDirectory + " created.");
    	}  
    	return success;
    }
    
    public void generateNewSicxConfigFile() {
    	
    	String sicx_data_dir = System.getProperty("user.home") + System.getProperty("file.separator") + ".sicx_data";
    	String sicx_local_dir = System.getProperty("user.home") + System.getProperty("file.separator") + "Desktop";
    	String truststore_dir = sicx_data_dir + System.getProperty("file.separator") + "truststore";
    	
    	// Create directories
    	createDirIfNotExists(sicx_data_dir); 
    	createDirIfNotExists(sicx_local_dir);
    	createDirIfNotExists(truststore_dir);
    	
    	super.setProperty("sslKey", sicx_data_dir + System.getProperty("file.separator") +  "trusted_client.priv");
    	super.setProperty("folder.local", sicx_local_dir);
    	super.setProperty("sslCertFile", sicx_data_dir + System.getProperty("file.separator") +  "trusted_client.cert");
    	super.setProperty("sslKeyPasswd", "changeit");
    	super.setProperty("metaService", "https://sicx1.hip.helsinki.fi:40669/MetaService");
    	super.setProperty("trustStoreDir", truststore_dir);
    		
    }
}
