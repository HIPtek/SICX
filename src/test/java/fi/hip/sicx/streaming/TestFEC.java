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

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

import com.onionnetworks.fec.FECCode;
import com.onionnetworks.fec.FECCodeFactory;
import com.onionnetworks.util.Buffer;

/**
 * Class to test FEC.
 *
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class TestFEC {
    @Test
    public void testBufferFEC() {
        // k = number of source packets to encode
        // n = number of packets to encode to
        int k = 5;
        int n = 7;
        int packetsize = 10240;

        Random rand = new Random();

        byte source[] = new byte[k * packetsize]; // this is our source file

        // this is just so we have something to encode
        rand.nextBytes(source);

        // this will hold the encoded file
        byte[] repair = new byte[n * packetsize];

        // These buffers allow us to put our data in them they
        // reference a packet length of the file (or at least will once
        // we fill them)
        Buffer[] sourceBuffers = new Buffer[k];
        Buffer[] stripeBuffers = new Buffer[n];

        for (int i = 0; i < sourceBuffers.length; i++)
            sourceBuffers[i] = new Buffer(source, i * packetsize, packetsize);

        for (int i = 0; i < stripeBuffers.length; i++)
            stripeBuffers[i] = new Buffer(repair, i * packetsize, packetsize);

        // When sending the data you must identify what it's index was.
        // Will be shown and explained later
        int[] repairIndex = new int[n];

        for (int i = 0; i < repairIndex.length; i++)
            repairIndex[i] = i;

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
        for (int i = 0; i < n - 2; i++)
            receiverBuffer[i] = new Buffer(received, i * packetsize, packetsize);

        // finally we can decode
        fec.decode(receiverBuffer, receiverIndex);

        byte endResult[] = new byte[k * packetsize];
        System.arraycopy(received, 0, endResult, 0, endResult.length);

        // check for equality
        if (Arrays.equals(source, endResult))
            System.out.println("source and Received Files are equal!");
        else
            System.out.println("source and Received Files are different!");
    }// end main
}
