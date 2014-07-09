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
package fi.hip.sicx.webdav;

import org.joni.test.meta.MetaFile;

import com.bradmcevoy.common.Path;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.ResourceFactory;

import fi.hip.sicx.store.MetaHandler;

/**
 * WebdavResourceFactory class.
 *
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 */
public class WebdavResourceFactory implements ResourceFactory {
	
	FolderResource ROOT = new FolderResource("root");
	MetaHandler meta = null;
	
	public WebdavResourceFactory() {

	}

	private boolean connectToMeta() {
		if(meta == null) {
			System.out.println("Connecting to meta server.");
			//Initialise meta connection
			meta = MetaHandler.getInstance();
			try {
				System.out.println("Initing metadata");
				meta.init();
			} catch (Exception ex) {
				System.out.println("Fail with exception " + ex);
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
    @Override
    public Resource getResource(String host, String strPath) {
    	
    	// Initialises connection to meta server (if necessary)
    	connectToMeta();
    	
    	if(strPath.startsWith("/webdav") == false) {
    		System.out.println("Invalid resource path: '" + strPath + "'.");
    		return null;
    	}
    	String modPath = strPath.substring(7);
    	System.out.println("getResource: '" + strPath + "' => '" + modPath + "'.");
    	   	
    	int lastIndex = modPath.lastIndexOf('/');
    	if(modPath.equals("/")) {
    		System.out.println("Path indicates for root directory.");
    		FolderResource retr = new FolderResource(modPath);
    		return retr;
    	}else if(lastIndex == (modPath.length() -1)) {
    		System.out.println("Path indicates for a directory.");
    		FolderResource retr = new FolderResource(modPath);
    		MetaFile mfile2 = retr.getMetaFile(modPath);
        	if(mfile2 == null) {
        		System.out.println("No folder found: " + modPath);
        		return null;
        	}
    		return retr;
    	}
    	
    	// Lets see if it really was a file
    	FileResource rets = new FileResource(modPath, null);
    	MetaFile mfile = rets.getMetaFile(modPath);
    	if(mfile == null) {
    		System.out.println("No resource found: " + modPath);
    		return null;
    	}
    	if(mfile.isDirectory()) {
    		System.out.println("Resource seems to be a directory: " + mfile.getName() + ".");
    		return new FolderResource(modPath);
    	}
    	
    	System.out.println("Resource seems to be a file: " + mfile.getName() + ".");
    	return rets; 
    }
    
    @SuppressWarnings("unused")
	private Resource find(Path path) {
        if (path.isRoot()) {
        	System.out.println("ROOT");
            return ROOT;
        }
        return new FileResource(path.getName(), null);
    }

}
