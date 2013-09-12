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

package fi.hip.sicx.store;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.emi.hydra.client.HydraConnection;
import org.emi.hydra.client.HydraCrypt;
import org.emi.hydra.client.HydraKey;
import org.emi.hydra.client.HydraSettings;
import org.joni.test.meta.ACLItem;
import org.joni.test.meta.MetaDataAPI;
import org.joni.test.meta.MetaFile;
import org.joni.test.meta.MetaFileImpl;
import org.joni.test.meta.SLA;
import org.joni.test.meta.StripeLocation;
import org.joni.test.meta.UserInfo;

import com.eaio.uuid.UUID;

import fi.hip.sicx.sla.SLAManager;
import fi.hip.sicx.store.StorageClientObserver.StorageClientState;
import fi.hip.sicx.streaming.StreamingFEC;
import fi.hip.sicx.vaadin.LocalProperties;

/**
 * MetaDataClient class.
 *
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class MetaDataClient {
	
	String PATH_SEPARATOR = "/";
	long EXTRA_SIZE = 256;
	
	static private HydraSettings hydraConnections = null;
	
	StorageClientObserver sco;
	
	public MetaDataClient() {
		
	}
		
	public MetaFile uploadFile(MetaDataAPI meta, SLAManager man, 
			 				   MetaFile mfile, MetaFile target, 
			 				   File file,
			 				   boolean use_encryption,
			 				   SLA sla, int k, int n,
			 				  StorageClientObserver sco) throws Exception {
		return uploadFile(meta, man, mfile, target, file, null, use_encryption, sla, k, n, sco);
	}
	
	public HydraSettings getHydraConnections() throws IOException, GeneralSecurityException{
	    if(hydraConnections == null){
	        hydraConnections = new HydraSettings();
	    }
	    return hydraConnections;
	}

	/**
	 * Uploads the given file and saves the information
	 * to meta data server.
	 * 
	 * @param meta Metadata server instance.
	 * @param man SLA manager instance.
	 * @param mfile MetaFile of the current directory (if null, root is used).
	 * @param target MetaFile of the new file (with wanted SLA set).
	 * @param file File that is to be uploaded.
	 * @param file_newname Name of the new file.
	 * @param use_encryption Encryption usage.
	 * @param sla The used SLA.
	 * @param k number of source packets to encode.
	 * @param n number of packets to encode to.
	 * 
	 * @return Metafile of the new uploaded file.
	 * 
	 * @throws Exception
	 */
	public MetaFile uploadFile(MetaDataAPI meta, SLAManager man, 
			MetaFile mfile, MetaFile target, 
			File file, String file_newname,
			boolean use_encryption,
			SLA sla, int k, int n,
			StorageClientObserver insco) throws Exception {
		// jk: choose storages based on sla
		
		this.sco = insco;
		
		// for each block: 
		//   .. crypt, stripe
		//   upload to the selected storages
		//   report update status
		int min_stripes = k;
		int max_stripes = n;
		int packetSize = 10240;
		int keyLength = 128;
		String algorithm = "AES";
		String cipher = "AES/CBC/PKCS5Padding";

		MetaFile mf = target;
		long stripePaddingSize = 0;
		long newfile_length = 0;
		ArrayList<StripeLocation> stripes = new ArrayList();
		List<StorageClient> jcc = null;
		OutputStream outStreams[] = null;
		InputStream inS = null;
		
		int progress = 0;
		progressMade(progress, StorageClientState.INITIALIZING);
		progress = 50;

		if(file != null && file.length() > 0) {
			jcc = man.getStorageBySLA(sla, n);

			// Update progress to GUI
			//progress = 50;
			//setStatus(UploaderState.INITIALIZING);
			progressMade(progress, StorageClientState.INITIALIZING);

			// todo: this should be determined by the SLA: how many
			// stripes are required to reconstruct the file!
			//int needed = jcc.size(); 

			// Get output streams where to write the stripes
			long datasize = (long)Math.ceil((double)file.length()*1.0/k/packetSize)*packetSize;
			System.out.println("Stripe size in bytes: " + datasize + " > " + file.length()*1.0/k);
			outStreams = new OutputStream[n];
			System.out.println("Number of output streams: " + n);
			for(int kk = 0; n>kk; kk++) {
				StorageClient sc = jcc.get(kk);
				//String filename = this.file.getName() + "." + kk + "." + "stripe";
				String filename = mf.getId().toString() + kk;
				System.out.print("Saving stripe '" + filename);
				outStreams[kk] = sc.writeData(filename, (int)datasize, sco);
				stripes.add(new StripeLocation(new URI(sc.getURI(filename)), sc.getType(), sc.getVersion()));
				System.out.println("' to URL: " + sc.getURI(filename).toString());
				// Update progress to GUI
				progress = (int)(50+1.0*kk/(1.0*n-1.0)*50);
				//setStatus(UploaderState.INITIALIZING);
				progressMade(progress, StorageClientState.INITIALIZING);
			}

			if(use_encryption) {
				try {
					// Crypting with hydra
					HydraKey key = HydraKey.generateKey(algorithm, keyLength);
					inS = HydraCrypt.encryptFile(file, key, cipher);
					// Store the hydra key
					String dummy_filename = mf.getId().toString();
					String dummy_username = meta.getUserInfo().getName();
					//System.out.println("Username: '" + meta.getUserInfo().getName() + "'.");
					HydraConnection.distributeKey(getHydraConnections(), 2, key, dummy_filename, dummy_username);
				}catch (Exception e) {
					e.printStackTrace(System.err);
				}
			}else{
				inS = new FileInputStream(file);
			}
			//
			//System.out.println("File " + this.file.getName() + " could not be read.");

			// stripe the input stream to the output streams
			System.out.print("Doing striping... ");
			// Update progress to GUI
			progress = 0;
			//setStatus(UploaderState.ACTIVE);
			progressMade(progress, StorageClientState.ACTIVE);
			stripePaddingSize = StreamingFEC.stripe(inS, outStreams, packetSize, k, n, file.length() + EXTRA_SIZE);
			System.out.println("done.");
			System.out.println("File striped to " + n + " stripes.");
			System.out.println("Striped file size:" + file.length() + ", stripedpadlength: " + stripePaddingSize);     

			newfile_length = file.length();

		}

		// Finish uploading
		if(stripes.size()>0) {
			try {
				progress = 0;
				while(progress < 100) {
					double progressall = 0;	
					for(int kk = 0; kk < n; kk++) {
						StorageClient sc = jcc.get(kk);
						double progresslocal = sc.getTransferProgress();
						progressall += progresslocal;
						if(progresslocal >= 100) {
							outStreams[kk].close();
						}
						//System.out.println("Progress(" + kk + "): " + progressall);
					}
					progress = (int)(progressall*1.0/n);
					//setStatus(UploaderState.ACTIVE);
					System.out.println("Progress: " + progress);
					progressMade((int)(progress/2.0), StorageClientState.ACTIVE);
					Thread.sleep(200);
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			for(int kk = 0; kk < n; kk++) {					
				StorageClient sc = jcc.get(kk);
				if(sc.writeDataWaitToComplete(100000) == false) {
					System.out.println("Wait failed: " + kk);
				}

				// Lets double check that file was uploaded ok
				if(sc.checkFile(stripes.get(kk).getURI().getRawPath().substring(1) , null) == false) {
					System.out.println("UPLOAD FAILED - RETRY SHOULD BE HAPPENING NEXT.");
					return null; // Lets fail if even one piece is missing
				}
				progress = (int)(50 + 40*kk*1.0/n);
				progressMade(progress, StorageClientState.ACTIVE);
				//System.out.println("Wait completed" + kk);
			}
			
			//setStatus(UploaderState.COMPLETE);
		}
		
		UserInfo user = meta.getUserInfo();
		if (user == null) {
			throw new Exception("User not found in the meta service!");
		} else {
			System.out.print("Updating meta data... ");

			// use the metafile as root that was given, otherwise store in root
			MetaFile root = mfile;
			if (root == null || !root.isDirectory())
				root = meta.getFile(user.getRoots().get(0));

			mf.setParent(root.getId());
			mf.setDirectory(false);
			if(file_newname == null) {
				mf.setName(file.getName());
			}else {
				mf.setName(file_newname);
			}
			mf.setLength(newfile_length);
			mf.addACLItem(new ACLItem(user.getName(), true, true));
			mf.setSLA(sla);
			mf.setMinStripes(k);
			mf.setPadLength(EXTRA_SIZE);
			mf.setStripePadLength(stripePaddingSize);
			mf.setBlockSize(packetSize);
			//mf.setK

			// todo: stripes
			if(stripes.size()>0) {
				mf.setStripes(stripes);
			}

			meta.putFile(mf);
			System.out.println("done: " + mf.getName());

			// todo: key pieces	
		}
		
		progress = 100;
		progressMade(progress, StorageClientState.COMPLETE);
		
		return mf; // or mfile?
	}
	
	/**
	 * Displays progress only if observer is not null.
	 * @param progress
	 * @param state
	 */
	private void progressMade(int progress, StorageClientState state) {
		if(this.sco!=null) {
			sco.progressMade(progress, state);
		}
	}
	
	/**
	 * Renames the directory within the same directory. It is not allowed to
	 * move between directories. The returned MetaFile should be same as original.
	 * 
	 * @param meta Metadata server instance.
	 * @param targetdir Target directory where to move (has to be same as current directory of the
	 * moved directory).
	 * @param dirmoved Directory that is moved.
	 * @param directory_newname Name of the new of the directory.
	 * 
	 * @return Metafile of the new uploaded file.
	 * 
	 * @throws Exception
	 */
	public MetaFile moveDirectory(MetaDataAPI meta, 
			                       MetaFile targetdir, MetaFile dirmoved, 
							       String directory_newname) throws Exception {
		
        // Then copy and update all the necessary parameters
		UserInfo user = meta.getUserInfo();
		if (user == null) {
			throw new Exception("User not found in the meta service!");
		}

		MetaFile root = targetdir;
		if (root == null || !root.isDirectory()) {
			root = meta.getFile(user.getRoots().get(0));
		}
		if (root == null) {
			throw new IOException("No suitable target directory was found!");
		}
		
		if (!dirmoved.isDirectory()) {	
			throw new IOException("We can only move directories here - not files!");
		}

		// Try to get old parent
		MetaFile oldparent = meta.getFile(dirmoved.getParent());
		if(oldparent == null) {
			System.err.println("Move dir failed, old parent was not found!");
			throw new IOException("Move dir failed, old parent was not found!");
		}
		
		if (dirmoved.getParent().equals(targetdir.getId())) {
			// The move is simple rename
			meta.updateFile(dirmoved.setName(directory_newname));
			return dirmoved;
		}

		// Directory is moved to another directory, update: 
		// 1) moved directory 
		// 2) new parent
		// 3) old parent
		dirmoved.setParent(targetdir.getId());
		targetdir.addFile(dirmoved);
		oldparent.removeFile(dirmoved.getId());
		meta.updateFile(dirmoved);
		meta.updateFile(targetdir);
		meta.updateFile(oldparent);
		
		//throw new IOException("We cannot move directories to other directories (at least yet)!");

		
		return dirmoved;
	}	
	
	/**
	 * Moves the given file to a new directory, also possibly with new name.
	 * The original MetaFile is deleted and new is returned.
	 *
	 * @param meta Metadata server instance.
	 * @param man SLA manager instance.
	 * @param targetdir Directory where the file is moved.
	 * @param filemoved The file that is to be moved.
	 * @param file_newname Name of the new file.
	 * 
	 * @return Metafile of the moved file.
	 * 
	 * @throws Exception
	 */
	public MetaFile moveFile(MetaDataAPI meta, SLAManager man,  
							  MetaFile targetdir, MetaFile filemoved, 
							  String file_newname) throws Exception {

		// First, create new MetaFile that is to be returned
        MetaFile mf = new MetaFileImpl();
		
        // Then copy and update all the necessary parameters
		UserInfo user = meta.getUserInfo();
		if (user == null) {
			throw new Exception("User not found in the meta service!");
		}

		MetaFile root = targetdir;
		if (root == null || !root.isDirectory()) {
			root = meta.getFile(user.getRoots().get(0));
		}
		
		if(filemoved.isDirectory()) {
			throw new IOException("We can only move files here - not directories!");
		}
		
		mf.setParent(root.getId());
		mf.setBlockSize(filemoved.getBlockSize());
		mf.setDirectory(false);
		if(file_newname == null) {
			mf.setName(filemoved.getName());
		}else {
			mf.setName(file_newname);
		}
		mf.setLength(filemoved.getLength());
		mf.addACLItem(new ACLItem(user.getName(), true, true));
		mf.setSLA(filemoved.getSLA());
		mf.setMinStripes(filemoved.getMinStripes());
		mf.setPadLength(filemoved.getPadLength());
		mf.setStripePadLength(filemoved.getStripePadLength());
		mf.setStripes(filemoved.getStripes());
		//mf.setKeyPieces(filemoved.getKeyPieces());
		
    	String dummy_filename = filemoved.getId().toString();
    	String dummy_username = meta.getUserInfo().getName();
    	HydraKey reconstructed_key = HydraConnection.gatherKey(getHydraConnections(), dummy_filename, dummy_username);
    	HydraConnection.removeEntries(getHydraConnections(), dummy_filename, dummy_username);
		String dummy_filename2 = mf.getId().toString();
		String dummy_username2 = meta.getUserInfo().getName();
		HydraConnection.distributeKey(getHydraConnections(), 2, reconstructed_key, dummy_filename2, dummy_username2);

		meta.putFile(mf);
		
		// Last, remove the old metafile (not the data)
        MetaFile parent;
        try {
        	parent = meta.getFile(filemoved.getParent());
        	if (parent != null) {
        		parent.removeFile(filemoved.getId());
        		meta.updateFile(parent);
        	}
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }

		return mf; // or mfile?
	}
	
	/**
	 * Delete the given Metafile and the data.
	 * 
	 * @param meta
	 * @param man
	 * @param mfile
	 * @return
	 */
	public boolean deleteFile(MetaDataAPI meta, SLAManager man,
						   MetaFile mfile) {
		StorageClientObserver sco = null;
        if (mfile.isDirectory()) {
            // .. check if it is empty etc.
        	//System.out.println("MetaClien: deleting directories not implemented - yet.");
        	//return false;
        } else {
        	if(mfile.getLength() != 0) {
        	
            // get stripes, connections:
            List<StripeLocation> stripes = mfile.getStripes();
            for (StripeLocation s : stripes) {
                StorageClient sc = man.getStorageClient(s.getURI(), s.getType(), s.getVersion());
                //initSubTask(90/stripes.size());
                if (sc != null) {
                    URI uri;
					try {
						uri = new URI(s.getURI().toString());
					
                    String cloudFile = uri.getPath().substring(1);
                    if(sc.deleteFile(cloudFile, sco) == false) {
                        System.out.printf("File " + mfile.getName() + " delete failed.\n");		   
                        return false;
                    }
                    man.returnStorage(sc);
					} catch (URISyntaxException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
                }
                //subTaskComplete();
            }
        	}
        }
   
        // Remove encryption keys
        try {
        	String dummy_filename = mfile.getId().toString();
        	String dummy_username = meta.getUserInfo().getName();
        	HydraConnection.removeEntries(getHydraConnections(), dummy_filename, dummy_username);
        }catch(Exception e) {
        	System.out.println("Failed to remove hydra encyption keys: " + e);
        }
        
        // All pieces were deleted so we can remove the file reference
        MetaFile parent;
        try {
        	parent = meta.getFile(mfile.getParent());
        	if (parent != null) {
        		parent.removeFile(mfile.getId());
        		meta.updateFile(parent);
        	}
        } catch (IOException e) {
        	// TODO Auto-generated catch block
        	e.printStackTrace();
        }
        
        return true;
	}
	
	/**
	 * Finds a MetaFile corresponding to the given path. Search is implemented in the
	 * server side to minimise network delay overhead.
	 * 
	 * @param meta Meta server instance.
	 * @param path Path to be searched.
	 * @return null Metafile in the path, null if path is not found
	 * @throws Exception
	 */
	public MetaFile findMetaFile(MetaDataAPI meta, String path) throws Exception {
		System.out.println("findMetaFile, finding path: " + path);
		
		MetaFile retme = null; 
		try {
			retme = meta.getFileByPath(path);
		}catch(FileNotFoundException e) {
			System.out.println("File '" + path + "' not found: " + e);
			return null;
		}
		
		return retme;
	}

	/**
	 * This is same as findMetaFile-function but does the search in local
	 * client than in the server. This makes the function much slower (~10x) if 
	 * there is long delays between the server and client. 
	 * 
	 * @param meta
	 * @param name
	 * @return
	 * @throws Exception
	 */
	public MetaFile findMetaFileLocal(MetaDataAPI meta, String name) throws Exception {

		if(name==null)
			return null;
		
		String totoken = name;
		StringTokenizer st = new StringTokenizer(totoken, PATH_SEPARATOR);
		
		MetaFile root = null;
		// Check that root exists
		if(st.hasMoreTokens()) {
			String t = st.nextToken();
			for (UUID uuid: meta.getUserInfo().getRoots()) {
				root = meta.getFile(uuid);
				if (root.getName().equals(t)) {
					break;
				} else {
					root = null;
				}
			}
		}    
		
		// Find if rest of the path exists
		while (root != null && st.hasMoreTokens()) {
			String t = st.nextToken(); 
			if (root.listFiles() != null) {
				for (UUID id : root.listFiles()) {
					root = meta.getFile(id);
					if (root.getName().equals(t))
						break;
					else
						root = null;
				}
			} else {
				root = null;
			}
		}

		// If null == path does not exist
		if (root == null) {
			return null;
		}
		
		System.out.println("Metafile found: " + root.getName());
		return root;
	}
		
	/**
	 * Gets list of files and directories from Meta server.
	 * 
	 * @param meta Metafile storage connection instance.
	 * @param apath Path to be fetched, e.g. /SecureRoot/test/test.txt. Used only if amfile parameter is null. 
	 * @param amfile Director to be listed. Apath parameter is used if amfile is null.
	 * @param listOfCloudFiles List of cloudfiles to be populated.
	 * @param currentRoot The current directory. 
	 * @param parent True if parent should be listed, false if current path.
	 * @param currentRootName Name of the current directory.
	 * 
	 * @return Metafile of current directory
	 */
	public MetaFile getDirectoryListing(MetaDataAPI meta, String apath, MetaFile amfile, 
			List<MetaFile> listOfCloudFiles, boolean parent) throws Exception {
		
		System.out.println("*** Listing getchildren NOW *** ");
		Long starttime = System.nanoTime();
		Long endtime = System.nanoTime();
		
		// We do not accept null MetaFile
		if(apath==null && amfile==null) {
			System.out.println("We cannot list null metafile.");
			return null;
		}else if(listOfCloudFiles == null) {
			System.out.println("We cannot save MetaFiles to null List.");
			return null;
		}
			
		// If listing of roots is requested
		if((apath != null && apath.equals("/")) ||
		   (amfile != null && amfile.getName().equals("/")) ||
		   (parent == true && amfile != null && amfile.getParent()==null)) {
			UserInfo ui = meta.getUserInfo();	    
			//	MetaFile entry = null;
			for (UUID uuid: ui.getRoots()) {
				try {
					listOfCloudFiles.add(meta.getFile(uuid));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if(listOfCloudFiles.size()==0) {
				System.out.println("No roots found.");
				return null;
			}
			amfile = new MetaFileImpl();
			amfile.setName("/");
			System.out.println("Roots found:" + listOfCloudFiles.size());
			return amfile;
		}

		
		MetaFile mfile = amfile;
		if(amfile == null) {
			mfile = findMetaFile(meta, apath);
		}else {
			apath = ".../" + amfile.getName();
		}
		if(mfile==null) {
			System.out.println("We cannot list null metafile (was not found).");
			return null;
		}
		System.out.println("Processing dir: " + apath);
		
	    endtime = System.nanoTime();
		System.out.println("FTime elapsed (seconds): " + 0.001*0.001*0.001*(float)(endtime-starttime));
		
		// Populate list of files to be displayed (if not populated already)
		if (listOfCloudFiles.size() == 0) {
			List<MetaFile> mlist = null;
			if(parent == true) {
				mlist = meta.getListFile(mfile.getParent());
			}else {
				try {
					mlist = meta.getListFile(mfile.getId());
				}catch (Exception e) {
					System.out.println("Target was not found: " + e);
					return null;
				}
			}
			if(mlist != null && mlist.size() != 0) {
				for(MetaFile met: mlist) {
					if(met == mlist.get(0)) {
						mfile=met; // First item is the directory itself
						continue;
					}
					listOfCloudFiles.add(met);
				}
			}
			/*
			for (UUID uuid: root.listFiles()) {
				MetaFile entry = meta.getFile(uuid);
				listOfCloudFiles.add(entry);
			}*/
			endtime = System.nanoTime();
			System.out.println("FFTime elapsed (seconds): " + 0.001*0.001*0.001*(float)(endtime-starttime));
		}
		//currentRoot = root;

		endtime = System.nanoTime();
		System.out.println("FTime elapsed (seconds): " + 0.001*0.001*0.001*(float)(endtime-starttime));

		String currentRootName = getCanonicalName(meta, mfile); //root);

		System.out.println("Directory listing for path: " + currentRootName);
		System.out.println("Items in directory: " + listOfCloudFiles.size());

		return mfile;
	}
	
    /**
     * Reports that progress for the currently executing task has been
     * made
     */
    public void progressMade(int progressTotal) {
    	
    }
	
    /**
     * Downloads a file based on the given metafile.
     * 
     * @param meta Meta server instance.
     * @param man SLA server instance.
     * @param mfile2 Metafile to be downloaded.
     * @param file File where the file is downloaded to.
     * @param use_encryption True if encryption is used, false if not.
     * 
     * @return true if download was successful.
     * @throws Exception
     */
	public boolean downloadFile(MetaDataAPI meta, SLAManager man, 
							 MetaFile mfile2, OutputStream outStream, 
							 boolean use_encryption,
							 StorageClientObserver insco) throws Exception {
		this.sco = insco;
		int progress = 0;
		
		// TODO: Zero size files are special case
//		if(mfile2.getLength()==0) {
//			file.createNewFile();
//			return file;
//		}
		
		MetaFile mfile = mfile2; 
		StorageClient sc[];
        int k = 5;
        int n = 7;
        int packetSize = 10240;
        List<StripeLocation> stripes = mfile.getStripes();

        //OutputStream decryptingStream = HydraCrypt.decryptingFile(outFile, key, cipher);
        InputStream inS[] = new InputStream[n];
        sc = new StorageClient[n];
        int ind = 0;
        for (StripeLocation s : stripes) {
            System.out.println("processing stripe " + s.getURI());
            sc[ind] = man.getStorageClient(s.getURI(), s.getType(), s.getVersion());
            if (sc[ind] == null) {
                continue;
        	}
            URI uri = new URI(s.getURI().toString());
            String cloudFile = uri.getPath().substring(1);
            
            if (!sc[ind].checkFile(cloudFile, sco)) {
                System.out.println("No stripe " + cloudFile + " to get " + mfile.getName() + ".");
                inS[ind] = null;
            	//continue;
            }else {
            	System.out.println("Downloading stripe " + cloudFile + " to get " + mfile.getName() + ".");
            	inS[ind] = sc[ind].readData(cloudFile, (int)mfile.getLength(), sco);
            }
            ind++;
            
    		// Update progress to GUI
    		progress = (int)(50+1.0*ind/(1.0*n-1.0)*40);
            progressMade(progress, StorageClientState.INITIALIZING);
        }

        // Check that we have enough stripes
        int pieces = 0;
        for(InputStream inputs : inS) {
        	if(inputs != null) {
        		pieces++;
        	}
        }
        if(pieces < mfile.getMinStripes()) {
        	throw new IOException("Not enough stripes to construct the file: " + mfile.getName());
        }
    
        OutputStream decryptingStream = null;
        progress = 50;
        progressMade(progress, StorageClientState.ACTIVE);
        if (use_encryption) {
            HydraKey reconstructed_key = null;
            try {
                // Get keys
                String dummy_filename = mfile.getId().toString();
                String dummy_username = meta.getUserInfo().getName();
                System.out.println("Decrypting file: " + mfile.getName());
                reconstructed_key = HydraConnection.gatherKey(getHydraConnections(), dummy_filename, dummy_username);

            } catch (Exception e) {
                e.printStackTrace(System.err);
                // TODO: should clean up temp files etc.
                throw e;
            }
            // Reconstruct data
            decryptingStream = HydraCrypt.decryptingStream(outStream, reconstructed_key, "AES/CBC/PKCS5Padding");
        } else {
            decryptingStream = outStream; // mfile.getName());
        }

        long constructedSize = StreamingFEC.construct(inS, decryptingStream, packetSize, k, ind, mfile.getLength()+EXTRA_SIZE-mfile.getStripePadLength());
        System.out.println("File '" + mfile.getName() + " constructed from " + ind + " stripes and written to file.");
        System.out.println("Constructed file size:" + constructedSize + ", length: " + mfile.getLength() + ", stripedpadlength: " + mfile.getStripePadLength());     
        
        progress = 100;
        progressMade(progress, StorageClientState.ACTIVE);
        
        // Make sure read has finished (padding is read also) and return storage
        for(int i=0;ind>i;i++){
        	if(inS[i] != null) {
        	  while(inS[i].read() != -1);
        	  	inS[i].close();        	  
        	  	man.returnStorage(sc[i]);
        	}
        }
        
        decryptingStream.flush();
        decryptingStream.close();
                
        progress = 100;
        progressMade(progress, StorageClientState.COMPLETE);
        
		return true;
	}
	
	
	/**
	 * Returns Canonical name of the given metafile.
	 * 
	 * @param meta Interface to meta server.
	 * @param root File which canonical path is to be determined.
	 * @return
	 * @throws Exception
	 */
	public String getCanonicalName(MetaDataAPI meta, MetaFile root) throws Exception {
		UUID rootid = null;
		
    // Get full path
    String canonicalName = "";
    if(root != null) {
    	canonicalName = root.getName();
    	while ((rootid = root.getParent()) != null) {
    		root = meta.getFile(rootid);
    		canonicalName = root.getName() + PATH_SEPARATOR + canonicalName;
    	}
    }
    //if (canonicalName.length() == 0)
    String currentRootName = PATH_SEPARATOR + canonicalName;
    
    return currentRootName;
	}
	
}
