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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joni.test.meta.ACLItem;
import org.joni.test.meta.MetaDataAPI;
import org.joni.test.meta.MetaFile;
import org.joni.test.meta.MetaFileImpl;
import org.joni.test.meta.SLA;
import org.joni.test.meta.UserInfo;

import com.bradmcevoy.http.DeletableResource;
import com.bradmcevoy.http.MoveableResource;
import com.bradmcevoy.http.QuotaResource;
import com.bradmcevoy.http.Range;
import com.bradmcevoy.http.Auth;
import com.bradmcevoy.http.CollectionResource;
import com.bradmcevoy.http.PropFindableResource;
import com.bradmcevoy.http.PutableResource;
import com.bradmcevoy.http.GetableResource;
import com.bradmcevoy.http.MakeCollectionableResource;
import com.bradmcevoy.http.Request;
import com.bradmcevoy.http.Request.Method;
import com.bradmcevoy.http.Resource;
import com.bradmcevoy.http.exceptions.BadRequestException;
import com.bradmcevoy.http.exceptions.ConflictException;
import com.bradmcevoy.http.exceptions.NotAuthorizedException;
import com.eaio.uuid.UUID;

import fi.hip.sicx.store.MetaDataClient;
import fi.hip.sicx.store.MetaHandler;
import fi.hip.sicx.sla.SLAManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Map;

