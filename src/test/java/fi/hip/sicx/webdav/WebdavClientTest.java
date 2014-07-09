package fi.hip.sicx.webdav;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import fi.hip.sicx.store.StorageClient;
import fi.hip.sicx.store.StorageIOException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Vector;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test WebdavClient interface.
 *
 * @author Seppo Heikkila <seppo.heikkila@cern.ch> 
 */
@Ignore
public class WebdavClientTest {
	WebdavClient wc = null;
	
	@Before
	public void init() {
		// Lets initialise default connection for the tests
		wc = new WebdavClient("localhost/esec/remote.php/webdav/", 80, "estorage", "sicx", "Shared/GoogleDAdmin");
		assertTrue(wc.connect());
	}
	
	@Test
	public void testConnect() {
				
	    // Test that null host cannot be connected
	    wc = new WebdavClient(null, 80, null, null, null);
	    assertFalse(wc.connect());
        
	    // Test that invalid port fails
	    wc = new WebdavClient("localhost", -80, null, null, null);
	    assertFalse(wc.connect());      
	}
	
	@Test 
	public void testStoreFile() {
		assertTrue(wc.storeFile("/etc/hosts", "hosts2.txt", null));		
	}
	
	@Test
	public void testGenerateValidStorageFilename() {
		String input = "7087ce50-325a-11e2-80d8-c48508379bb44";
		System.out.println(input + " => " + wc.generateValidStorageFilename(input));
		input = "71357370-325a-11e2-80d8-c48508379bb44";
		System.out.println(input + " => " + wc.generateValidStorageFilename(input));
	}
	
	@Test
	public void testMultipleConnectionOneAfterEachOther() {
		int test_connections = 3;
		ArrayList<StorageClient> clients = new ArrayList<StorageClient>();
		for(int i = 0; test_connections > i; i++) {
			StorageClient sc = new WebdavClient("localhost/oc452/remote.php/webdav/", 80, "estorage", "sicx", "Shared/GDy");
			assertTrue(sc.connect());
			assertTrue(sc.storeFile("/home/ssheikki/CV.pdf", "CV-uploadedok-" + i + ".pdf", null));
			System.out.println("Connection: " + i);
			clients.add(sc);
			if(clients.get(i).checkFile("CV-uploadedok-" + i + ".pdf", 
								 				null) == true) {
				System.out.println("Transfer ok: " + i);
			}else {
				System.out.println("Transfer fail: " + i);
			}
			
		}
	}
	
	@Test
	public void testMultipleConnectionParallel() 
			throws StorageIOException, IOException, InterruptedException {
		int test_connections = 7; // Number of connections to test
		
		ArrayList<StorageClient> clients = new ArrayList<StorageClient>();
		ArrayList<OutputStream> outs = new ArrayList<OutputStream>();
		File f = new File("/home/ssheikki/CV.pdf");
		for(int i = 0; test_connections > i; i++) {
			StorageClient sc = new WebdavClient("localhost/c/remote.php/webdav/", 80, "estorage", "sicx", "Shared/GDSsheikki");
			assertTrue(sc.connect());
			outs.add(sc.writeData("hello-world-now-ver" + test_connections + "1" + i + ".pdf", 
							 				(int)f.length(),
							 				null));
			clients.add(sc);
		}
		
		FileInputStream fis = new FileInputStream(f);
		boolean read_one_byte_at_time = true;
		int data = 0;byte[] datab = new byte[1000];
		do {
			if(read_one_byte_at_time) {
				data = fis.read();
				if(data == -1) {
					break;
				}
				for(int i = 0; test_connections > i; i++) {
					outs.get(i).write(data);
				}
			}else {
				data = fis.read(datab);
				if(data == -1) {
					break;
				}
				for(int i = 0; test_connections > i; i++) {
					outs.get(i).write(datab, 0, data);
				}
			}
			System.out.println("Progress: " + clients.get(0).getTransferProgress() + "%.");
		} while(true);
		
		for(int i = 0; test_connections > i; i++) {
			outs.get(i).close(); // This is must - otherwise file will not be uploaded.
		}
		fis.close();
		
		// Blocking wait
		boolean use_blocking_wait = false;
		if(use_blocking_wait) {
			for(int i = 0; test_connections > i; i++) {
				if(clients.get(i).writeDataWaitToComplete(60000)==true){
					System.out.println("Transfer ready: " + i);
				}else {
					System.out.println("Transfer failed: " + i);
				}
			}
		}else {
			// Non blocking wait
			for(int i = 0; test_connections > i; i++) {
				while(clients.get(i).writeDataIsCompleted()==false) {
					System.out.println("Progress(" + i + "): " + clients.get(i).getTransferProgress() + "%.");
				}
				if(clients.get(i).writeDataWaitToComplete(60000)==true){
					System.out.println("Transfer ready: " + i);
				}else {
					System.out.println("Transfer failed: " + i);
				}
			}
		}
		
		// Finally check if file was uploaded
		for(int i = 0; test_connections > i; i++) {
			if(clients.get(i).checkFile("hello-world-now-ver" + test_connections + "1" + i + ".pdf", 
							 				null) == true) {
				System.out.println("Transfer ok: " + i);
			}else {
				System.out.println("Transfer fail: " + i);
			}
		}
	}
	
	@Test
	public void testReadData() {
		InputStream is = wc.readData("52cfb800-33b8-11e2-bb49-c48508379bb45", 1, null);
		assertTrue(is != null);
		String path = "/tmp/";
		String body = "SICX_WebdavClient_testReadData";
		String ending = ".tmp";
		File dir = new File(path);
		FileOutputStream fos = null;
		try {
			File f = File.createTempFile(body, ending, dir);
			fos = new FileOutputStream(f);
			assertTrue(fos != null);
			do {
				int read_byte = is.read();
				if(read_byte != -1) {
					fos.write(read_byte);
				}else {
					break;
				}
			}while(true);
			fos.close();
			f.delete(); // Lets clean temporary files
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	@Test
	public void testCheckFile() {
		assertTrue(wc.checkFile("52cfb800-33b8-11e2-bb49-c48508379bb45", null));
		assertFalse(wc.checkFile("471c8610-3353-11e2-9326-c48508379bb46", null));
	}
	
	@Test
	public void testWriteData() {
        try {
        	String helloWorld = "Hello world!";
        	String testfilename = "hello-world5.txt"; 
        	OutputStream out = wc.writeData(testfilename, 
        									helloWorld.getBytes("UTF-8").length,
        				 				    null);
        	out.write(helloWorld.getBytes("UTF-8"));
        	out.close(); // This is must - otherwise file will not be uploaded.
        	// We have to wait for the upload to be completed
        	while(wc.writeDataIsCompleted() == false) {
        		Thread.sleep(10);
        	}
     
        	assertTrue(wc.checkFile(testfilename, null));
        	assertTrue(wc.deleteFile(testfilename, null));
        	assertFalse(wc.checkFile(testfilename, null));
        	
		} catch (StorageIOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}	
	
	@Test
	public void testGetChildrenDirectories() {
		wc.setPath("Shared"); 
		Vector<String> bebes = wc.getChildrenDirectories();
		for(String s : bebes) {
			System.out.println("Name: " + s);
		}
	}
}
