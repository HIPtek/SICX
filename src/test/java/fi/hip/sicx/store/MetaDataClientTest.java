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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.joni.test.meta.MetaDataAPI;
import org.joni.test.meta.MetaFileImpl;
import org.joni.test.meta.SLA;
import org.junit.Test;

import fi.hip.sicx.sla.SLAManager;

import org.joni.test.meta.*;

/**
 * Tests to validate that meta data client works.
 * 
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch> 
 */

public class MetaDataClientTest implements StorageClientObserver {

	@Test 
	public void testUploadFile() throws IOException {
		// Initialise meta connection
        MetaHandler metah = MetaHandler.getInstance();
        try {
            System.out.println("Initing metadata");
            metah.init();
        } catch (Exception ex) {
            System.out.println("Fail with exception " + ex);
            ex.printStackTrace();
        }

		MetaDataAPI meta = MetaHandler.getInstance().getService();
        SLAManager man = SLAManager.getInstance();
        
        SLAManager slaManager = SLAManager.getInstance();
        slaManager.init();
        String[] slanames = slaManager.getAvailableSLANames();
        SLA sla = slaManager.getSLAByName(slanames[0]);
        
        MetaFile target = new MetaFileImpl();
        target.setSLA(sla);
        
        File file = File.createTempFile("sicx_webdavCV.pdf", "temp");
        
		MetaDataClient mc = new MetaDataClient();
		try {
			// Upload
			target = mc.uploadFile(meta, man, 
					   	           null, target, 
					   	           file, true,
					   	           sla, 5, 7, this);
			assertTrue(target != null);
			
			// Checking file
			System.out.println("*********** STARTING FILE CHECK *************");
			
			
			// Download
			System.out.println("*********** STARTING DOWNLOAD *************");
			File download_here = File.createTempFile("sicx_webdavtest.pdf", "temp");
			FileOutputStream outputStream = new FileOutputStream(download_here);
			boolean result = mc.downloadFile(meta, man, target, 
			                                        outputStream, true,
												   this);
			assertTrue(result);
			
			System.out.println("Downloaded file name: " + download_here.getAbsolutePath());
			System.out.println("Downloaded file size: " + download_here + " = " + download_here.length());
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
	}

	@Override
	public void progressMade(int progressTotal) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void progressMade(int progressTotal, StorageClientState uploadStatus) {
		// TODO Auto-generated method stub
		System.out.println();
		System.out.println(uploadStatus + ": " + progressTotal);
		System.out.println();
	}
}
