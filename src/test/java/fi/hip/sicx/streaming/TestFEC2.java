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
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import javax.crypto.NoSuchPaddingException;

import org.junit.Test;

import com.onionnetworks.fec.FECCode;
import com.onionnetworks.fec.FECCodeFactory;
import com.onionnetworks.util.Buffer;

/**
 * Class to test FEC2.
 *
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class TestFEC2 {
    private static Random rand = new Random();
    static int BUFF_SIZE = 10223;

    public void generateTestFile(long size) throws IOException {
        // create test file of random values of size 25*packetsize+2534 = 258534
        byte randBuffer[] = new byte[BUFF_SIZE];

        FileOutputStream outStream = new FileOutputStream("target/fecTestData.dat");

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
    public void testCryptFECs() throws IOException, InterruptedException {
        testFileStreamFEC(BUFF_SIZE * 5 - 3);
        testFileStreamFEC(BUFF_SIZE * 5 - 3);
        testFileStreamFEC(5);
        testFileStreamFEC(BUFF_SIZE * 5 - 8);
        testFileStreamFEC(BUFF_SIZE * 5 - 20);
        testFileStreamFEC(BUFF_SIZE * 5 - 256);
        testFileStreamFEC(BUFF_SIZE * 5 - 255);
//        testFileStreamFEC(BUFF_SIZE * 500 + 2256);
//        testFileStreamFEC(BUFF_SIZE * 50000 + 2256);
        testFileStreamFEC(905);
    }
   
    
    public void testFileStreamFEC(int size) throws IOException, InterruptedException {
        // k = number of source packets to encode
        // n = number of packets to encode to
        int k = 5;
        int n = 7;
        int packetSize = BUFF_SIZE;
        int extraSize = 256;

        Date start = new Date();
        generateTestFile(size);

        Date startStriping = new Date();

        // create stripe streams
        FileOutputStream stripeStreams[] = new FileOutputStream[n];

        for (int i = 0; i < n; i++) {
            stripeStreams[i] = new FileOutputStream("target/fecTestStripe." + i);
        }

        // create input stream
        File file = new File("target/fecTestData.dat");
        System.out.println("File size is: " + file.length());
        long fileSize = file.length();
        FileInputStream inStream = new FileInputStream("target/fecTestData.dat");

        // stripe the input stream to the output streams
        long paddingSize = StreamingFEC.stripe(inStream, stripeStreams, packetSize, k, n, fileSize
                + extraSize);
        System.out.println("padding size = " + paddingSize);

        inStream.close();
        for (int i = 0; i < n; i++) {
            stripeStreams[i].flush();
            stripeStreams[i].close();
        }
        Date startConst = new Date();

        FileOutputStream outputStream = new FileOutputStream("target/fecTestOutput.dat");

        FileInputStream stripeInStreams[] = new FileInputStream[n];

        for (int i = 0; i < n; i++) {
            stripeInStreams[i] = new FileInputStream("target/fecTestStripe." + i);
        }

        long constructedSize = StreamingFEC.construct(stripeInStreams, outputStream, packetSize, k, n, size + 256 - paddingSize);

        for (int i = 0; i < n; i++) {
            stripeInStreams[i].close();
        }
        outputStream.flush();
        outputStream.close();
        Date endConst = new Date();

        assertEquals(size, constructedSize);

        InputStream startStream = new FileInputStream("target/fecTestData.dat");
        InputStream endStream = new FileInputStream("target/fecTestOutput.dat");

        byte bufferStart[] = new byte[3456];
        byte bufferEnd[] = new byte[3456];
        int startRead;
        do {
            startRead = startStream.read(bufferStart);
            int endRead = endStream.read(bufferEnd);
            assertEquals(startRead, endRead);

            assertTrue(Arrays.equals(bufferStart, bufferEnd));
        } while (startRead == 3456);
        System.out.println("Random file generation : " + (startStriping.getTime() - start.getTime()) + "ms");
        System.out.println("Striping  plain        : " + (startConst.getTime() - startStriping.getTime()) + "ms");
        System.out.println("Reconstruction  plain  : " + (endConst.getTime() - startConst.getTime()) + "ms");

        
        for (int i = 0; i < n; i++) {
            File stripeFile = new File("target/fecTestStripe." + i);
            stripeFile.delete();
        }
        File testFile = new File("target/fecTestData.dat");
        testFile.delete();
        testFile = new File("target/fecTestOutput.dat");
        testFile.delete();

    }

    @Test
    public void testArrayFEC2() {
        // k = number of source packets to encode
        // n = number of packets to encode to
        int k = 5;
        int n = 7;
        int packetsize = 10240;
        byte source[] = new byte[k * packetsize]; // this is our source file

        // NOTE: The source needs to split into k*packetsize sections
        // So if your file is not of the right size you need to split
        // it into groups. The final group may be less than
        // k*packetsize, in which case you must pad it until you read
        // k*packetsize. And send the length of the file so that you
        // know where to cut it once decoded.

        // this is just so we have something to encode
        rand.nextBytes(source);

        // this will hold the encoded file
        byte[] repair = new byte[n * packetsize];

        // These buffers allow us to put our data in them they
        // reference a packet length of the file (or at least will once
        // we fill them)
        Buffer[] sourceBuffers = new Buffer[k];
        Buffer[] stripeBuffers = new Buffer[n];

        for (int i = 0; i < sourceBuffers.length; i++) {
            sourceBuffers[i] = new Buffer(source, i * packetsize, packetsize);
        }

        for (int i = 0; i < stripeBuffers.length; i++) {
            stripeBuffers[i] = new Buffer(repair, i * packetsize, packetsize);
        }

        // When sending the data you must identify what it's index was.
        // Will be shown and explained later
        int[] repairIndex = new int[n];

        for (int i = 0; i < repairIndex.length; i++) {
            repairIndex[i] = i;
        }

        // create our fec code
        FECCode fec = FECCodeFactory.getDefault().createFECCode(k, n);

        // encode the data
        fec.encode(sourceBuffers, stripeBuffers, repairIndex);
        // encoded data is now contained in the repairBuffer/repair byte array

        // From here you can send each 'packet' of the encoded data, along with
        // what repairIndex it has. Also include the group number if you had to
        // split the file

        // We only need to store k, packets received
        // Don't forget we need the index value for each packet too
        Buffer[] receiverBuffer = new Buffer[n - 2];
        int[] receiverIndex = new int[n - 2];

        // this will store the received packets to be decoded
        byte[] received = new byte[(n - 2) * packetsize];

        // We will simulate dropping every even packet
        int j = 0;
        for (int i = 0; i < n; i++) {
            if (i == 2 || i == 6)
                continue;
            byte[] packet = stripeBuffers[i].getBytes();
            System.out.println("i = " + i + " j = " + j + " buffoffset " + j * packetsize + " len = " + packet.length
                    + " bufflen = " + (n - 1) * packetsize);
            System.arraycopy(packet, 0, received, j * packetsize, packet.length);
            receiverIndex[j] = i;
            j++;
        }

        // create our Buffers for the encoded data
        for (int i = 0; i < n - 2; i++) {
            receiverBuffer[i] = new Buffer(received, i * packetsize, packetsize);
        }

        // finally we can decode
        fec.decode(receiverBuffer, receiverIndex);

        byte endResult[] = new byte[k * packetsize];
        System.arraycopy(received, 0, endResult, 0, endResult.length);

        // check for equality
        if (Arrays.equals(source, endResult)) {
            System.out.println("source and Received Files are equal!");
        } else {
            System.out.println("source and Received Files are different!");
        }
    }// end main

}
