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

import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import com.tiemens.secretshare.engine.SecretShare;

/**
 * The class to extend SecretKeySpec to contain and handle the
 * subclass of HydraKeys. We mostly require to add the information on
 * the amount of keys required for reconstruction and to add the
 * helper methods to disassemble and reassemble individual keys.
 * 
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Eetu Korhonen
 */
public class HydraKey extends SecretKeySpec {
    
    public static final String ALGORITHM_DEFAULT = "AES";
    
    private static final long serialVersionUID = 5630197248137932896L;

    private byte[] initialization_vector;

    // private SecretShare splitted_key; // contains an instance of com.tiemens.secretshare
    // might make n & k obsolete

    /*
     * The BigInt to be used as a modulus for algorithm SSS-java offers defaults as 194-bit primes new
     * BigInteger("14976407493557531125525728362448106789840013430353915016137");
     * 
     * or with 384-bits new BigInteger("830856716641269388050926147210" + "378437007763661599988974204336" +
     * "741171904442622602400099072063" + "84693584652377753448639527");
     * 
     * 
     * we could default to 65521 which is the largest prime less than 2^16 as in the org.glite.security.ssss
     * C-implementation
     * 
     * TODO: not sure if relevant to be specified (as a generator for the random-function). TODO: so far, we have null
     * to use the default-behaviour of java-SSS
     */

    /**
     * Constructor for HydraKey. Nearly identical to the SecretKeySpec, but takes another parameter "prime", if we wish
     * to use any other value for SSS than the default 16-bit prime used by the C-implementation
     * org.glite.security.ssss.
     * 
     * prime, n and k are stored within the object to offer a convenient place to store that metadata to easen future
     * application-development
     * 
     * NOTE: Initialization does not by default accept a value for Initialization vector (set to null) You can generate
     * it separately and add it with setInitializationVector(String) -method
     * 
     * @param key The encryption key in byte-array
     * @param algorithm The algorithm to be used for key-generation See {@link http
     *            ://download.oracle.com/javase/1.5.0/docs/guide/security/jce/JCERefGuide.html} JCE Reference for
     * @param prime The prime-number to be used for splitting. If set as @null,
     * @param k the threshold of key (null => 2)
     * @param n the amount of shares in key (null => 3) defaults to 16-bit value of org.glite.security.ssss.
     * 
     * 
     */
    public HydraKey(byte[] key, String algorithm) {

        super(key, algorithm); // superclass definitions on these

        this.initialization_vector = null;
    }

    /**
     * Maps a List<ShareInfo> from com.tiemens.secretshare to a common Map<Integer, BigInteger> from java.util.map
     * 
     * @param shareArray the array to be mapped
     * @return the mapping
     */
    public Map<Integer, BigInteger> shareInfoToMap(List<SecretShare.ShareInfo> shares) {

        Map<Integer, BigInteger> keys = new HashMap<Integer, BigInteger>();

        for (int i = 0; i < shares.size(); i++) {
            keys.put(shares.get(i).getIndex(), shares.get(i).getShare()); // maps the splits to a <Integer, BigInteger>
                                                                          // mapping
            // TODO: The mapping probably does not need a loop
        }
        return keys;
    }