/**
 * FolderResource class.
 *
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class FolderResource implements PropFindableResource, CollectionResource, 
	PutableResource, GetableResource, QuotaResource, 
	MakeCollectionableResource, DeletableResource, MoveableResource {

	private String frname;
	MetaDataAPI meta;
	SLAManager man;
	MetaFile mfile = null;

	public FolderResource(String path, MetaFile amfile) {
		frname = path;
		mfile = amfile;
	}
	
	public FolderResource(String path) {
		frname = path;
	}

	@Override
    public String getUniqueId() {
		return null;
    }

    @Override
    public String getName() {
            return frname;
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
    		System.out.println("Could not get Metafile '" + frname + "' for directory creation date:.");
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
    		System.out.println("Could not get Metafile '" + frname + "' for directory modification date:.");
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
    public Resource child(String childName) {
            return null;
    }

    @Override
    public List<? extends Resource> getChildren() {
    	
    	System.out.println("*** Listing - children *** ");
    	Long starttime = System.nanoTime();
    	
		meta = MetaHandler.getInstance().getService();

		// Things needed to get MetaFiles from meta server
        MetaDataClient mc = new MetaDataClient();
        List<MetaFile> listOfCloudFiles = new ArrayList<MetaFile>();

        System.out.println("Target folder: '" + frname +"'.");

        Long endtime = System.nanoTime();
    	System.out.println("Time elapsed (seconds): " + 0.001*0.001*0.001*(float)(endtime-starttime));
        
        MetaFile currentRoot = null;
        try {
        	currentRoot = mc.getDirectoryListing(meta, frname, null, listOfCloudFiles, false);
        } catch (Exception e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        if(currentRoot == null) {
        	return null;
        }
		
    	endtime = System.nanoTime();
    	System.out.println("Time elapsed (seconds): " + 0.001*0.001*0.001*(float)(endtime-starttime));
		
        // Now we have to get the list of MetaFiles to Resources
    	List<Resource> resources = new ArrayList<Resource>();
    	for (MetaFile mf : listOfCloudFiles) {
    		if(mf.isDirectory()) {
    			resources.add(new FolderResource(mf.getName(), mf));
    		}else {
    			resources.add(new FileResource(mf.getName(), mf));
    		}
    	}
    	
    	System.out.println("*** Listing children done *** ");
    	endtime = System.nanoTime();
    	System.out.println("Time elapsed (seconds): " + 0.001*0.001*0.001*(float)(endtime-starttime));
    	
    	return resources;
    }
    
    @Override
    public Long getContentLength() {
        return null;
    }

    @Override
    public Long getMaxAgeSeconds(Auth auth) {
    	//return null;
        return (long) 10;
    }
    
    @Override
    public String getContentType(String type) {
    	//return "text/plain";
        return null;
    }
    
    @Override // This is to list directories
    public void sendContent(OutputStream out, Range range, Map<String, String> params, 
    		String contentType) throws IOException, NotAuthorizedException, BadRequestException {
        PrintWriter pw = new PrintWriter(out);
        pw.print("<html><body>");
        pw.print("<h1>" + this.getName() + "</h1>");
        pw.print("<p>" + this.getClass().getCanonicalName() + "</p>");
        doBody(pw);
        pw.print("</body>");
        pw.print("</html>");
        pw.flush();
    }    
    
    protected void doBody(PrintWriter pw) {
        pw.print("<ul>");
        for (Resource r : this.getChildren()) {
            String href = r.getName();
            if (r instanceof CollectionResource) {
                href = href + "/";
            }
            pw.print("<li><a href='" + href + "'>" + r.getName() + "(" + r.getClass().getCanonicalName() + ")" + "</a></li>");
        }
        pw.print("</ul>");
    }

    @Override
    public Resource createNew(String newName, InputStream inputStream, Long length, String contentType) 
    			throws IOException, ConflictException, NotAuthorizedException, BadRequestException {
        
    	System.out.println("New item added: " + newName + " (type: " + contentType + ")");
    	
    	// If item with the same name exists already, lets delete it
		meta = MetaHandler.getInstance().getService();
		man = SLAManager.getInstance();
		SLAManager slaManager = SLAManager.getInstance();
        SLA sla = slaManager.getSLAByName(slaManager.getDefaultSLAName());
        MetaDataClient mc = new MetaDataClient();
        List<MetaFile> listOfCloudFiles = new ArrayList<MetaFile>();
        MetaFile currentRoot = null;
        try {
        	currentRoot = mc.getDirectoryListing(meta, frname, null, listOfCloudFiles, false);
        } catch (Exception e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        for(MetaFile f : listOfCloudFiles) {
        	if(f.getName().equals(newName)) {
        		System.out.println("File exists already. Replacing it: " + newName);
                boolean complete = mc.deleteFile(meta, man, f);
                if(complete == false) {
                	throw new NotAuthorizedException(null);
                }
        	}
        }
    	
    	int k = 5;
        int n = 7;
		
        // FIXME: this is not that pretty (comment by Joni?)
        MetaFile target = new MetaFileImpl();
        target.setSLA(sla);
        
        // FIXME: This is not either - no temp file should be needed
        File file = null;
        if(length>0) {
        file = File.createTempFile("sicx_webdav_createnew", "temp");
        OutputStream out = new FileOutputStream(file);
    	int read = 0;
    	Long lefttoread = length;
    	byte[] bytes = new byte[1024];
    	System.out.println("Array size: " + bytes.length);
    	while (lefttoread > 0) {
  			read = inputStream.read(bytes);
    		out.write(bytes, 0, read);
  			lefttoread -= read;
    	}
    	if(lefttoread != 0) {
    		System.out.println("More bytes read than needed: " + lefttoread);
    	}
     
    	//inputStream.close();
    	out.flush();
    	out.close();
        
        System.out.println("File size: " + file.length() + " = " + length);
        }
        
        // Lets get mfile
        //MetaFile mfile = new MetaFileImpl();
   	    //mfile.setName(frname);
      //mfile = mc.findMetaFile(meta, mfile.getName());
        
   	    //MetaClient mc = new MetaClient();
       	try {
   		   target = mc.uploadFile(meta, man, currentRoot,    // directory where inserted 
											 target,  // MetaFile of uploaded file
											 file,    // file to be uploaded
											 newName, // filename of the file to be created
											 true, sla, k, n, null);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
       	
		if(target == null) {
			return null;
		}else {
			return new FileResource(target.getName(), target);
			//return new ScratchpadResource(mfile.getName(), mfile);
		}
    }

	@Override
	public Long getQuotaUsed() {
		// TODO Auto-generated method stub
		//return null;
		return 1000000000000L; // 1TB
	}

	@Override
	public Long getQuotaAvailable() {
		return 1000000000000L; // 1TB
		//return null;
	}
	
    public MetaFile getMetaFile() {
    	if(mfile != null)
    		return mfile;
    	
    	if(frname == null)
    		return null;
    	
    	MetaFile target = new MetaFileImpl();
	    target.setName(frname);
	        
	    meta = MetaHandler.getInstance().getService();
       	MetaDataClient mc = new MetaDataClient();
   		try {
   			target = mc.findMetaFile(meta, target.getName());
   			this.mfile = target;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
   		
   		return target;
    }

    public MetaFile getMetaFile(String name) {
    	//MetaFile target = new MetaFileImpl();
	    //target.setName(frname);
    	MetaFile target = null;
	        
	    meta = MetaHandler.getInstance().getService();
       	MetaDataClient mc = new MetaDataClient();
   		try {
				target = mc.findMetaFile(meta, name); // target
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
   		return target;
    }
    
	@Override
	public CollectionResource createCollection(String newName)
			throws NotAuthorizedException, ConflictException,
			BadRequestException {

		System.out.println("Create new folder requested: " + newName);
		
		try {

			meta = MetaHandler.getInstance().getService();
			UserInfo user = meta.getUserInfo();
			MetaFile root = getMetaFile();
			if (root == null || !root.isDirectory())
				root = meta.getFile(user.getRoots().get(0));

			MetaFile mf = new MetaFileImpl();
			mf.setParent(root.getId());
			mf.setDirectory(true);
			mf.setName(newName);
			mf.addACLItem(new ACLItem(user.getName(), true, true));
			SLAManager slaManager = SLAManager.getInstance();
			SLA sla = slaManager.getSLAByName(slaManager.getDefaultSLAName());
			mf.setSLA(sla);
			meta.putFile(mf);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}

		return new FolderResource(newName);
	}

	@Override
	public void delete() throws NotAuthorizedException, ConflictException,
			BadRequestException {
		MetaFile target = getMetaFile();
		if(target == null) 
			throw new BadRequestException("Directory unknown!");
		
		List<? extends Resource> res = getChildren();
		if(res.size() != 0) 
			throw new BadRequestException("Directory not empty!");
		
    	meta = MetaHandler.getInstance().getService();
    	man = SLAManager.getInstance();
		
        MetaDataClient mc = new MetaDataClient();
        boolean complete = mc.deleteFile(meta, man, target);
        if(complete == false) {
        	throw new NotAuthorizedException(null);
        }
	}

	@Override
	public void moveTo(CollectionResource rDest, String name)
			throws ConflictException, NotAuthorizedException,
			BadRequestException {
	
		// Check input parameters
		if(name == null || name.isEmpty()) {
			System.err.println("Move failed, given new name is empty.");
			throw new BadRequestException("Move failed, given new name is empty.");
		}else if(rDest == null) {
			System.err.println("Move failed, target directory is null.");
			throw new BadRequestException("Move failed, target directory is null.");
		}
		
		// Check and get directory where moved
		FolderResource fr = (FolderResource) rDest;
		if(fr == null) {
			System.err.println("Move failed, destination folder not found.");
			throw new BadRequestException("Move failed, destination folder not found.");
		}
		MetaFile targetfolder = fr.getMetaFile();
		if(targetfolder == null) {
			System.err.println("Move failed, destination folder MetaFile not found.");
			throw new BadRequestException("Move failed, destination folder MetaFile not found.");
		}

		// Check and get file to be moved
		MetaFile source = null;
		if(this.mfile == null) {
			source = getMetaFile(frname);
		}else {
			source = this.mfile;
		}
		if(source == null) {
			System.err.println("Move failed, moved directory now found.");
			throw new BadRequestException("Move failed, moved directory now found.");
		}
		
		// Check that parent stays the same
		if(source.getParent() == null) {
			System.err.println("Move failed, current directory has not parent.");
			throw new BadRequestException("Move failed, current directory has not parent.");
		}
		/*
		if(!source.getParent().equals(targetfolder.getId())) {
			System.err.println("Move failed, for now we can move only inside the same directory.");
			throw new BadRequestException("Move failed, for now we can move only inside the same directory.");
		}*/

		System.out.println("Moving dir '" + source.getName() + "' to '" + fr.getName() + "/" + name + "'");
		   	        	
    	try {
    		// Moving is a bit like rename
        	meta = MetaHandler.getInstance().getService();
    		MetaDataClient mc = new MetaDataClient();
    		mc.moveDirectory(meta, targetfolder, source, name);
    	} catch (Exception e) {
			// TODO Auto-generated catch block
    		System.err.println("Move failed: " + e);
    		throw new BadRequestException("Move failed: " + e);
		}
	}
}
