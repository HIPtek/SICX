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

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.net.MalformedURLException;
import java.rmi.RemoteException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.bouncycastle.crypto.CryptoException;
import org.glite.security.trustmanager.ContextWrapper;
import org.hydra.HydraAPI;
import org.hydra.KeyPiece;
import org.joni.test.meta.ACLItem;

import com.caucho.hessian.client.TMHessianURLConnectionFactory;

import fi.hip.sicx.srp.HandshakeException;
import fi.hip.sicx.srp.SRPAPI;
import fi.hip.sicx.srp.SRPClient;
import fi.hip.sicx.srp.SessionKey;
import fi.hip.sicx.srp.SessionToken;
import fi.hip.sicx.srp.hessian.HessianSRPProxy;
import fi.hip.sicx.srp.hessian.HessianSRPProxyFactory;
import fi.hip.sicx.srp.hessian.TMHostnameVerifier;

/**
 * A class to work as an interface between the Hydra-webservice SOAP-interface and our Java-client.
 * 
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 * @author Eetu Korhonen
 */
public class HydraConnection {

    private String servername;
    private HydraAPI service;
    private SRPAPI srpService;
    private String _address;
    private HessianSRPProxyFactory factory;

    /**
     * Constructor for HydraConnection
     * 
     * @param address e.g. "https://vtb-generic-70.cern.ch:8443/3/glite-data-hydra-service/services/Hydra"
     * @param servername a name you want to give for this connection
     * @param props the properties that get forwarded to trustmanager to set up the ssl connection.
     * @throws ServiceException 
     * @throws GeneralSecurityException 
     * @throws IOException 
     * @throws HandshakeException 
     * @throws CryptoException 
     */
    public HydraConnection(String address, String servername, Properties props) throws IOException, GeneralSecurityException {
        ContextWrapper _wrapper = new ContextWrapper(props, false);

        TMHostnameVerifier verifier = new TMHostnameVerifier();

        factory = new HessianSRPProxyFactory();
        TMHessianURLConnectionFactory connectionFactory = new TMHessianURLConnectionFactory();
        connectionFactory.setWrapper(_wrapper);
        connectionFactory.setVerifier(verifier);
        connectionFactory.setHessianProxyFactory(factory);
        factory.setConnectionFactory(connectionFactory);
        
        if(!address.trim().endsWith("/")){
            _address = address.trim() + "/";
        } else {
            _address = address;
        }
        
        this.servername = servername;
    }
    
    public void login(String username, String password) throws MalformedURLException, CryptoException, HandshakeException{
        srpService = (SRPAPI) factory.create(SRPAPI.class, _address + "SRPService");
        SessionKey hydra1Session = SRPClient.login(srpService, username, password);
        
        System.out.println("asdfasdfa " + _address + "HydraService");
        service = (HydraAPI) factory.create(HydraAPI.class, _address + "HydraService");
        HessianSRPProxy proxy = (HessianSRPProxy) Proxy.getInvocationHandler(service);
        proxy.setSession(new SessionToken(username, hydra1Session.getK()).toString());
        
        
    }

    /**
     * Removes a given entry from a single HydraConnection
     * 
     * @param filename
     * @param userid
     * @param schema
     * @throws NoSuchAlgorithmException
     * @throws IOException 
     */
    private void removeEntry(String filename, String userid) throws IOException, NoSuchAlgorithmException {
        service.removeKeyPiece(getEntryName(filename, userid));
    }


    /*
     * edscipher VARCHAR(50), edskey VARCHAR(200), edsiv VARCHAR(200), edskeyinfo VARCHAR(200), edskeysneeded INT NOT
     * NULL, edskeyindex INT NOT NULL
     */

    /**
     * Adds a HydraKey to a responding entry within this instance of HydraConnection
     * 
     * @param entryName - The Entryname for which to link the key
     * @param edsKey - The Slice of key to be stored
     * @param edsCipher - The Algorithm/cipher intended to use with the key
     * @param edsKeyInfo - Meta-information of the Key. E.g. Which client was used to generate the data
     * @param edsIv - Initialization vector of the individual encryption. If the algorithm requires a random IV. NOTE:
     *            THE IV IS NOT SPLIT WITH SSSS. ALL INSTANCES CONTAIN THE UNENCRYPTED, SAME IV.
     * @param edsKeyIndex - The index of given key
     * @param edsKeysNeeded - How many keys are needed to reconstruct.
     * @throws ServiceException
     * @throws IOException 
     */
    private void addHydraKeyToEntry(String entryName, BigInteger edsKey, String edsCipher, String edsKeyInfo, BigInteger iv,
            Integer edsKeyIndex, Integer edsKeysNeeded, List<ACLItem> acl) throws IOException {

//        System.out.println("name = [" + entryName + "] acl = [" + acl + "] value = [" + edsKey.toString() + "] iv = [" + iv +  "]");
        KeyPiece piece = new KeyPiece();
        piece.iv = iv;
        piece.keyPiece = edsKey;
        piece.minPieces = edsKeysNeeded;
        piece.pieceNumber = edsKeyIndex;
        piece.setACL(acl);
        
        service.putKeyPiece(entryName, piece);
    }