    /**
     * Splits the key to a Map<index_of_keypart, keypart> object
     * 
     * @param n the amount of total slices
     * @param k the threshold to be needed for reconstruction
     * @return The mapping in a standard java.util.map object
     */
    public Map<Integer, BigInteger> splitKey(int n, int k) {
        // Insert the parameters to an instance of PublicInfo
        // as in:
        // public PublicInfo(final int inN,
        // final int inK,
        // final BigInteger inPrimeModulus,
        // final String inDescription)

        // uses prime as null -> defaults to the ~300-bit prime
        // TODO: implement the modulo-creation function compatible with the c-implementation
        // TODO: Maybe should return a com.tiemens.secretshare -compliant ShareInfo-array
        SecretShare.PublicInfo publicinfo = new SecretShare.PublicInfo(n, k, null, null);

        // new instance of secretshare

        SecretShare share = new SecretShare(publicinfo);
        // BigInteger secret = new BigInteger(1, this.getEncoded()); // translates byte-array to a positive bigint
        //
        // TODO: This is a root of several bugs. Currently, the SSS-implementation used does not support
        // operations with signed integers, so we have to use the positive interpretation of the number. (signum = 1)
        // But when casting from BigInteger to byte[], the cast usually interpretes the first bit as a signum...
        // troublesome when generating a new bytearray from reconstructed secret (BigInteger)
        SecretShare.SplitSecretOutput splits = share.split(new BigInteger(1, this.getEncoded()));
        Map<Integer, BigInteger> keys = shareInfoToMap(splits.getShareInfos());

        /*
         * Map<Integer,BigInteger> keys = new HashMap<Integer, BigInteger>(); for (int i = 0; i < k; i++) { keys.put(i,
         * splits.getShareInfos().get(i).getShare()); // maps the splits to a <Integer, BigInteger> mapping // TODO: The
         * mapping probably does not need a loop // TODO: maybe should do a Map<Int
         * 
         * }
         */
        return keys;
    }

    /**
     * Returns the secret key of HydraKey in BigInteger-representation
     * 
     * @return the BigInteger representation of Key in store
     * 
     *         TODO: this kind of methods should probably not be present. Or public. Giving extra information whether a
     *         HydraKey matches another HydraKey presents a security threat. Find a way to allow these checks only
     *         within the unit tests (e.g. org.glite.data.hydra.javacli.tests by encryption/decryption results)
     * 
     */
    public BigInteger getKeyBI() {
        return new BigInteger(this.getEncoded());
    }

    /**
     * Set the initialization vector of HydraKey instance if it is one needed to be stored with key-information.
     * 
     * @param iv The string containing iv
     */
    public void setInitializationVector(byte[] iv) {
        if (iv != null) {
            this.initialization_vector = iv;
        }
    }

    /**
     * Get the initialization vector of HydraKey instance if one is stored with key-information.
     * 
     * @return The IV
     */
    public byte[] getInitializationVector() {
        return this.initialization_vector;
    }

    /**
     * This function pads the secret key stored by HydraKey to a given length if it needs to be of certain length.
     * Required for example, if we have randomly generated key beginning with zero-byte. Reconstructing that key with
     * SSS would normally only create a key of length original_key_length - 1 (in bytes). E.g. key 0,1,2,3,4,5
     * (BigEndian)splitted and reconstructed would render 1,2,3,4,5 .
     * 
     * This is useful, since the plugin used (SSS implementation in java) works with BigIntegers, and requires them to
     * be positive. Java does not natively support unsigned-arithmetic, so we avoid a lot of pitfalls and
     * re-implementing the SSS by preprocessing our inputs.
     * 
     * @param length The length of the final (padded) key
     * @throws Exception if the length is smaller than the original byte-array of HydraKey
     * @return HydraKey The new key with proper padding
     * 
     */
    public HydraKey padToLength(int length) throws Exception {
        byte[] originalbytes = this.getEncoded();
        byte[] newbytes = HydraUtils.paddedByteArray(originalbytes, length);
        HydraKey newhydrakey = new HydraKey(newbytes, this.getAlgorithm());
        return newhydrakey;

    }

    /* * * * * * * * * * * * * * * * * * *
     * Static methods * * * * * * * * * * * * * * * * * *
     */


