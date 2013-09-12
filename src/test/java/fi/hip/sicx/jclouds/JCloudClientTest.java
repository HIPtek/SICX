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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Arrays;

import org.junit.Test;

import fi.hip.sicx.store.StorageClientObserver;

/**
 * Tests to validate that JCloudClient class is working as it should. 
 * 
 * @author Seppo Heikkila <seppo.heikkila@cern.ch> 
 */
public class JCloudClientTest 
    implements StorageClientObserver {
    
    /**
     * Tests that the interfaces work with "filesystem" service,
     * which basically stores files to the local disk. This can
     * be run often as it just use local filesystem.
     */
    @Test
    public void testCloudConnection() {
	boolean exception = false;
	//String cloudservice = "swift-keystone";
	String cloudservice = "filesystem";
	//String cloudservice = "aws-s3";
	//String cloudservice = "walrus";

	// Also with alternative constructor
	JCloudClient jcc = new JCloudClient(cloudservice); 
	if(jcc.connect() == false) {
	    System.out.printf("JCloud connection failed.\n");
	    exception = true;
	}
	assertFalse(exception);
	
	// Create temporary test file
	File inputFile = null;
	String body = "sixc_secret_data1";
	String ending = ".tmp";
	try {
		inputFile = File.createTempFile(body, ending, null);
		FileWriter outFile = new FileWriter(inputFile.getAbsolutePath());
		PrintWriter out = new PrintWriter(outFile);
		out.println("This is test data file.");
		out.close();
	}catch (IOException ex) {
		System.err.println("Cannot create temp file for test: " + ex.getMessage());
		exception = true;
	}
	assertFalse(exception);
	
	// Check if file exists (it should not) 
	@SuppressWarnings("null")
	String infile = inputFile.getName();
	if(jcc.checkFile(infile, this)) {
		System.out.printf("1File " + infile + " exists.\n");
		exception = true;
	}else {
		System.out.printf("1File " + infile + " does not exists.\n");		   		   
	}
	assertFalse(exception);
			
	// Store and get file
	if(jcc.storeFile(inputFile.getAbsolutePath(), inputFile.getName(), this)==false) {
		exception = true;
	}
	assertFalse(exception);
	if(jcc.getFile(inputFile.getName(), inputFile.getAbsolutePath()+".tmp", this)==false){
		exception = true;
	}
	assertFalse(exception);
	
	// Compare the uploaded and stored files
	File outputFile = new File(inputFile.getAbsolutePath()+".tmp");
	InputStream startStream = null;
	InputStream endStream = null;
	
	try {
		startStream = new FileInputStream(inputFile.getAbsolutePath());
		endStream = new FileInputStream(inputFile.getAbsolutePath()+".tmp");
		byte bufferStart[] = new byte[3456];
		byte bufferEnd[] = new byte[3456];
		int startRead;
		do {
			startRead = startStream.read(bufferStart);
			int endRead = endStream.read(bufferEnd);
			assertEquals(startRead, endRead);

			assertTrue(Arrays.equals(bufferStart, bufferEnd));
		} while (startRead == 3456);
	}catch (IOException ex) {
		System.err.println("Cannot read a file for test: " + ex.getMessage());
		exception = true;
	} finally{
	    try{
	    if(startStream != null){
	        startStream.close();
	    }
        if(endStream != null){
            endStream.close();
        }
	    }catch(IOException e){
	        // do nothing
	    }
	    
	}
	assertFalse(exception);
	
	// Check again if file exists (it should)
	if(jcc.checkFile(infile, this)) {
		System.out.printf("2File " + infile + " exists.\n");		   
	}else {
		System.out.printf("2File " + infile + " does not exists.\n");
		exception = true;
	}
	assertFalse(exception);
	
	// Print files and containers
	/*
	if(jcc.listContainersAndFiles() == false) {
	    exception = true;
	}
	assertFalse(exception);*/

	// Delete file from the cloud
	System.out.println("Deleting file '" + inputFile.getName() + "' from the cloud.");
	jcc.deleteFile(inputFile.getName(), this);
	
	// Test shutdown
	if(jcc.logout() == false) {
		exception = true;
	}
	assertFalse(exception);
		
	// Clean created files
	System.out.print("Cleaning files... ");
	if(cloudservice.equals("filesystem")) {
		File dellocal = new File("local/filesystemstorage/sicxhiptek/" + inputFile.getName());
		dellocal.delete();
		dellocal = new File("local/filesystemstorage/sicxhiptek");
		dellocal.delete();
		dellocal = new File("local/filesystemstorage");
		dellocal.delete();
		dellocal = new File("local");
		dellocal.delete();
	}
	outputFile.delete();
	inputFile.delete();
	System.out.println("done.");
	
	System.out.printf("Test completed successfully.\n");

    }

    /**
     * This test uploads 1MB data to a given cloud service
     * and reads it back.
     * 
     * If asynchronous upload works, you should see (e.g. with
     * ifconfig) that first half is transfered and after sleep
     * another half. This means that the file is not buffered
     * and transfered when it is complete but part by part.
     * 
     * Note that the transmitted bytes are read with Linux specific
     * system call. This means that this test works only in Linux.
     */
    //@Test
    public void testAsynchronousFileWriteAndRead() {
    	// Configure your internet interface here
		String internetInterface = "wlan0";
    	boolean exception = false;
    	
    	// Connect to cloud
    	String provider = "filesystem"; 
    	//String provider = "walrus";
    	//String provider = "aws-s3";
    	JCloudClient jcc = new JCloudClient(provider);
    	if(jcc.connect() == false) {
    	    System.out.printf("JCloud connection failed.\n");
    	    exception = true;
    	}
    	assertFalse(exception);
    	
    	// Get output stream where to write
    	int datasize = 100000;
    	String filename = "AsyncUploadTest.dat";
    	OutputStream out = jcc.writeData(filename, datasize, this);
    	
    	// Write half of the data (and call cancel between - should be catched)
    	exception = true;
     	try {
    		for(int i = 0; datasize/2>i;i++) {   
    			out.write(6);
    			if(i >= datasize/4) {
    				jcc.cancelWriteData(); // Cancel transfer
    			}
    		}
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		exception = false;
    		System.out.println("Cancel caused IO exception, which is ok (until proven otherwise).");
    	}
     	assertFalse(exception);
     	
     	// Lets wait a bit so above cancel does not disturb next test
     	try {
     		if(!provider.equals("filesystem")) { // We do not transfer anyway anything
     			Thread.sleep(500);
     		}
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
     	
     	// Lets start again
     	System.out.println("Lets restart the transfer.");
     	out = jcc.writeData(filename, datasize, this);
    	int startTXBytes = getTransmittedBytes(internetInterface, "TX");
    	int startTXBytesReal = jcc.getTransferedBytes();
    	
    	// Write half of the data
     	try {
    		for(int i = 0; datasize/2>i;i++) {   
    			out.write(6);
    		}
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
     	
     	// Wait for 5 seconds, should time out
     	System.out.println(jcc.getTransferProgress() + " percent of the data written - waiting five seconds.");
      	boolean writeInHalfIsCompleted = jcc.writeDataIsCompleted();
      	if(!provider.equals("filesystem")) { // We do not transfer anyway anything
      		jcc.writeDataWaitToComplete(5000);
      	}
     	int middleTXBytes = getTransmittedBytes(internetInterface, "TX");
     	int middleTXBytesReal = jcc.getTransferedBytes();
     	System.out.println("Finished the five second wait - going to write the other half of the data.");
    	
     	// Write 2nd half of the data and close stream
     	try {
    		for(int i = 0; datasize/2>i;i++) {   
    			out.write(6);
    		}
    		out.close();
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}

     	// Wait for 1000 seconds, should return immediately as all is transfered
     	jcc.writeDataWaitToComplete(1000000);
     	int endTXBytes = getTransmittedBytes(internetInterface, "TX");
     	int endTXBytesReal = jcc.getTransferedBytes();
     	
     	System.out.println("Write test results:");
     	if(!provider.equals("filesystem")) { // We do not transfer anyway anything 
     		System.out.println("Data transferred in 1st part: " + (middleTXBytes-startTXBytes));
     		System.out.println("Data transferred in 1st part according to counter: " + (middleTXBytesReal-startTXBytesReal));
     		System.out.println("Data transferred in 2nd part: " + (endTXBytes-middleTXBytes));
     		System.out.println("Data transferred in 2nd part according to counter: " + (endTXBytesReal-middleTXBytesReal));
     		System.out.println("Data transferred in total: " + (endTXBytes-startTXBytes));
     	}
     	System.out.println("Data transferred in total according to counter: " + (endTXBytesReal-startTXBytesReal));
     	if(!provider.equals("filesystem")) { // We do not transfer anyway anything 
     		System.out.println(jcc.getTransferProgress() + " percent of the data was written.");
     		System.out.println("Data transfer was not completed (aka " + writeInHalfIsCompleted + "), but is now: " + jcc.readDataIsCompleted());	
     		System.out.println("Uploaded data size: " + datasize);
     		double percent = ((endTXBytes-startTXBytes)-datasize)*1.0/(1.0*datasize)*100;
     		System.out.println("Transfer overhead: " + ((endTXBytes-startTXBytes)-datasize) + " bytes (" + percent + " per cent)");
     		if((endTXBytesReal-startTXBytesReal) != datasize) {
     			System.out.println("Test failed because datasize (" + datasize + ") != transfferred according to counter (" + (endTXBytesReal-startTXBytesReal) + "). which should not be even possible.");
     			assertFalse(true);
     		}

     		if((middleTXBytes-startTXBytes) >= datasize/2 &&
     				(endTXBytes-middleTXBytes) >= datasize/2) {
     			System.out.println("Write test success because data was sent in parts, i.e. when it was available.");
     		}else {
     			System.out.println("Write test failed because data was not sent in parts, i.e. when it was available.");
     			if(!provider.equals("filesystem")) {
     				assertFalse(true);
     			}
     		}
     	}
     	
     	// Lets test download also...
     	int startRXBytes = getTransmittedBytes(internetInterface, "RX");
     	InputStream in = jcc.readData(filename, datasize, this);
     	int startRXBytesReal = jcc.getTransferedBytes();
     	System.out.println("Reading 1st half of the file.");
     	try {
    		for(int i = 0; datasize/2>i;i++) {   
    			if(in.read() != 6) {
    				//System.out.println("Read failed, data read was not what expected.");
    	     		continue;
    			}
    			if(i >= datasize/4) {
    				jcc.cancelReadData(); // Cancel transfer
    			}
    		}
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
     		if(!provider.equals("filesystem")) { // Should not come with other than filesystem?
     			e.printStackTrace();
     		}
    	}
     	int cancelRXBytesReal = getTransmittedBytes(internetInterface, "RX")-startRXBytes;
     	System.out.println("Cancel test done - lets restart download.");
     	
     	// Lets try again after cancelling
     	startRXBytes = getTransmittedBytes(internetInterface, "RX");
     	in = jcc.readData(filename, datasize, this);
     	startRXBytesReal = jcc.getTransferedBytes();
     	System.out.println("Reading 1st half of the file.");
     	try {
    		for(int i = 0; datasize/2>i;i++) {   
    			if(in.read() != 6) {
    				System.out.println("Read failed, data read was not what expected.");
    	     		assertFalse(true);
    			}
    		}
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
     	System.out.println("Sleeping five seconds."); // Why? To see if the rest of the data is read if there is enough time
		try {
			if(!provider.equals("filesystem")) { // We do not transfer anyway anything 
				Thread.sleep(5000);
			}
		} catch (InterruptedException e2) {
			// TODO Auto-generated catch block
			e2.printStackTrace();
		}
     	int middleRXBytes = getTransmittedBytes(internetInterface, "RX");
     	int middleRXBytesReal = jcc.getTransferedBytes();
     	boolean readInHalfIsCompleted = jcc.readDataIsCompleted();
     	System.out.println("Reading 2nd half of the file.");
     	try {
    		for(int i = 0; datasize/2>i;i++) {   
    			if(in.read() != 6) {
    	     		System.out.println("Read failed, read data was not what expected.");
    	     		assertFalse(true);
    			}
    		}
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
     	try {
			if(in.read() != -1) {
    	     	System.out.println("Data end not recognised.");
    	     	assertFalse(true);
    		}
    		in.close();
    	} catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
     	//jcc.readDataWaitToComplete(1000000);
     	
     	// Lets wait a bit so ifconfig is updated
     	try {
     		if(!provider.equals("filesystem")) { // We do not transfer anyway anything
     			Thread.sleep(1500);
     		}
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
     	
     	int endRXBytes = getTransmittedBytes(internetInterface, "RX");
     	int endRXBytesReal = jcc.getTransferedBytes();
     	System.out.println("Read test results:");
     	if(!provider.equals("filesystem")) { // We do not transfer anyway anything 
     		System.out.println("Data transferred in 1st part: " + (middleRXBytes-startRXBytes));
     		System.out.println("Data transferred in 1st part according to counter: " + (middleRXBytesReal-startRXBytesReal));
     		System.out.println("Data transferred in 2nd part: " + (endRXBytes-middleRXBytes));
     		System.out.println("Data transferred in 2nd part according to counter: " + (endRXBytesReal-middleRXBytesReal));
     		System.out.println("Data transferred in total: " + (endRXBytes-startRXBytes));
     	}
     	System.out.println("Data transferred in total according to counter: " + (endRXBytesReal-startRXBytesReal));
     	if(!provider.equals("filesystem")) { // We do not transfer anyway anything 
     		System.out.println(jcc.getTransferProgress() + " percent of the data was read.");
     		System.out.println("Data transfer was not completed (aka " + readInHalfIsCompleted + "), but is now: " + jcc.readDataIsCompleted());	
     		System.out.println("Downloaded data size: " + datasize);
     		double percent2 = ((endRXBytes-startRXBytes)-datasize)*1.0/(1.0*datasize)*100;
     		System.out.println("Transfer overhead: " + ((endRXBytes-startRXBytes)-datasize) + " bytes (" + percent2 + " per cent)");
     		System.out.println("Bytes transferred when cancelling in the middle: " + cancelRXBytesReal);

     		if((middleRXBytes-startRXBytes) >= datasize/2 &&
     				(endRXBytes-middleRXBytes) >= datasize/2) {
     			System.out.println("Write test success because data was sent in parts, i.e. when it was available.");
     		}else {
     			System.out.println("Write test indicates that data was not sent in two completely separate parts, i.e. at least part");
     			System.out.println("of the data was sent before it was really needed, but this is ok, until proven otherwise.");
     			//System.out.println("Write test failed because data was not sent in parts, i.e. when it was available.");
     			//assertFalse(true);
     		}
     	}
		
     	System.out.println("File read back successfully.");
     	
     	// Delete uploaded file (as this is a test)
     	System.out.print("Deleting uploaded file from the cloud... ");
     	jcc.deleteFile(filename, this);
     	System.out.println("done.");
     	
     	// Log out
     	jcc.logout();
     	System.out.println("Test done.");
    }
    
    /**
     * Return TX or RX bytes from the given interface. Zero if no
     * interface or information not available. This function
     * works in Linux only - free feel however to implement
     * for other OS also. 
     * 
     * @param wlan Interface name, such "wlan0" or "eth0"
     * @param TXorRX Transmitted or Read bytes: "TX" or "RX"
     * @return number of transfered bytes
     */
    public int getTransmittedBytes(String wlan, String TXorRX) {
    	try 
    	{ 
    		Process p=Runtime.getRuntime().exec("ifconfig " + wlan); 
    		p.waitFor(); 
    		BufferedReader reader=new BufferedReader(new InputStreamReader(p.getInputStream())); 
    		String line=reader.readLine(); 
    		while(line!=null) 
    		{ 
    			int beginIndex = line.indexOf(TXorRX + " bytes:");
    			if(beginIndex >=0) {
    				line = line.substring(beginIndex+9);
    				int endIndex = line.indexOf(" (");
    				line = line.substring(0, endIndex);
    				return Integer.parseInt(line);
    			}
    			line=reader.readLine();
    		} 

    	} 
    	catch(IOException e1) {
    	    //ignore 
    	}
    	catch(InterruptedException e2) {
    	    //ignore 
    	}

    	return 0;
    }
    
    /**
     * Observer dummy implementation.
     */
    public void progressMade(int progressTotal) {
        // dummy
    }

	@Override
	public void progressMade(int progressTotal, StorageClientState uploadStatus) {
		// TODO Auto-generated method stub
		
	}

}