    /**
     * Gets the attributes as an Attribute[] list from given entry
     * 
     * @param entryname
     * @return
     * @throws ServiceException
     * @throws IOException 
     */
    private KeyPiece getKey(String entryname) throws IOException {
        return service.getKeyPiece(entryname);
    }

    /**
     * A method to return the server version of this connection. Used for org.glite.data.hydra.javacli.tests-purposes
     * 
     * @return The version of the hydra-server at endpoint
     * @throws IOException 
     */
    public String getServerVersion() throws IOException {
        return service.getVersion();
    }

    /*
     * STATIC METHODS
     */

    /**
     * Forms an Entry-name in syntax.
     * 
     * @param filename
     * @param userid
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String getEntryName(String filename, String userid) throws NoSuchAlgorithmException {
        String file_entry = filename + "" + userid + "" + HydraUtils.bytesToString(getHash(filename, userid));
        // TODO: Use String replaceAll(String regex, String replacement)
        file_entry = file_entry.replace(",", "#").replace(" ", "_"); // entryname cannot have commas or spaces...
        file_entry = file_entry.replace("%", "#").replace("^", "_");
        file_entry = file_entry.replace("*", "#").replace("(", "_");
        file_entry = file_entry.replace(")", "#").replace("+", "_");
        file_entry = file_entry.replace("!", "#").replace("?", "_");
        file_entry = file_entry.replace("\\", "#").replace("[", "_");
        file_entry = file_entry.replace("]", "#").replace("~", "_");
        return file_entry; 
    }

    /**
     * Splits and Distributes a key to a set of Hydra-servers.
     * 
     * @param connections - An array of established connections.
     * @param reconstruction_threshold - How many Slices do we need to reconstruct a key
     * @param key - The HydraKey to be stored
     * @param filename - The filename of the file for which we link the encryption key
     * @param userid - A string representing a unique Hydra-Server user. For example, can be derived from the
     *            authentication certificate or VOMS virtual organization
     * @throws ServiceException
     * @throws NoSuchAlgorithmException
     * @throws IOException 
     * @returns the number of pieces stored
     */
    public static int distributeKey(HydraSettings connections, Integer reconstruction_threshold, HydraKey key,
            String filename, String userid) throws NoSuchAlgorithmException, IOException {

        int number_of_connections = connections.getEndpoints().length;

        // split the key
        Map<Integer, BigInteger> keySlices = key.splitKey(number_of_connections, reconstruction_threshold);
        String entryname = getEntryName(filename, userid);
        ACLItem aclItem = new ACLItem(userid, true, true);
        List<ACLItem> acl = new ArrayList<ACLItem>();
        acl.add(aclItem);
        BigInteger iv = null;
        if (key.getInitializationVector() != null) {
            // convert to BigInteger-lookalike string to avoid storing weird characters in xml-file
            iv = (new BigInteger(key.getInitializationVector()));
        }
        int storedPieces = 0;
        for (int i = 0; i < number_of_connections; i++) {
            java.util.Date startTime = new java.util.Date();
            // System.out.println("name = [" + entryname + "] userid = [" + userid + "] value = [" + keySlices.get(i +
            // 1).toString() + "] iv = [" + iv + "]" );
            boolean exception = false;
            try {
                connections.getEndpoints()[i].addHydraKeyToEntry(entryname, keySlices.get(i + 1), // +1 because indexing
                                                                                                  // of keys
                        // begins from 1. For some
                        // silly reason.
                        key.getAlgorithm(), "JHydraCliKey", iv, i + 1, // for same reason, + 1
                        reconstruction_threshold, acl);
            } catch (Exception e) {
                System.out.println("Failed to store piece " + i + ".");
                exception = true;
                e.printStackTrace();
            }
            if (!exception) {
                storedPieces++;
            } else {
                java.util.Date endTime = new java.util.Date();
                System.out.println("Piece " + i + " storage time :" + (endTime.getTime() - startTime.getTime()));
            }
        }
        
        if(storedPieces < reconstruction_threshold){
            System.out.println("Failed to store enough pieces (" + reconstruction_threshold + ") to allow reconstruction later. (Stored " + storedPieces + " pieces)");
            forceRemoveEntries(connections, filename, userid);
            throw new IOException("Failed to store enough pieces (" + reconstruction_threshold + ") to allow reconstruction later. (Stored " + storedPieces + " pieces)");
        }
        if(storedPieces < connections.getEndpoints().length){
            System.out.println("Storage of some key pieces failed, stored " + storedPieces + " pieces succesfully, and " + (connections.getEndpoints().length - storedPieces) + " failed.");
        }
        return storedPieces;
    }


