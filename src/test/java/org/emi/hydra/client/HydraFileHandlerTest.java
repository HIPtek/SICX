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

package org.emi.hydra.client;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;

import org.junit.Test;

/**
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class HydraFileHandlerTest {
    static final String inputPath = "src/test/input/";

	@Test
	public void test() 
			throws 	NoSuchAlgorithmException, 
					KeyStoreException, 
					CertificateException, 
					IOException, 
					UnrecoverableEntryException {
		HydraKey hk = HydraKey.generateKey();
		HydraFileHandler.saveSingleKeyToFile(hk, new File(inputPath + "keystore.ks"), "password".toCharArray(), null);
		
		// loads the key to a SecretKey
		
		SecretKey loadedKey = HydraFileHandler.loadSingleKeyFromFile(new File(inputPath + "keystore.ks"), "password".toCharArray(), null);
		
		assertEquals("reconstructed key should match old one", new String(loadedKey.getEncoded()), new String(hk.getEncoded()));

		File keyFile = new File(inputPath + "keystore.ks");
		keyFile.delete();
		
	}

}
