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
package fi.hip.sicx.streaming;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import javax.crypto.NoSuchPaddingException;

import org.emi.hydra.client.HydraCrypt;
import org.emi.hydra.client.HydraKey;
import org.emi.hydra.client.HydraSettings;
import org.junit.Test;

/**                                                
 * Class to test crypt FEC.
 *                                                 
 * @author Joni Hahkala <joni.hahkala@cern.ch>     
 */
public class CryptFECTest {
    static final String inputPath = "src/test/input/";
    static int BUFF_SIZE = 10223;
    private static Random rand = new Random();

    public HydraSettings testSettings() throws Exception {

        Properties sslProps = new Properties();
        sslProps.load(new FileReader(inputPath + "meta-client-trusted.conf"));
        HydraSettings hs = new HydraSettings(sslProps);

        return hs;
    }

    public void generateTestFile(long size) throws IOException {
        // create test file of random values of size 25*packetsize+2534 = 258534
        byte randBuffer[] = new byte[BUFF_SIZE];

        FileOutputStream outStream = new FileOutputStream("target/cryptTestData.dat");

        for (int i = 0; i < size / BUFF_SIZE; i++) {
            // for (int i = 0; i < 500; i++) {
            // for (int i = 0; i < 0; i++) {
            rand.nextBytes(randBuffer);
            outStream.write(randBuffer);
        }

        rand.nextBytes(randBuffer);
        // Arrays.fill(randBuffer, (byte)64);
        outStream.write(randBuffer, 0, (int) size % BUFF_SIZE);
        // outStream.write(randBuffer, 0, 2535);
        // outStream.write(randBuffer, 0, 5);
        outStream.flush();
        outStream.close();

    }

    @Test
    public void testCryptFECs() throws InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException, IOException {
        testCryptFEC(BUFF_SIZE * 5 - 3);
        testCryptFEC(BUFF_SIZE * 5 - 3);
        testCryptFEC(5);
        testCryptFEC(BUFF_SIZE * 5 - 8);
        testCryptFEC(BUFF_SIZE * 5 - 20);
        testCryptFEC(BUFF_SIZE * 5 - 256);
        testCryptFEC(BUFF_SIZE * 5 - 255);
//        testCryptFEC(BUFF_SIZE * 500 + 2256);
//        testCryptFEC(BUFF_SIZE * 50000 + 2256);
        testCryptFEC(905);
//        testCryptFECMissStripe(BUFF_SIZE * 50000 + 2256);
    }


    public void testCryptFEC(int size) throws NoSuchAlgorithmException, IOException, InvalidKeyException,
            NoSuchPaddingException, InvalidAlgorithmParameterException {
        int keyLength = 256;
        String algorithm = "AES";
        String cipher = "AES/CBC/PKCS5Padding";
        int stripeK = 5;
        int stripeN = 7;
        int packetSize = BUFF_SIZE;
        int extraSize = 256;
        HydraKey key = HydraKey.generateKey(algorithm, keyLength);

        Date start = new Date();
        generateTestFile(size);

        Date startStriping = new Date();
        // create input stream
        File file = new File("target/cryptTestData.dat");
        System.out.println("File size is: " + file.length());
        long fileSize = file.length();
        InputStream encryptedStream = HydraCrypt.encryptFile(file, key, cipher);
        // create stripe streams
        FileOutputStream stripeStreams[] = new FileOutputStream[stripeN];

        for (int i = 0; i < stripeN; i++) {
            stripeStreams[i] = new FileOutputStream("target/cryptTestStripe." + i);
        }

        // stripe the input stream to the output streams
        long paddingSize = StreamingFEC.stripe(encryptedStream, stripeStreams, packetSize, stripeK, stripeN, fileSize
                + extraSize);
        System.out.println("Stripe padding size is: " + paddingSize);

        // close the streams
        encryptedStream.close();
        for (int i = 0; i < stripeN; i++) {
            stripeStreams[i].flush();
            stripeStreams[i].close();
        }

        Date startConst = new Date();

        File outFile = new File("target/cryptTestOutput.dat");
        OutputStream decryptingStream = HydraCrypt.decryptingFile(outFile, key, cipher);

        FileInputStream stripeInStreams[] = new FileInputStream[stripeN];

        for (int i = 0; i < stripeN; i++) {
                stripeInStreams[i] = new FileInputStream("target/cryptTestStripe." + i);
        }

        long constructedSize = StreamingFEC.construct(stripeInStreams, decryptingStream, packetSize, stripeK, stripeN,
                fileSize + extraSize - paddingSize);

        System.out.println("constructed size is: " + constructedSize);

        // these have possibly the crypting padding
        // assertEquals(fileSize, constructedSize);

        for (int i = 0; i < stripeN; i++) {
            if (stripeInStreams[i] != null) {
                stripeInStreams[i].close();
            }
        }
        decryptingStream.flush();
        decryptingStream.close();

        Date endConst = new Date();

        InputStream origFileStream = new FileInputStream("target/cryptTestData.dat");
        InputStream decFileStream = new FileInputStream("target/cryptTestOutput.dat");

        System.out.println("decrypted size is: " + new File("target/cryptTestOutput.dat").length());
        byte bufferOrig[] = new byte[3456];
        byte bufferDecrypted[] = new byte[3456];
        int origReadLen;
        do {
            origReadLen = origFileStream.read(bufferOrig);
            int decReadLen = decFileStream.read(bufferDecrypted);
            assertEquals(origReadLen, decReadLen);

            assertTrue(Arrays.equals(bufferOrig, bufferDecrypted));
        } while (origReadLen == 3456);
        File endFile = new File("target/cryptTestOutput.dat");
        assertEquals(endFile.length(), size);
        System.out.println("Random file generation : " + (startStriping.getTime() - start.getTime()) + "ms");
        System.out.println("Striping  crypt        : " + (startConst.getTime() - startStriping.getTime()) + "ms");
        System.out.println("Reconstruction  crypt  : " + (endConst.getTime() - startConst.getTime()) + "ms");

        for (int i = 0; i < stripeN; i++) {
            File stripeFile = new File("target/cryptTestStripe." + i);
            stripeFile.delete();
        }
        origFileStream.close();
        decFileStream.close();
        File testFile = new File("target/cryptTestData.dat");
        testFile.delete();
        testFile = new File("target/cryptTestOutput.dat");
        testFile.delete();

    }