    public static void removeEntries(HydraSettings connections, String filename, String userid)
            throws NoSuchAlgorithmException, IOException {
        for (int i = 0; i < connections.getEndpoints().length; i++) {
            java.util.Date startTime = new java.util.Date();
            connections.getEndpoints()[i].removeEntry(filename, userid);
            java.util.Date endTime = new java.util.Date();
            System.out.println("Piece " + i + " remove time :" + (endTime.getTime() - startTime.getTime()));
        }
    }

    public static void forceRemoveEntries(HydraSettings connections, String filename, String userid) {
        for (int i = 0; i < connections.getEndpoints().length; i++) {
            try {
                connections.getEndpoints()[i].removeEntry(filename, userid);
            } catch (NoSuchAlgorithmException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (RemoteException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Builds a HydraKey from given connection-endpoints and the metadata regarding filename and user TODO: Tests to
     * make sure that the key reconstructs or does not after service-failures of individual Hydras
     * 
     * 
     * @param connections
     * @param filename
     * @param userid
     * @return HydraKey reconstructed
     * @throws NoSuchAlgorithmException
     * @throws ServiceException
     */
    public static HydraKey gatherKey(HydraSettings connections, String filename, String userid)
            throws NoSuchAlgorithmException, IOException {

        // keep track on exception count
        // Throw certain kind of exception if SpecializedExceptionsAmoun == TotalAmountOfExceptions
        int exceptions = 0;

        int slices_gathered = 0;

        int n = 0, k = 0;
        String algorithm = null;

        String entryname = getEntryName(filename, userid);
        BigInteger iv = null;
        Map<Integer, BigInteger> gatheredKeySlices = new HashMap<Integer, BigInteger>();
        HydraKey hk = null;

        for (int i = 0; i < connections.getEndpoints().length; i++) {
            try {
                KeyPiece piece = connections.getEndpoints()[i].getKey(entryname);

                gatheredKeySlices.put(piece.pieceNumber, piece.keyPiece);

                if (k <= 0){
                    k = piece.minPieces;                   
                }
                
                if (n <= 0){
                    n = connections.getEndpoints().length;
                }
                
                if (algorithm == null){
                    algorithm = null;
                }
                
                // Get IV from store
                if (piece.iv != null) {
                    iv = piece.iv;
                }
                slices_gathered++;

            } catch (IOException e) {
               exceptions++;
               e.printStackTrace(System.out);
            } 
        }

        // Check that if all exceptions are of same type, throw that exception.
        if (exceptions != 0) {
            throw new IOException("All endpoints returned InternalException");
        } 

        // check that you have enough data for reconstruction
        if (k > 0 && slices_gathered - exceptions >= k) {
            hk = HydraKey.reconstructKey(gatheredKeySlices, n, k, null, algorithm);
            if (iv != null) {
                // Parse byte-array from BigInteger-represented String
                byte[] ivInBytes = iv.toByteArray();
                hk.setInitializationVector(ivInBytes);
            }
        } else {
            System.out.println("Too few slices available");
            // TODO Write your own exceptions
            // throw Exception("Too few")
            throw new IOException("Key piece gathering error. Check your connection settings.");
        }
        return hk;
    }

    /**
     * A Helper method to get a hash from combination of filename and userid. Used to separate several similarly named
     * files from a single author.
     * 
     * @param filename
     * @param userid
     * @return
     * @throws NoSuchAlgorithmException
     */
    private static byte[] getHash(String filename, String userid) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA");
        byte[] hash = md.digest((filename + userid).getBytes());
        return hash;
    }
    
    /**
     * Returns the end point address of the hydra service.
     * 
     * @return The URL of the hydra service.
     */
    public String getEndpoint(){
        return _address;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString(){
        return "Hydra connection to: " + servername + " at " + _address;
    }

}
