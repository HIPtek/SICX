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

package fi.hip.sicx.jclouds;

import static org.jclouds.concurrent.FutureIterables.awaitCompletion;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.ArrayList;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.apache.commons.io.input.CountingInputStream;
import org.jclouds.ContextBuilder;
import org.jclouds.blobstore.BlobStore;
import org.jclouds.blobstore.BlobStoreContext;
import org.jclouds.blobstore.BlobStoreContextFactory;
import org.jclouds.blobstore.domain.Blob;
import org.jclouds.blobstore.domain.PageSet;
import org.jclouds.blobstore.domain.StorageMetadata;
import org.jclouds.blobstore.domain.StorageType;
import org.jclouds.blobstore.util.BlobStoreUtils;
import org.jclouds.filesystem.reference.FilesystemConstants;
import org.jclouds.logging.Logger;
import org.jclouds.logging.config.ConsoleLoggingModule;
import org.jclouds.openstack.keystone.v2_0.config.CredentialTypes;
import org.jclouds.openstack.keystone.v2_0.config.KeystoneProperties;
import org.jclouds.openstack.v2_0.ServiceType;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.inject.Module;

import fi.hip.sicx.store.StorageClient;
import fi.hip.sicx.store.StorageClientObserver;

/**
 * Class to interface cloud services.
 * 
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 * @author Joakim Koskela <jookos@gmail.com>
 */
public class JCloudClient implements StorageClient {

    // Connection parameters
    private String provider;
    private String identity;
    private String credential;
    private String containerName;

    // JCloud blobstore
    private Properties properties;
    private BlobStoreContext context;
    private BlobStore blobStore;

    // Specific to writeData() function
    private PipedInputStream in;
    private PipedOutputStream out;
    private DataOutputStream dos;
    private CountingInputStream cis;
    private Blob writeToBlob;
    private Map<Blob, Future<?>> responses;
    private int datasize;
    private ArrayList<Future<Blob>> futures;
    private String fileInTheCloud;
    private File fs_outfile; 
    private FileOutputStream fs_outstream; 

    public JCloudClient() {
        // Default "cloud storage" is local file system
        this.provider = "filesystem";
        this.identity = "foo";
        this.credential = "bar";
        this.containerName = "sicxhiptek";
        this.datasize = 0;
        this.dos = null;
        this.cis = null;
        this.fs_outstream = null;
    }

    @SuppressWarnings("deprecation")
    public JCloudClient(String aprovider) {
        this.provider = aprovider;
        this.datasize = 0;
        this.dos = null;
        this.cis = null;

        // Set default account settings in case walrus, filesystem, etc.
        if (this.provider.equals("walrus")) {
            this.identity   = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
            this.credential = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
            this.containerName = "sicxhiptek";
        } else if (this.provider.equals("aws-s3")) {
            this.identity   = "xxxxxxxxxxxxxxxxxxxx";                               
            this.credential = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
            this.containerName = "seppo-2013-03-04-test"; // Note that the bucket/container has to exist already 
        } else if (this.provider.equals("greenqloud-aws-s3")) {
            this.identity   = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";	    
            this.credential = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx";
            this.containerName = "sicxhiptek";
        } else if (this.provider.equals("swift-keystone")) {
            this.identity   = "xxxxxxxxxxx";
            this.credential = "xxxxxxxxxxx";
            this.containerName = "myfiles";
        } else if (this.provider.equals("filesystem")) {
            this.identity = "foo";
            this.credential = "bar";
            this.containerName = "sicxhiptek";
        }

        // Args
        if (!Iterables.contains(BlobStoreUtils.getSupportedProviders(), provider))
            throw new IllegalArgumentException("provider " + provider + " not in supported list: "
                    + BlobStoreUtils.getSupportedProviders());
    }