    public void testCryptFECMissStripe(int inSize) throws NoSuchAlgorithmException, IOException, InvalidKeyException,
            NoSuchPaddingException, InvalidAlgorithmParameterException {
        int size = inSize;
        int keyLength = 256;
        String algorithm = "AES";
        String cipher = "AES/CBC/PKCS5Padding";
        int stripeK = 5;
        int stripeN = 7;
        int packetSize = BUFF_SIZE;
        int extraSize = 256;
        HydraKey key = HydraKey.generateKey(algorithm, keyLength);

        Date start = new Date();
        generateTestFile(size);

        Date startStriping = new Date();
        // create input stream
        File file = new File("target/cryptTestData.dat");
        System.out.println("File size is: " + file.length());
        long fileSize = file.length();
        InputStream encryptedStream = HydraCrypt.encryptFile(file, key, cipher);
        // create stripe streams
        FileOutputStream stripeStreams[] = new FileOutputStream[stripeN];

        for (int i = 0; i < stripeN; i++) {
            stripeStreams[i] = new FileOutputStream("target/cryptTestStripe." + i);
        }

        // stripe the input stream to the output streams
        long paddingSize = StreamingFEC.stripe(encryptedStream, stripeStreams, packetSize, stripeK, stripeN, fileSize
                + extraSize);
        System.out.println("Stripe padding size is: " + paddingSize);

        // close the streams
        encryptedStream.close();
        for (int i = 0; i < stripeN; i++) {
            stripeStreams[i].flush();
            stripeStreams[i].close();
        }

        Date startConst = new Date();

        File outFile = new File("target/cryptTestOutput.dat");
        OutputStream decryptingStream = HydraCrypt.decryptingFile(outFile, key, cipher);

        FileInputStream stripeInStreams[] = new FileInputStream[stripeN];

        for (int i = 0; i < stripeN; i++) {
            if (i == 3 || i == 5) {
                stripeInStreams[i] = null;
            } else {
                stripeInStreams[i] = new FileInputStream("target/cryptTestStripe." + i);
            }
        }

        long constructedSize = StreamingFEC.construct(stripeInStreams, decryptingStream, packetSize, stripeK, stripeN,
                fileSize + extraSize - paddingSize);

        System.out.println("constructed size is: " + constructedSize);

        // these have possibly the crypting padding
        // assertEquals(fileSize, constructedSize);

        for (int i = 0; i < stripeN; i++) {
            if (stripeInStreams[i] != null) {
                stripeInStreams[i].close();
            }
        }
        decryptingStream.flush();
        decryptingStream.close();

        Date endConst = new Date();

        InputStream origFileStream = new FileInputStream("target/cryptTestData.dat");
        InputStream decFileStream = new FileInputStream("target/cryptTestOutput.dat");

        System.out.println("decrypted size is: " + new File("target/cryptTestOutput.dat").length());
        byte bufferOrig[] = new byte[3456];
        byte bufferDecrypted[] = new byte[3456];
        int origReadLen;
        do {
            origReadLen = origFileStream.read(bufferOrig);
            int decReadLen = decFileStream.read(bufferDecrypted);
            assertEquals(origReadLen, decReadLen);

            assertTrue(Arrays.equals(bufferOrig, bufferDecrypted));
        } while (origReadLen == 3456);
        File endFile = new File("target/cryptTestOutput.dat");
        assertEquals(endFile.length(), size);
        System.out.println("Random file generation    : " + (startStriping.getTime() - start.getTime()) + "ms");
        System.out.println("Striping   crypt miss     : " + (startConst.getTime() - startStriping.getTime()) + "ms");
        System.out.println("Reconstruction crypt miss : " + (endConst.getTime() - startConst.getTime()) + "ms");

        for (int i = 0; i < stripeN; i++) {
            File stripeFile = new File("target/cryptTestStripe." + i);
            stripeFile.delete();
        }
        origFileStream.close();
        decFileStream.close();
        File testFile = new File("target/cryptTestData.dat");
        testFile.delete();
        testFile = new File("target/cryptTestOutput.dat");
        testFile.delete();

    }
}
