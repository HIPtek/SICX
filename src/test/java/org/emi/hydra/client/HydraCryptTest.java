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

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Map;

import org.junit.Test;

/**
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class HydraCryptTest {
    static final String inputPath = "src/test/input/";
     
	/**
	 * Tests a simple random key generation, encrypting a secret message, 
	 * breaking the key with SSS, reconstructing the key and using
	 * the reconstructed key to open the encrypted message.
	 * @throws Exception
	 */
	@Test
	public void testEncryptDecrypt() throws Exception {
		
		// generates a key
		HydraKey hk1 = HydraKey.generateKey("blowfish", 128);
//		byte[] keybytes = hk1.getEncoded();
		byte[] plaintext = "This is a secret message".getBytes();
		
		byte[] ciphertext = HydraCrypt.encrypt(plaintext, hk1, "blowfish/CBC/PKCS5Padding");
		
		// splits the key
		
		Map<Integer, BigInteger> splitted_key = hk1.splitKey(3, 2);
		
		splitted_key.remove(2); // removes a key from the split
		assertEquals("we should now have two pieces of key to reconstruct the key", splitted_key.size(), 2);
		
		// reconstructs the key
		HydraKey hk2 = HydraKey.reconstructKey(splitted_key, 3, 2, null, "blowfish");
//        assertEquals("reconstructed key should match original generated key", keybytes, hk2.getEncoded());
		hk2 = hk2.padToLength(16);	// pad key to default length, 128 bits = 16 bytes
		hk2.setInitializationVector(hk1.getInitializationVector());
//		assertEquals("reconstructed key should match original generated key", keybytes, hk2.getEncoded());
		
		// decrypts the ciphertext
		byte[] reconstructed_plaintext = HydraCrypt.decrypt(ciphertext, hk2, "blowfish/CBC/PKCS5Padding");
		assertTrue("reconstruction should match byte-representation of plaintext", Arrays.equals(reconstructed_plaintext, plaintext));
		assertEquals("reconstructed text should match String-representation of plaintext", new String(reconstructed_plaintext), "This is a secret message");
		
	}
	/**
	 * Tests a file encryption and decryption
	 * @throws Exception
	 */
	@Test
	public void testFileEncrypt() throws Exception {
		HydraKey hk1 = HydraKey.generateKey();
		FileOutputStream startFile = new FileOutputStream(inputPath + "Secretmsg.txt");
		startFile.write("This is the secret message in the file".getBytes());
		startFile.close();
		//System.out.println(System.getProperty("user.dir"));
		File openfile = new File(inputPath + "Secretmsg.txt");
		InputStream cipheredStream = HydraCrypt.encryptFile(openfile, hk1, "AES/CBC/PKCS5Padding");
		FileOutputStream output = new FileOutputStream(inputPath + "CryptedSecretMsg.txt");
		int cipherByte = -2;
		// writing byte at a time. bad.
		while (cipherByte != -1) {
			cipherByte = cipheredStream.read();
			if (cipherByte != -1)
				output.write(cipherByte);
		}
		output.close();
		
		// open the crypted message
		openfile = new File(inputPath + "CryptedSecretMsg.txt");
		InputStream decipheredStream = HydraCrypt.decryptFile(openfile, hk1, "AES/CBC/PKCS5Padding");
		output = new FileOutputStream(inputPath + "PlainSecretMsg.txt");
		
		cipherByte = -2;
		// writing byte at a time. bad.
		while (cipherByte != -1) {
			cipherByte = decipheredStream.read();
			if (cipherByte != -1)
				output.write(cipherByte);
		}
		output.close();
		
		
		// org.glite.data.hydra.javacli.tests part
		InputStream originalSecret = new FileInputStream(inputPath + "Secretmsg.txt");
		InputStream reconstructedSecret = new FileInputStream(inputPath + "PlainSecretMsg.txt");
		int originalchar = -2;
		int reconstructchar = -2;
		int errors = 0;
		while (originalchar != -1 || reconstructchar != -1) {
			originalchar = originalSecret.read();
			reconstructchar = reconstructedSecret.read();
			if (originalchar != reconstructchar)
				errors++;
		}
		originalSecret.close();
		reconstructedSecret.close();
		assertEquals("Reconstruct should be identical to original", errors, 0);
		
		// delete the tempfiles
		openfile.delete();		// cryptedSecretMsg.txt
		openfile = new File(inputPath + "PlainSecretMsg.txt");
		openfile.delete();
        openfile = new File(inputPath + "Secretmsg.txt");
        openfile.delete();
		
	}

}