    /**
     * Connect to the cloud.
     * 
     * @return true if success, otherwise false
     */
    @SuppressWarnings("deprecation")
	public boolean connect() {
        // Init
        properties = new Properties();

        if (provider.equals("filesystem")) {
            // Setup where the provider must store the files
            properties.setProperty(FilesystemConstants.PROPERTY_BASEDIR, "./local/filesystemstorage");
            properties.setProperty("jclouds.identity", this.identity);     // For meaning of these lines, see:
            properties.setProperty("jclouds.credential", this.credential); // http://code.google.com/p/jclouds/issues/detail?id=735#c2
            context = new BlobStoreContextFactory().createContext(provider, properties);
        } else if (provider.equals("aws-s3")) {
            properties.setProperty("aws-s3.endpoint", "http://xxxxxx.cern.ch:5080");
            context = new BlobStoreContextFactory().createContext("aws-s3", identity, credential, // accesskeyid, secretkey
                    ImmutableSet.<Module> of(new ConsoleLoggingModule()), properties);
        } else if (provider.equals("greenqloud-aws-s3")) {
            properties.setProperty("aws-s3.endpoint", "https://s.greenqloud.com");
            context = new BlobStoreContextFactory().createContext("aws-s3", identity, credential, // accesskeyid, secretkey
                    ImmutableSet.<Module> of(new ConsoleLoggingModule()), properties);
        } else if (provider.equals("swift-keystone")) {
            this.containerName = "myfiles";
            properties.setProperty(KeystoneProperties.SERVICE_TYPE, ServiceType.OBJECT_STORE); // Object storage (SWIFT)
            properties.setProperty(KeystoneProperties.CREDENTIAL_TYPE, CredentialTypes.PASSWORD_CREDENTIALS);

            properties.setProperty("swift-keystone.endpoint", "http://127.0.0.1:5000/v2.0/");
            properties.setProperty("jclouds.identity", "xxxxxxxxx");
            properties.setProperty("jclouds.credential", "xxxxxxxx");
	    context = ContextBuilder.newBuilder("swift-keystone").credentials("admin:admin", "seesam")
		      .endpoint("http://172.16.1.2:5000/v2.0/").build(BlobStoreContext.class);
        } else if (provider.equals("walrus")) {
            properties.setProperty("walrus.endpoint", "http://ecc.eucalyptus.com:8773/services/Walrus");
            context = new BlobStoreContextFactory().createContext("walrus", identity, credential, // accesskeyid, secretkey
                    ImmutableSet.<Module> of(new ConsoleLoggingModule()), properties);
        } else {
            context = new BlobStoreContextFactory().createContext(provider, identity, credential);
        }
        System.out.printf("BlobStore connected: '%s', '%s', '%s'.%n", provider, identity, credential);
        blobStore = context.getBlobStore();

        // Create Container
        if (blobStore.containerExists(containerName)) {
            System.out.printf("Container '%s' not created: exists already.\n", containerName);
        } else {
            if (blobStore.createContainerInLocation(null, containerName) == false) {
                System.out.printf("Container '%s' creation failed.\n", containerName);
            }
        }

        return true;
    }

    /**
     * Stores file from filesystem to the cloud.
     * 
     * @param localInputFilename File name and path of the file to be uploaded from local filesystem
     * @param fileInTheCloud Name of the file in the cloud
     * @param StorageClientObserver observer
     */
    public boolean storeFile(String localInputFilename, String currentFileInTheCloud, StorageClientObserver sco) {
        File input = new File(localInputFilename);
        if (input.isFile()) {
            Blob blob = blobStore.blobBuilder(currentFileInTheCloud).payload(input).build();
            blobStore.putBlob(containerName, blob);
            if (blobStore.blobExists(containerName, currentFileInTheCloud) == false) {
                System.out.printf("File '%s' upload as '%s' failed.\n", localInputFilename, currentFileInTheCloud);
            } else {
                System.out.printf("File '%s' uploaded as '%s'\n", localInputFilename, currentFileInTheCloud);
            }
        } else {
            System.out.printf("File '%s' does not exist.\n", localInputFilename);
        }
        return true;
    }