    /**
     * Maps a Map<Integer, BigInteger> to List<ShareInfo> from com.tiemens.secretshare
     * 
     * 
     * 
     * @param Map<Integer, BigInteger> to be Listed
     * @param n amount of shares in the original split
     * @param k amount of shares needed to regenerate secret
     * @param prime (can be null)
     * @return the mapping to List<com.tiemens.secretshare.ShareInfo>
     */
    public static List<SecretShare.ShareInfo> mapToShareInfo(Map<Integer, BigInteger> map, int n, int k,
            BigInteger prime) {
        List<SecretShare.ShareInfo> list = new ArrayList<SecretShare.ShareInfo>();

        for (Map.Entry<Integer, BigInteger> entry : map.entrySet()) {
            SecretShare.PublicInfo publicInfo = new SecretShare.PublicInfo(n, k, prime, null);
            SecretShare.ShareInfo share = new SecretShare.ShareInfo(entry.getKey(), entry.getValue(), publicInfo);
            list.add(share);
        }
        return list;
    }

    public static BigInteger combineKey(Map<Integer, BigInteger> parts, int n, int k, BigInteger prime) {
        SecretShare.PublicInfo publicinfo = new SecretShare.PublicInfo(n, k, prime, null);
        SecretShare share = new SecretShare(publicinfo);

        List<SecretShare.ShareInfo> shares = mapToShareInfo(parts, n, k, prime);
        SecretShare.CombineOutput output = share.combine(shares);

        return output.getSecret();
    }

    /**
     * Generates a new HydraKey with default settings.
     * 
     * Uses blowfish to generate a 128-bit key. Uses 16-bit prime for splitting.
     * 
     * @return Returns the HydraKey-instance
     * @throws NoSuchAlgorithmException
     */
    public static HydraKey generateKey() throws NoSuchAlgorithmException {
        // Sets the keygenerator to use blowfish key
        KeyGenerator keygen = KeyGenerator.getInstance(ALGORITHM_DEFAULT);
        keygen.init(128); // set a keylength of 128 bits
        SecretKey secret_key = keygen.generateKey();
        HydraKey hk = new HydraKey(secret_key.getEncoded(), ALGORITHM_DEFAULT);
        return hk;
    }

    /**
     * Takes arguments to define the key-generation procedure
     * 
     * @param algorithm the algorithm to be used.
     * @param length the length of the wanted key in bits
     * @param prime the (large) prime to be used with hydra key-splitting
     * @return a valid HydraKey generated randomly
     * @throws NoSuchAlgorithmException
     */
    public static HydraKey generateKey(String inAlgorithm, int length)
            throws NoSuchAlgorithmException {

        if (length == 0) { 
            throw new IllegalArgumentException("Key length can't be 0");
        }

        String algorithm = inAlgorithm;
        
        if (algorithm == null) {
            algorithm = ALGORITHM_DEFAULT;
        }

        KeyGenerator keygen = KeyGenerator.getInstance(algorithm);
        keygen.init(length); // set a keylength of 128 bits
        SecretKey secret_key = keygen.generateKey();
        HydraKey hk = new HydraKey(secret_key.getEncoded(), algorithm);
        return hk;
    }

    /**
     * A function to reconstruct a key from slices
     * 
     * @param shares a map of <key_slice_index, keyslice_value>
     * @param prime The prime used to reconstruct the modulo TODO: is this necessary for the algorithm?
     * @param alg the algorithm used (set to Unknown if null)
     * @param k the threshold on needed key-amount
     * @param n the amount of total keys (can be null)
     * @return a new instance of HydraKey constructed from the pieces given
     */
    public static HydraKey reconstructKey(Map<Integer, BigInteger> shares, int n, int k, BigInteger prime,
            String alg) {
        BigInteger constructed_key = combineKey(shares, n, k, prime);
        String algorithm = alg;
        if (algorithm == null) {
            algorithm = "Unknown";
        }

        // chops off an empty signum-byte if found
        byte[] keyBytes = constructed_key.toByteArray();
        if (keyBytes[0] == 0) {
            // System.out.println("chopping stuff");

            byte[] tempArray = Arrays.copyOfRange(keyBytes, 1, keyBytes.length);
            keyBytes = tempArray;
        }

        HydraKey hk = new HydraKey(keyBytes, algorithm);
        return hk;
    }

}
