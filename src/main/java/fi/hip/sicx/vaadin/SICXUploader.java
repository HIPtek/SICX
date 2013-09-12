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
import java.io.FileOutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.joni.test.meta.MetaDataAPI;
import org.joni.test.meta.MetaFile;
import org.joni.test.meta.MetaFileImpl;
import org.joni.test.meta.SLA;
import org.joni.test.meta.StripeLocation;

import fi.hip.sicx.sla.SLAManager;
import fi.hip.sicx.store.MetaDataClient;
import fi.hip.sicx.store.MetaHandler;
import fi.hip.sicx.store.StorageClient;
import fi.hip.sicx.store.StorageClientObserver;

/**
 * SICXUploader
 *
 * A threaded handler for uploading and retrieving stuff from the
 * storage elements
 *
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 * @author Joakim Koskela <jookos@gmail.com>
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class SICXUploader 
    extends Thread
    implements StorageClientObserver {

    /* instance sdata needed for completing the task */
    private SICXUploaderObserver app; 
    private File file;
    private MetaFile mfile;
    private MetaFile target;
    private SLA sla;
    private int min_stripes;
    private int max_stripes;
    
    private boolean use_encryption;
    
    // Current state of the task
    public enum UploaderState {
        QUEUED, IDLE, INITIALIZING, ACTIVE, INTERMEDIATE_RESULT, COMPLETE, ERROR, ERROR_PATH
    }
    private UploaderState uploadStatus; 
    private int progress; // Percentage
    
    private String requestType; // Possible actions to perform: list, upload, download

    private List<MetaFile> listOfCloudFiles; // For listing cloud files
    private MetaFile currentRoot;            // For listing cloud files
    private String path;                     // MetaFile path (if MetaFile instance does not exist)
    private String currentRootName;          // For listing cloud files

    // We store the storageclients so they can be 'returned' to the manager
    private List<StorageClient> jcc = null;
    StorageClient sc[];
    int sc_size;

    // list
    public SICXUploader(SICXUploaderObserver appl, String arequestType) {
        this(appl, arequestType, null, null, null, null);
    }

    // list folder or delete mfile
    public SICXUploader(SICXUploaderObserver appl, String arequestType, MetaFile mfile) {
        this(appl, arequestType, mfile, null, null, null);
    }

    // download mfile to file
    public SICXUploader(SICXUploaderObserver appl, String arequestType, MetaFile mfile, File file) {
        this(appl, arequestType, mfile, file, null, null);
    }

    // upload file to the mfile folder
    public SICXUploader(SICXUploaderObserver appl, String arequestType, 
    					MetaFile amfile, File afile, SLA sla,
    					String apath) {
 	
        this.path = apath;
    	this.app = appl;
        this.file = afile;
        this.mfile = amfile;
        this.requestType = arequestType;
        this.uploadStatus = UploaderState.IDLE;
        this.progress = 0;
        this.sla = sla;
        this.sc_size = -1;
        this.sc = null;
        this.use_encryption = true; //false;
        this.min_stripes = 5;
        this.max_stripes = 7;
        
        if (this.requestType.equals("upload")) {
        	// this is not that pretty:
        	this.target = new MetaFileImpl();
        	this.target.setSLA(sla);
        }else if (this.requestType.equals("download")) {
        	this.target = this.mfile;
        }
        
        this.listOfCloudFiles = new ArrayList<MetaFile>();
    }

    @Override
    public void run() {

        try {
            processRequest();
        } catch (Exception ex) {
            System.out.println(this.getId() + ": exception while processing: " + ex);
            ex.printStackTrace();
            uploadStatus = UploaderState.ERROR;
        } finally {
            if (uploadStatus != UploaderState.ERROR) {
                uploadStatus = UploaderState.COMPLETE;
            }
        }

        if (jcc != null) {
            SLAManager.getInstance().returnStorage(jcc);
        }
        
        app.processed(this);
    }
    
    /**
     * Separate the actual processing for easier exists & exception handling
     */
    private void processRequest() 
        throws Exception {

        setStatus(UploaderState.INITIALIZING);

	MetaDataAPI meta = MetaHandler.getInstance().getService();
        SLAManager man = SLAManager.getInstance();
        
        if (this.requestType.equals("upload")) {
        	int k = 5;
            int n = 7;
            this.min_stripes = k;
            this.max_stripes = n;
            
            // Update progress to GUI
    		progress = 25;
    		setStatus(UploaderState.INITIALIZING);

    		// Upload file
    		// TODO Retrying should happen in stripe level, not like this
    		MetaDataClient mc = new MetaDataClient();
    		int retry_times = 3;
    		while(retry_times>0) {
    			MetaFile targettmp = mc.uploadFile(meta, man, 
    								   mfile, target, 
    								   file, use_encryption,
    								   sla, k, n, this);
    			if(targettmp != null) {
    				target = targettmp;
    				break;
    			}
    			retry_times--;
    		}

        } else if (this.requestType.equals("download")) {

        	// Download file
        	MetaDataClient mc = new MetaDataClient();
        	FileOutputStream outStream = new FileOutputStream(file);
        	
        	mc.downloadFile(meta, man, this.mfile, outStream, use_encryption, this);
            
        } else if (this.requestType.equals("delete")) {

            progress = 0;
            setStatus(UploaderState.INITIALIZING);

            MetaDataClient mc = new MetaDataClient();
            boolean complete = mc.deleteFile(meta, man, mfile);

            progress = 90;
            setStatus(UploaderState.ACTIVE);

            // mark it as gone only if every single stripe was deleted!
            if (complete) {
                progress = 100;
                setStatus(UploaderState.COMPLETE);
            } else {
                setStatus(UploaderState.ERROR);
            }

        } else if (this.requestType.equals("list") || this.requestType.equals("parentlist")) {

            setStatus(UploaderState.ACTIVE);
        
            // Populate list of files
            MetaDataClient mc = new MetaDataClient();
            this.currentRoot = mc.getDirectoryListing(meta, 
						      this.path,
						      this.mfile,  
						      this.listOfCloudFiles,
						      this.requestType.equals("parentlist"));
                        
            // If the requested path does not exist
            if (this.currentRoot == null) {
            	if(mfile == null && this.path != null) {
            		System.out.println("Non-existing path: " + this.path);
            	}else {
            		System.out.println("Non-existing path.");
            	}
                setStatus(UploaderState.ERROR_PATH);
                return;
            }
            this.currentRootName = mc.getCanonicalName(meta, this.currentRoot);
            this.currentRootName = this.currentRootName.replaceAll("//", "/");
            
            System.out.println("Directory listing for path: " + currentRootName);
            System.out.println("Items in directory: " + listOfCloudFiles.size());
            
            // we aren't actually complete yet, but inform the gui so
            // it can produce a list at this point already.
            this.target = null; // signal 'listing is ready'
            setStatus(UploaderState.INTERMEDIATE_RESULT);

            // todo: update this with a more light-weight solution.
            // Calculate file availability
            if (listOfCloudFiles != null) {
                for (MetaFile mfile : listOfCloudFiles) {
                    if (mfile.isDirectory())
                        continue;
                    
                    System.out.println("Checking file('" + mfile.getName() + "'): ");
                    
                    List<StripeLocation> stripes = mfile.getStripes();
                    int pieces = 0;
                    int needed = mfile.getMinStripes();
                    if (stripes != null)
                        for (StripeLocation s : stripes) {
                            URI uri = new URI(s.getURI().toString());
                            String cloudFile = uri.getPath().substring(1);
                            //System.out.println("Getting sc for file: " + cloudFile);
                            StorageClient sc = man.getStorageClient(s.getURI(), s.getType(), s.getVersion());
                            if (sc == null)
                                continue;
                            
                            if (sc.checkFile(cloudFile, this)) {
                                pieces++;
                                System.out.println("*: " + s.getURI());
                            }
                                
                            man.returnStorage(sc);
                            //if (pieces == needed)
                            //    break;
                            
                            
                        }
                    
                    int available = 100;
                    //if (pieces < needed)
                    available = (pieces * 100) / needed;

                    // use progress for this..
                    this.progress = available;
                    this.target = mfile;
                    setStatus(UploaderState.INTERMEDIATE_RESULT);
                    System.out.println();
                }
            }

            setStatus(UploaderState.COMPLETE);
        }
    }

    private void setStatus(UploaderState state) {
        uploadStatus = state;
        app.processed(this);
    }
        
    public void progressMade(int progressTotal) {
        progress = progressTotal;
        setStatus(this.uploadStatus);
    }
    
    public void progressMade(int progressTotal, StorageClientState newuploadStatus) {
        progress = progressTotal;
        if(newuploadStatus.equals(StorageClientState.INITIALIZING)) {
        	setStatus(UploaderState.INITIALIZING);
        }else if(newuploadStatus.equals(StorageClientState.ACTIVE)) {
        	setStatus(UploaderState.ACTIVE);
        }else if(newuploadStatus.equals(StorageClientState.COMPLETE)) {
        	setStatus(UploaderState.COMPLETE);
        }else {
        	setStatus(this.uploadStatus);
        }
    }
    
    /**
     * Is the thread still running or not.
     * @return
     */
    public boolean isFinished() {
        return (uploadStatus == UploaderState.ERROR || uploadStatus == UploaderState.COMPLETE);
    }
    
    public File getFile() {
    	return this.file;
    }

    public MetaFile getMetaFile() {
    	return this.mfile;
    }
 
    public MetaFile getTargetFile() {
    	return this.target;
    }
    
    public UploaderState getUploadStatus() {
    	return this.uploadStatus;
    }
    
    public void updateProgress() {
    	double retme = 0;
    	if (this.requestType.equals("upload")) {
    		if(getUploadStatus()==UploaderState.ACTIVE) { 
    			if(jcc!=null) {
    				for(int kk = 0; jcc.size() >kk; kk++) {
    					StorageClient sc = jcc.get(kk);
    					double progresslocal = (double)sc.getTransferProgress();
    					retme += progresslocal;
    				}
    				progress = (int)(retme/jcc.size());
    			}
    		}
    	}else if (this.requestType.equals("download")) {
    		if(getUploadStatus()==UploaderState.ACTIVE) { 
    			if(sc!=null & sc_size >= 1) {
    				int count = 0;
    				for(int kk = 0; sc_size >kk; kk++) {
    					StorageClient scy = sc[kk];
    					double progresslocal = (double)scy.getTransferProgress();
    					if(progresslocal >= 0) {
    						retme += progresslocal;
    						count++;
    					}
    				}
    				// FIXME: does not seem to work
    				progress = (int)(retme/count)*5;
    			}
    		}
    	}
        app.processed(this);
    }
    

    public int getProgress() {
    	return (int)progress;
    	//return this.progress + ((this.taskProgress * this.taskProgressLimit) / 100);
    }
    
    public List<MetaFile> getListOfFiles() {
    	return this.listOfCloudFiles;
    }

    public MetaFile getCurrentRoot() {
    	return this.currentRoot;
    }

    public String getCurrentRootName() {
    	return this.currentRootName;
    }
    
    public String getRequestType() {
	    return this.requestType;
    }
    
    public int getMaxAvailabilityPercentage() {
        return (int)Math.ceil((100.0*this.max_stripes)/(1.0*this.min_stripes));
    }
}
