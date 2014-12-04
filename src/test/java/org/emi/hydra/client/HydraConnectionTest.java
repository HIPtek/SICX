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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Date;
import java.util.Properties;

/**
 * Test class for HydraConnection interfaces Note, this org.glite.data.hydra.javacli.tests is hugely dependant on the
 * model hydra-services.xml file with predefined endpoints. And the certificates on org.glite.data.hydra.javacli.tests
 * machine should be in order or stored in resources -folder.
 * 
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author ekorhone
 */
public class HydraConnectionTest {
    static final String inputPath = "src/test/input/";

  
    public HydraSettings testSettings() throws FileNotFoundException, IOException, GeneralSecurityException {

        HydraSettings hs = new HydraSettings();
        Properties sslProps = new Properties();
        sslProps.load(new FileReader(inputPath + "meta-client-trusted.conf"));
        Properties hydraConf = new Properties();
        hydraConf.load(new FileReader(inputPath + "hydras.propeties"));
        hs.readHydraSettings(hydraConf);
        
        int connection_amount = hs.getEndpointsArray().size();
        assertEquals("There should be 3 endpoints", connection_amount, 3);

        return hs;
    }

    //Commented out 2012.10.01 by Seppo @Test
    public void testConnection() throws FileNotFoundException, IOException, GeneralSecurityException {
        HydraSettings hs = testSettings();
        for (int i = 0; i < hs.getEndpointsArray().size(); i++) {
            testVersion(hs.getEndpointsArray().get(i));
        }

    }

    public void testVersion(HydraConnection hc) {
        String serverVersion = null;
        try {
        	System.out.println("HC: " + hc.toString());
            serverVersion = hc.getServerVersion();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail("Should get version of server at endpoint");
        }

        assertEquals("Server version should be 1.x.x something", serverVersion.startsWith("1."), true);
    }

    //Commented out 2012.10.01 by Seppo @Test
    public void testKeyStorage() throws FileNotFoundException, IOException, GeneralSecurityException {
        System.out.println("Start");
        Date oldTime = new Date();
        HydraSettings hs = testSettings(); // setup the endpoints
        HydraKey original_key = HydraKey.generateKey(); // setup the original encryption key
        String original_plaintext = "This string should be encrypted."; // setup a text to be encrypted
        String dummy_filename = "dummy.txt"; // the HydraConnection -key notation requires filename and username
        String dummy_username = "CN=Seppo Heikkila,OU=Tech,O=HIP,DC=slcs,C=FI"; // define a dummy-username for storing key.
        Date newTime = new Date();
        System.out.println("setup: " + (newTime.getTime()-oldTime.getTime()));

        // encrypt the text
        oldTime = newTime;
        byte[] crypted_text = HydraCrypt.encrypt(original_plaintext.getBytes(), original_key, "AES/CBC/PKCS5Padding");

        newTime = new Date();
        System.out.println("Crypt: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        try {
            // remove key entry
            HydraConnection.removeEntries(hs, dummy_filename, dummy_username);
        } catch (Exception e) {
            System.out.println("Removing old temp entry failed. Good, maybe the previous test succeeded.");
        }
        
        newTime = new Date();
        System.out.println("removeEntries: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        
        // store the key
        newTime = new Date();
        System.out.println("CreateEntries: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        
        HydraConnection.distributeKey(hs, 2, original_key, dummy_filename, dummy_username);

        newTime = new Date();
        System.out.println("distributeKey: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        
        // reconstruct the key
        HydraKey reconstructed_key = HydraConnection.gatherKey(hs, dummy_filename, dummy_username);

        newTime = new Date();
        System.out.println("gatherKeys: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        
        // reconstruct ciphertext
        byte[] decrypted_text = HydraCrypt.decrypt(crypted_text, reconstructed_key, "AES/CBC/PKCS5Padding");
        assertTrue("Decrypted text should match original", original_plaintext.matches(new String(decrypted_text)));

        newTime = new Date();
        System.out.println("decrypt: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        
        // remove key entry
        HydraConnection.removeEntries(hs, dummy_filename, dummy_username);
    }


   static public void main(String[] args) throws FileNotFoundException, IOException, GeneralSecurityException {
        System.out.println("Start");
        Date oldTime = new Date();
        HydraConnectionTest t = new HydraConnectionTest();
        HydraSettings hs = t.testSettings(); // setup the endpoints
        HydraKey original_key = HydraKey.generateKey(); // setup the original encryption key
        String original_plaintext = "This string should be encrypted."; // setup a text to be encrypted
        String dummy_filename = "dummy.txt"; // the HydraConnection -key notation requires filename and username
        String dummy_username = "CN=Seppo Heikkila,OU=Tech,O=HIP,DC=slcs,C=FI"; // define a dummy-username for storing key.
        Date newTime = new Date();
        System.out.println("setup: " + (newTime.getTime()-oldTime.getTime()));

        // encrypt the text
        oldTime = newTime;
        byte[] crypted_text = HydraCrypt.encrypt(original_plaintext.getBytes(), original_key, "AES/CBC/PKCS5Padding");

        newTime = new Date();
        System.out.println("Crypt: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        try {
            // remove key entry
            HydraConnection.removeEntries(hs, dummy_filename, dummy_username);
        } catch (Exception e) {
            System.out.println("Removing old temp entry failed. Good, maybe the previous test succeeded.");
            e.printStackTrace(System.out);
        }
        
        newTime = new Date();
        System.out.println("removeEntries: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        
        // store the key
        newTime = new Date();
        System.out.println("CreateEntries: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        
        HydraConnection.distributeKey(hs, 2, original_key, dummy_filename, dummy_username);

        newTime = new Date();
        System.out.println("distributeKey: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        
        // reconstruct the key
        HydraKey reconstructed_key = HydraConnection.gatherKey(hs, dummy_filename, dummy_username);

        newTime = new Date();
        System.out.println("gatherKeys: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        
        // reconstruct ciphertext
        byte[] decrypted_text = HydraCrypt.decrypt(crypted_text, reconstructed_key, "AES/CBC/PKCS5Padding");
        assertTrue("Decrypted text should match original", original_plaintext.matches(new String(decrypted_text)));

        newTime = new Date();
        System.out.println("decrypt: " + (newTime.getTime()-oldTime.getTime()));
        oldTime = newTime;
        
        // remove key entry
        HydraConnection.removeEntries(hs, dummy_filename, dummy_username);
        
        //return 0;
    }

}

