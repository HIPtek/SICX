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

import java.io.InputStream;
import java.io.OutputStream;

/**
 * StorageClient
 *
 * Simple interface for the storage clients (backends) used in SICX.
 *
 * @author Joakim Koskela <jookos@gmail.com>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 */
public interface StorageClient {

    /**
     * Establish connection to the StorageClient.
     * @return true if success, else false
     */
    public boolean connect();
    
    public boolean storeFile(String localInputFilename, 
			     String fileInTheCloud, 
			     StorageClientObserver sco);
  
    /**
     * Gets a file stored in the cloud and saves to a local disk.
     * 
     * @param cloudFile The file in the cloud that is to be saved.
     * @param localOutFile The saved file name.
     * @return true if file saved successfully, otherwise false
     */
    public boolean getFile(String cloudFile, 
			   String localOutFile, 
			   StorageClientObserver sco);

    /**
     * Returns OutputStream that can be used to write data to the specified 
     * target name. The size of the file has to be known and given.
     * 
     * @param targetName Name of the data in the cloud
     * @param datasize Size of the data
     * @param sco observer
     * @return OutputStream where the data can be written
     * @throws IOStorageException 
     */
    public OutputStream writeData(String targetName, 
				  int indatasize, 
				  StorageClientObserver sco) throws StorageIOException;
    
    /**
     * Returns InputStream that can be read data from a wanted file 
     * in the cloud. 
     * Note! Might be that is not working asynchronously - or at least
     * buffers tens of megabytes of data.
     * 
     * @param fileInTheCloud Name of the file in the cloud
     * @param indatasize Tells how big the data data is which is to be read.
     * 					Used only to calculate progress of the transfer.
     * 					Note! This could be also asked from server?
     * @param sco observer
     * @return InputStream that can be used to read data from a file in the cloud
     */
    public InputStream readData(String fileInTheCloud, 
				int indatasize, 
				StorageClientObserver sco);
    
    /**
     * Check if the given file exists.
     * 
     * @param cloudFile File in the cloud
     * @param sco observer
     * @return true if exists already, if not false
     */
    public boolean checkFile(String cloudFile, 
			     StorageClientObserver sco);

    public boolean deleteFile(String cloudFile, 
			      StorageClientObserver sco);
   
    /** Close connection */
    public boolean logout();

    /**
     * Returns the percentage of required data written to OutputStream 
     * that was returned by writeData function.
     * 
     * @return Percentage done (0-100%)
     */
    public int getTransferProgress();
    
    /**
     * If writeData has been called, this function can be used to wait
     * for the write to complete.
     * 
     * @param timeout Maximum time that we wait for the completion
     * @return true if wait was done, else false
     */
    public boolean writeDataWaitToComplete(int timeout);
    
    /**
     * Tells if all the data has been written successfully, i.e.
     * the upload has finished.
     * 
     * @return true if upload has finished, otherwise false
     */
    public boolean writeDataIsCompleted();
    
    /** Returns the complete URI for the given path */
    public String getURI(String path);
    public String getType();
    public String getVersion();
    public String setURI(String newURIStart);

    /** tests whether this connection can be reused */
    public boolean isReusable();
    
}