    /**
     * If writeData has been called, this function can be used to wait for the write to complete.
     * 
     * @param timeout Maximum time that we wait for the completion
     * @return true if wait was done, else false
     */
    public boolean writeDataWaitToComplete(int timeout) {

        // Filesystem is missing this feature so this is a workaround for now
        if (this.provider.equals("filesystem")) {
            if (this.dos.size() >= this.datasize) {
                try {
                    storeFile(this.fs_outfile.getCanonicalPath(), fileInTheCloud, null);
                    this.fs_outfile.delete();
                } catch (IOException e) {
                    e.printStackTrace();
                    return false;
                }
            } else {
                try {
                    Thread.sleep(timeout);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        if (this.responses.get(this.writeToBlob).isCancelled()) {
            return false;
        }

        Long maxTime = new Long(timeout);
        Logger logger = Logger.NULL;

	awaitCompletion(this.responses, this.context.utils().userExecutor(),
			maxTime, logger, String.format("putting into containerName: %s", containerName));

        return true;
    }

    /**
     * Returns InputStream that can be read data from a wanted file in
     * the cloud. Note! Might be that is not working asynchronously -
     * or at least buffers tens of megabytes of data.
     * 
     * @param currentFileInTheCloud Name of the file in the cloud
     * @param indatasize Tells how big the data data is which is to be
     *        read. Used only to calculate progress of the
     *        transfer. Note! This could be also asked from server?
     * @param sco observer
     * @return InputStream that can be used to read data from a file in the cloud
     */
    public InputStream readData(String currentFileInTheCloud, int indatasize, StorageClientObserver sco) {
        this.dos = null;
        this.datasize = indatasize;

        this.futures = new ArrayList<Future<Blob>>();
        this.futures.add(this.context.getAsyncBlobStore().getBlob(containerName, currentFileInTheCloud));
        for (Future<Blob> future : this.futures) {
            try {
                this.cis = new CountingInputStream(future.get().getPayload().getInput());
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        return this.cis;
    }

    /**
     * Returns OutputStream that can be used to write data to the
     * specified filename in the cloud. The size of the file has to be
     * known and given.
     * 
     * @param fileInTheCloud Name of the file in the cloud
     * @param datasize Size of the file
     * @param sco observer
     * @return OutputStream that can be used to write data to the file in the cloud
     */
    public OutputStream writeData(String infileInTheCloud, int indatasize, StorageClientObserver sco) {

        // Save parameters
        this.datasize = indatasize;
        this.fileInTheCloud = infileInTheCloud;

        // Filesystem is missing this feature so this is a workaround for now
        if (this.provider.equals("filesystem")) {
            // Create temporary test file
            String body = "sixc_secret_data1" + infileInTheCloud;
            String ending = ".tmp";
            try {
                this.fs_outfile = File.createTempFile(body, ending, null);
                this.fs_outstream = new FileOutputStream(this.fs_outfile);
                this.dos = new DataOutputStream(this.fs_outstream); // This is needed to know the number of bytes
                                                                    // written
            } catch (IOException e) {
                e.printStackTrace();
            }
            return this.dos;
        }

        // Lets make pipe where data can be written
        this.cis = null;
        this.in = new PipedInputStream();
        this.out = null;
        try {
            this.out = new PipedOutputStream(this.in);
            this.dos = new DataOutputStream(this.out); // This is needed to know the number of bytes written
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.writeToBlob = blobStore.blobBuilder(fileInTheCloud).payload(in).contentLength(this.datasize).build();
        this.responses = Maps.newHashMap();

        this.responses.put(this.writeToBlob, context.getAsyncBlobStore().putBlob(containerName, writeToBlob));

        return dos;
    }

    /**
     * Cancels the writing of the data to the cloud. After the writing
     * has been cancelled, writing to OutputStream will throw IO
     * exception. Thus, cancel first all possible write operations and
     * only then call this function.
     * 
     * @return true if cancel success, otherwise false
     */
    public boolean cancelWriteData() {

        // Filesystem is missing this feature so this is a workaround for now
        if (this.provider.equals("filesystem")) {
            try {
                this.dos.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
            return true;
        }

        if (this.dos != null) {
            this.responses.get(this.writeToBlob).cancel(true);
            try {
                this.dos.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }

        return true;
    }

    /**
     * Cancels reading of the data from the cloud. After the reading has been cancelled, reading from the InputStream
     * will not be possible.
     * 
     * @return true if cancel success, otherwise false
     */
    public boolean cancelReadData() {
        if (this.cis != null) {
            for (Future<Blob> future : this.futures) {
                future.cancel(true);
            }
            try {
                this.cis.close();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the amount of bytes written to OutputStream that was returned by writeData function.
     * 
     * @return Number of bytes written
     */
    public int getTransferedBytes() {
        if (this.dos != null) {
            return dos.size();
        } else if (this.cis != null) {
            return cis.getCount();
        }

        return 0;
    }

    /**
     * Tells if all the data has been written successfully, i.e. the upload has finished.
     * 
     * @return true if upload has finished, otherwise false
     */
    public boolean writeDataIsCompleted() {
        // Filesystem is missing this feature so this is a workaround for now
        if (this.provider.equals("filesystem")) {
            if (getTransferedBytes() == this.datasize) {
                return true;
            } else {
                return false;
            }

        }

        if (this.dos != null) {
            return this.responses.get(this.writeToBlob).isDone();
        }
        return true;
    }

    /**
     * Tells if all the data has been read successfully, i.e. the
     * download has finished. Note! Not necessarily optimal
     * implementation (see writeDataIsCompleted for good one).
     * 
     * @return true if upload has finished, otherwise false
     */
    public boolean readDataIsCompleted() {
        if (this.cis != null) {
            if (this.getTransferProgress() == 100) {
                return true;
            }
            return false;
            // for (Future<Blob> future : this.futures) {
            // return future.isDone();
            // }
        }
        return true;
    }

    /**
     * Returns the percentage of required data written to OutputStream
     * that was returned by writeData function.
     * 
     * @return Percentage done (0-100%)
     */
    public int getTransferProgress() {
        double retme = 0;
        if (this.dos != null && this.datasize > 0) {
            retme = 100.0 * dos.size() * 1.0 / (1.0 * this.datasize);
        } else if (this.cis != null && this.datasize > 0) {
            retme = 100.0 * cis.getCount() * 1.0 / (1.0 * this.datasize);
        }

        return (int) retme;
    }

    /**
     * Gets a file stored in the cloud and saves to a local disk.
     * 
     * @param cloudFile The file in the cloud that is to be saved.
     * @param localOutFile The saved file name.
     * @return true if file saved successfully, otherwise false
     */
    public boolean getFile(String cloudFile, String localOutFile, StorageClientObserver sco) {

        if (getFile(context.createInputStreamMap(containerName), cloudFile, localOutFile, sco) == false) {
            System.out.printf("Failed to save file: '%s'\n", cloudFile);
        } else {
            System.out.printf("File '%s' saved as: '%s'.\n", cloudFile, localOutFile);
        }

        return true;
    }

    /**
     * Gets a file stored in the cloud and saves to a local disk.
     * 
     * @param containerMap
     * @param cloudFile The file in the cloud that is to be saved.
     * @param localOutFile The saved file name.
     * @return true if file saved successfully, otherwise false
     */
    public boolean getFile(Map<String, InputStream> containerMap, String cloudFile, String localOutFile,
            StorageClientObserver sco) {
        try {
            InputStream inputStream = containerMap.get(cloudFile);
            File f = new File(localOutFile);
            OutputStream outStream = new FileOutputStream(f);
            byte buf[] = new byte[1024];
            int len;
            while ((len = inputStream.read(buf)) > 0)
                outStream.write(buf, 0, len);
            outStream.close();
            inputStream.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }

    /**
     * Check if the given file exists in the cloud.
     * 
     * @param cloudFile File in the cloud
     * @param sco observer
     * @return true if exists already, if not false
     */
    public boolean checkFile(String cloudFile, StorageClientObserver sco) {
        return blobStore.blobExists(containerName, cloudFile);
    }

    /**
     * Deletes the given file from the cloud.
     * 
     * @param cloudFile File in the cloud.
     * @param sco observer
     * @return true if deleted successfully
     */
    public boolean deleteFile(String cloudFile, StorageClientObserver sco) {
        blobStore.removeBlob(containerName, cloudFile);
        return true;
    }

    /**
     * Prints containers and files in the cloud. Not supported for
     * filesystem cloud.
     * 
     * @return true if all ok
     */
    public boolean listContainersAndFiles() {

        // List Buckets/Containers/Folders
        for (StorageMetadata resourceMd : blobStore.list()) {
            if (resourceMd.getType() == StorageType.CONTAINER || resourceMd.getType() == StorageType.FOLDER) {
                // Use Map API
                Map<String, InputStream> containerMap = context.createInputStreamMap(resourceMd.getName());
                System.out.printf("  %s: %s entries%n", resourceMd.getName(), containerMap.size());
                // List files
                for (Map.Entry<String, InputStream> entry : containerMap.entrySet()) {
                    System.out.println("File = " + entry.getKey()); // + ", Value = " + entry.getValue());
                }
            } else {
                System.out.printf("What is this type: NA\n");
            }
        }

        return true;
    }

    /**
     * Lists files that are in the specified container.
     * 
     * @return List of containers separated by ";".
     */
    public String listFiles() {
        return listFiles(this.containerName);
    }

    /**
     * Lists files that are in the specified container.
     * 
     * @param containerName Name of the container that is to be listed.
     * @return List of containers separated by ";".
     */
    public String listFiles(String currentContainerName) {
        String retme = null;
        System.out.println("creating map");
        PageSet<? extends StorageMetadata> pages = blobStore.list(currentContainerName);

        System.out.println("map created");

        // List files
        int counter = 0;
        for (StorageMetadata entry : pages) {
            System.out.println("adding " + counter);
            if (counter == 0) {
                retme = entry.getName();
            } else {
                retme = retme + ";" + entry.getName();
            }
            counter++;
        }

        return retme;
    }

    /**
     * Close connection
     * 
     * @return true if close ok
     */
    public boolean logout() {
        context.close();

        return true;
    }

    public String getURI(String path) {
        return "jclouds:///" + path;
    }

    public String getType() {
        return "jclouds";
    }

    public String getVersion() {
        return "1.0";
    }

    /**
     * Tests whether this connection can be reused
     * 
     * @return true if reusable, otherwise false
     */
    public boolean isReusable() {
        return true;
    }

    @Override
    public String setURI(String newURIStart) {
        return null;
    }

}
