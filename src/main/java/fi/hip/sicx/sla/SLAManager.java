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
package fi.hip.sicx.sla;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.net.URI;

import fi.hip.sicx.jclouds.JCloudClient;
import fi.hip.sicx.store.*;
import fi.hip.sicx.webdav.WebdavClient;

import org.joni.test.meta.*;

/**
 * SLAManager
 *
 * The manager class for SLA- related things.  This includes the
 * policies, the actual SLAs, user accounts etc.
 * 
 * Perhaps this should be implemented using a different pattern, but
 * let's try it this way.  Perhaps some sort of 'storage manager' is
 * needed too.
 *
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Joakim Koskela <jookos@gmail.com>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 */
public class SLAManager {

    //String defaultCloudService = "walrus";
    String defaultCloudService = "filesystem";
    //String defaultCloudService = "aws-s3";
            
    String default_webdav_host = "localhost/esec/remote.php/webdav/";
    
    // yet-another singleton?
    private static SLAManager instance;
    static {
        instance = new SLAManager();
    }

    private SLAManager() {

    }

    private ArrayList<StorageClient> clients = new ArrayList();

    private Vector<String> webdavConnections = null;
    private int webdavConnectionsRoundRobinCounter = 0;
    
    /**
     * Initializes array of available connections.
     */
    public boolean init() {
    	WebdavClient ret = new WebdavClient(default_webdav_host, 
					    80, "estorage", "sicx", "Shared");
        if (!ret.connect()) {
            ret = null;
            return false;
        }
    	webdavConnections = ret.getChildrenDirectories();
    	for(int i = 0; webdavConnections.size() > i; i++) {
    		webdavConnections.set(i,  "Shared/" + webdavConnections.get(i));
    	}
    	System.out.println("WebDav connections: " + webdavConnections.size());
    	for(String s : webdavConnections) {
    		System.out.println("Webdav conn: " + s);
    	}
    	webdavConnectionsRoundRobinCounter = 0;
    	
    	return true;
    }
    
    public List<StorageClient> getStorageBySLA(SLA sla, int num) 
        throws Exception {
        
        /* TODO connection pools:

           instead of using the same connection for everything (and
           therefore have tasks wait on each other), we should create a
           stack of these.

           when a thread asks for a connection, we find a free one or
           create a new. when the thread is done, it should 'return'
           the connection here, so it will be marked as free and
           reused.

           if we (upon getting back a connection) see that there are >
           SOME_LIMIT of free connections, we just kill it (so we don't
           end up with 1 million idle, but connected, connections.

           ..also, when asking for a connection, if we see that there
           are > SOME_OTHER_LIMIT of connections being used, we just
           stall and wait until a connection is returned to us.
         */

        // todo: populate the list with as many entries as the sla dictates..
        ArrayList<StorageClient> list = new ArrayList();
        StorageClient ret = null;
        //int num = 3; // require num connections

        // testing:
        //if ((ret = getStorageClient(null, "hipstore", "1.0")) != null)
        //    list.add(ret);

        int localroundrobin = 0; // Every 2nd jclouds and ever second webdav
        int maxtries = 20; //  We should not try too much
        while (num > list.size()) {
        	if(localroundrobin == 0) {
        		ret = getStorageClient(null, "jclouds", "1.0");
        		localroundrobin = 1;
        	}else {
        		ret = getStorageClient(null, "webdav", "1.0");
        		localroundrobin = 0;
        	}
            if (ret != null) {
                list.add(ret);
            } else {
            	maxtries--;
            	if(maxtries<=0) {
            		break;
            	}
            }
        }
        
        // nag if we didn't get what we wanted..
        if (num > list.size())
            throw new Exception("Connection generation failed.\n");
        
        return list;
    }

    /**
     * returns the storage client from where the given stripe can be
     * fetched
     */
    public StorageClient getStorageClient(URI uri, String type, String version) {
        
        // 1. try the cache
        StorageClient ret = null;
        synchronized (clients) {
            for (int i=0; i < clients.size(); i++) {
                ret = clients.get(i);
                if(uri != null) {
                	System.out.println("CLIENT MATCHING: " + uri.toASCIIString().substring(0, uri.toASCIIString().lastIndexOf('/')+1) + " vs. " + ret.getURI(""));
                }
                if (ret.getType().equals(type) && 
                	(uri != null && ret.getURI("").equals(uri.toASCIIString().substring(0, uri.toASCIIString().lastIndexOf('/')+1))) &&
                	(version == null || ret.getVersion().equals(version))) {
                    ret = clients.remove(i);
                    System.out.println("* MATCH: " + ret.getURI(""));
                    return ret;
                }
            }
        }
        if(uri != null) {
        	System.out.println("* No match: " + uri.toASCIIString().substring(0, uri.toASCIIString().lastIndexOf('/')+1));
        }else {
        	System.out.println("* No match: but anything goes.");
        }
        
        // 2. create a new connection
        // some sort of factory pattern would be nice..
        if (type.equals("hipstore")) {
            try {
                ret = new HIPStoreClient();
                if (!ret.connect())
                    ret = null;
            } catch (Exception ex) {
                System.out.println("HIPStore client init failed: " + ex);
                ex.printStackTrace();
                ret = null;
            }
        } else if (type.equals("jclouds")) {
            ret = new JCloudClient(defaultCloudService);
            if (!ret.connect())
                ret = null;
        } else if (type.equals("webdav")) {
        	//ret = new WebdavClient("www.vapahtaja.com/sicx/remote.php/webdav/", 
        	//					    80, "admin", "seesam", "Shared/GoogleDrive");
        	if(webdavConnections != null && webdavConnections.size() > 0) {
        		String targetdir = null;
        		if(uri == null || uri.toASCIIString().lastIndexOf('/') == 10) {
        			// Choose randomly
        			targetdir = webdavConnections.get(webdavConnectionsRoundRobinCounter);
        			if(webdavConnectionsRoundRobinCounter + 1 >= webdavConnections.size()) {
        				webdavConnectionsRoundRobinCounter = 0;
        			}else {
        				webdavConnectionsRoundRobinCounter++;
        			}
        		}else {
        			// Choose what is set
        			targetdir = uri.toASCIIString().substring(10, uri.toASCIIString().lastIndexOf('/'));
        		}
        		System.out.println("TARGETDIR: " + targetdir);
    			ret = new WebdavClient(default_webdav_host, 
    									80, "estorage", "sicx", targetdir);
    			ret.setURI(targetdir + "/");
		
        		if (!ret.connect()) {
        			ret = null;
        		}
        	}else {
        		ret = null;
        	}
        } else {
            // ?
        }
        
        return ret;
    }

    public void returnStorage(List<StorageClient> scs) {
        synchronized (clients) {
            for (StorageClient sc : scs) {
                if (clients.size() > 5 || !sc.isReusable()) {
                    sc.logout();
                    sc = null;
                } else
                    clients.add(sc);
            }
        }
    }

    public void returnStorage(StorageClient sc) {
        synchronized (clients) {
            if (clients.size() > 5 || !sc.isReusable()) {
                sc.logout();
                sc = null;
            } else
                clients.add(sc);
        }
    }
        
    public static SLAManager getInstance() {
        return instance;
    }

    public SLA getSLAByName(String slaName) {
        return new SLA(slaName);
    }

    public String[] getAvailableSLANames() {
        return new String[] { "Premium", "Free", "Open", "Paranoid"};
    }

    public String getDefaultSLAName() {
        return getAvailableSLANames()[0];
    }
}
