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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import com.onionnetworks.fec.FECCode;
import com.onionnetworks.fec.FECCodeFactory;
import com.onionnetworks.util.Buffer;

/**
 * Simple striping layer on top of the FEC library.
 * 
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class StreamingFEC {
    /**
     * Stripes the data from input into the output.
     * 
     * @param inStream
     *            The stream where to read the input data.
     * @param outStreams
     *            The streams where to write the stripes. The number of streams
     *            has to match n.
     * @param packetSize
     *            The packet size used for the stripes for calculating the FEC.
     * @param k
     *            The number of required stripes to reconstruct the file.
     * @param n
     *            The number of stripes.
     * @param endSize the file length to pad the file to.           
     * @return The number of padding bytes used.
     * @throws IOException
     *             thrown in case reading of the input stream or writing to the
     *             output streams fails.
     */
    public static long stripe(InputStream inStream, OutputStream outStreams[], int packetSize, int k, int n, long endSize)
            throws IOException {

        long fileSize = 0;
        long paddedLen = 0;

        byte source[] = new byte[k * packetSize]; // this is our source file
        // this will hold the encoded file
        byte[] stripes = new byte[n * packetSize];

        // These buffers allow us to put our data in them they
        // reference a packet length of the file (or at least will once
        // we fill them)
        Buffer[] sourceBuffers = new Buffer[k];
        Buffer[] stripeBuffers = new Buffer[n];

        for (int i = 0; i < sourceBuffers.length; i++)
            sourceBuffers[i] = new Buffer(source, i * packetSize, packetSize);

        for (int i = 0; i < stripeBuffers.length; i++)
            stripeBuffers[i] = new Buffer(stripes, i * packetSize, packetSize);

        // When sending the data you must identify what it's index was.
        // Will be shown and explained later
        int[] stripeIndex = new int[n];

        for (int i = 0; i < stripeIndex.length; i++)
            stripeIndex[i] = i;

        // create our fec code
        FECCode fec = FECCodeFactory.getDefault().createFECCode(k, n);

        int read;
        do {
            read = 0;
            // fill the buffer until full or file ends
            int num = 0;
            do {
                num = inStream.read(source, read, source.length - read);
                if (num > 0) {
                    read += num;
                }
            } while (num > 0 && read < source.length);

            if(read == -1){
                read = 0;
            }
            if (read < k * packetSize) {
                Arrays.fill(source, read, k * packetSize, (byte) 0);
                if(fileSize + k*packetSize >= endSize){
                    paddedLen = endSize;
                } else {
                    paddedLen = fileSize + k*packetSize;
                }
            }
            // encode the data
            fec.encode(sourceBuffers, stripeBuffers, stripeIndex);
            // encoded data is now contained in the stripeBuffer/stripes byte
            // array
            for (int i = 0; i < n; i++) {
                outStreams[stripeIndex[i]].write(stripes, i * packetSize, packetSize);
            }

            fileSize += read;
        } while (paddedLen < endSize);

        return paddedLen - fileSize;
    }

    /**
     * Reads the file stripes from the input streams and writes the
     * reconstructed file into the output stream.
     * 
     * @param inStreams
     *            The streams where to read the input streams. Put null for missing stripes.
     * @param outStream
     *            the stream where to write the reconstructed data.
     * @param packetSize
     *            The packet size used for the stripes of the data.
     * @param k
     *            the number of required stripes to reconstruct the data.
     * @param n
     *            the number of stripes.
     * @param fileSize
     *            the size of the original file.
     * @return the number of reconstructed bytes.
     * @throws IOException
     *             thrown in case reading of the input streams or writing to the
     *             output stream fails.
     */
    public static long construct(InputStream inStreams[], OutputStream outStream, int packetSize, int k, int n,
            long fileSize) throws IOException {

        int realStreams = 0;
        
        // count the real streams that are present
        for(int i = 0; i < n; i++){
            if(inStreams[i] != null){
                realStreams++;
            }
        }
        if(realStreams < k){
            throw new IOException("Can't reconstruct data from " + " stripes, minimum needed is " + k + ".");
        }
        
        int stripeIndexes[] = new int[realStreams];
        
        int existingStream = 0;
        for (int i = 0; i < n; i++) {
            if(inStreams[i] != null){
                stripeIndexes[existingStream++] = i;                
            }
        }

        long fileLeft = fileSize;

        byte source[] = new byte[n * packetSize]; // this is our source streams

        // These buffers allow us to handle our data in the source buffer.
        Buffer[] stripeBuffers = new Buffer[realStreams];

        for (int i = 0; i < realStreams; i++){
            stripeBuffers[i] = new Buffer(source, i * packetSize, packetSize);
        }

        // create our fec code
        FECCode fec = FECCodeFactory.getDefault().createFECCode(k, n);

        do {
            int read;

            for (int i = 0; i < realStreams; i++) {
                read = 0;
                // fill the buffer until full or file ends
                int num = 0;
                do {
                    num = inStreams[stripeIndexes[i]].read(source, i * packetSize + read, packetSize - read);
                    if (num > 0) {
                        read += num;
                    }
                } while (num > 0 && read < packetSize);
                if (read != packetSize) {
                    throw new IOException("Unexpected end of stripe. Succesfully read " + (fileSize - fileLeft)
                            + " bytes.");
                }
            }
            
            // copy the stripe indexes as the decode overwrites it
            int tempIndexes[] = new int[realStreams];
            System.arraycopy(stripeIndexes, 0, tempIndexes, 0, realStreams);
            // decode the data
            fec.decode(stripeBuffers, tempIndexes);
            // encoded data is now contained in the sourceBuffer/source byte
            // array
            outStream.write(source, 0, (fileLeft < k * packetSize) ? (int) fileLeft : k * packetSize);

            fileLeft -= k * packetSize;
        } while (fileLeft > 0);
        
        outStream.flush();

        return fileSize;
    }

}
