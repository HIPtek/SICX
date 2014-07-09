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

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import java.util.Map;

import org.joni.test.meta.MetaDataAPI;
import org.joni.test.meta.MetaFile;

import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.FileItem;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;

import fi.hip.sicx.sla.SLAManager;
import fi.hip.sicx.store.MetaDataClient;
import fi.hip.sicx.store.MetaHandler;

/**
 * FileResource class.
 *
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class FileResource implements com.bradmcevoy.http.FileResource {

        private static final int BUFF_SIZE = 419600;
        private String frname = "";
        MetaDataAPI meta;
        SLAManager man;
        MetaFile mfile;

        public FileResource(String name, MetaFile amfile) {
            frname = name;
        	mfile = amfile;
        }
        
        public String getFilename() {
                return frname;
        }

        @Override
        public String getUniqueId() {
        	return null;
        }

        @Override
        public String getName() {
                return getFilename();
        }

        @Override
        public Object authenticate(String user, String password) {
                return "anonymous";
        }

        @Override
        public boolean authorise(Request request, Method method, Auth auth) {
                return true;
        }

        @Override
        public String getRealm() {
                return null;
        }

        @Override
        public Date getCreateDate() {
        	Date retme;
        	MetaFile target = null;
        	
        	// Get the real metafile instance
        	if(this.mfile == null) {
        		target = getMetaFile(frname);
        	}else {
        		target = this.mfile;
        	}

        	if(target == null) {
        		System.out.println("Could not get Metafile for file creation date.");
        		return null;
        	}
        	long mdate = target.getCreated();
    		retme = new Date(mdate);
    		
        	return retme;
        }

        @Override
        public Date getModifiedDate() {
        	Date retme;
        	MetaFile target = null;
        	
        	// Get the real metafile instance
        	if(this.mfile == null) {
        		target = getMetaFile(frname);
        	}else {
        		target = this.mfile;
        	}

        	if(target == null) {
        		System.out.println("Could not get Metafile for file modification date.");
        		return null;
        	}
        	long mdate = target.getLastModified();
    		retme = new Date(mdate);
    		
        	return retme;
        }

        @Override
        public String checkRedirect(Request request) {
                return null;
        }

        @Override
        public Long getMaxAgeSeconds(Auth auth) {
        		return (long) 60;
                //return null;
        }

        @Override
        public String getContentType(String accepts) {
                return "text/plain";
        }

        private MetaFile getMetaFile() {
        	if(mfile != null)
        		return mfile;
        	
        	if(frname == null) {
        		System.out.println("File resource name was empty.");
        		return null;
        	}
       		
       		return getMetaFile(frname);
        }
        
        public MetaFile getMetaFile(String name) {
        	//MetaFile target = new MetaFileImpl();
   	        //target.setName(frname);
        	MetaFile target = null;
   	        
   	        meta = MetaHandler.getInstance().getService();
           	MetaDataClient mc = new MetaDataClient();
       		try {
   				target = mc.findMetaFile(meta, name); // target
   				this.mfile = target;
   			} catch (Exception e) {
   				// TODO Auto-generated catch block
   				e.printStackTrace();
   				return null;
   			}
       		return target;
        }
        
        @Override
        public Long getContentLength() {
            MetaFile target = null;

        	// Get the real metafile instance
        	if(this.mfile == null) {
        		target = getMetaFile(frname);
        	}else {
        		target = this.mfile;
        	}
    		
    		if(target==null) {
    			System.out.println("Contents length (" + frname + "): zero.");
    			return null;
    		}else {
    			System.out.println("Contents length (" + frname + "): " + target.getLength());
    			return target.getLength();
    		}
        }
        
    @Override
    public void sendContent(OutputStream out, Range range, Map<String, String> params, String contentType)
            throws IOException, NotAuthorizedException, BadRequestException {

        meta = MetaHandler.getInstance().getService();
        man = SLAManager.getInstance();

        // Things needed to get MetaFiles from meta server
        MetaDataClient mc = new MetaDataClient();

        MetaFile target = getMetaFile();
        if (target.getLength() == 0) {
            out.close();
            return;
        }

        try {
            System.out.println("Downloading file:" + target.getName() + '.');

            mc.downloadFile(meta, man, target, out, true, null);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        System.out.println("Finished to download a file:" + frname + '.');

    }

		@Override
		public void delete() throws NotAuthorizedException, ConflictException,
									BadRequestException {
			MetaFile target = null;

			// Get the real metafile instance
			if(this.mfile == null) {
				target = getMetaFile(frname);
			}else {
				target = this.mfile;
			}
			System.out.println("About to delete file: " + target.getName());
			
        	meta = MetaHandler.getInstance().getService();
        	man = SLAManager.getInstance();
			
            MetaDataClient mc = new MetaDataClient();
            boolean complete = mc.deleteFile(meta, man, target);
            if(complete == false) {
            	throw new NotAuthorizedException(null);
            }
            
		}

		@Override
		public void copyTo(CollectionResource toCollection, String name)
				throws NotAuthorizedException, BadRequestException,
				ConflictException {
			System.out.println("\n*** Copy requrested but not implemented. ***\n");
		}

		@Override
		public void moveTo(CollectionResource rDest, String name)
				throws ConflictException, NotAuthorizedException,
				BadRequestException {
			
			// Check input parameters
			if(name == null || name.isEmpty()) {
				throw new BadRequestException("Move failed, target file name is empty.");
			}else if(rDest == null) {
				throw new BadRequestException("Move failed, target directory is null.");
			}
			
			// Check and get directory where moved
			FolderResource fr = (FolderResource) rDest;
			
			// Check and get file to be moved
			MetaFile source = null;
			if(this.mfile == null) {
				source = getMetaFile(frname);
			}else {
				source = this.mfile;
			}
			System.out.println("Moving file '" + source.getName() + "' to '" + fr.getName() + "/" + name + "'");
			
			// Moving is a bit like rename
        	meta = MetaHandler.getInstance().getService();
        	man = SLAManager.getInstance();
        	        	
        	try {
        		MetaFile targetfolder = fr.getMetaFile();
        		if(targetfolder == null) {
        			throw new BadRequestException("Move failed, destination folder MetaFile not found.");
        		}
        		MetaDataClient mc = new MetaDataClient();
        		mc.moveFile(meta, man, targetfolder, source, name);
        	} catch (Exception e) {
				// TODO Auto-generated catch block
        		throw new BadRequestException("Move failed: " + e);
			}
		}

		@Override
		public String processForm(Map<String, String> parameters,
				Map<String, FileItem> files) throws BadRequestException,
				NotAuthorizedException, ConflictException {
			System.out.println("\n*** Postable Resource requrested but not implemented. ***\n");
			return null;
		}

}
