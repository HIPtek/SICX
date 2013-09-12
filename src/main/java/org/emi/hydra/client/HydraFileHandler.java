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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStore.SecretKeyEntry;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;

import javax.crypto.SecretKey;

/**
 * A helper class to handle file-operations in our tool
 * 
 * Most action is performed with SecretKeys, since stored keys do not
 * necessarily need the extra-metadata information of HydraKey. And
 * HydraKeys by definition should be stored at cloud. These functions
 * are mainly to org.glite.data.hydra.javacli.tests our implementation
 * and to ensure compatibility with JCE.
 * 
 * Note, that most file operations here only handle local encryption
 * and with HydraStorage, these are obsolete. With Hydra, there is no
 * need to store keys in local files.
 * 
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Eetu Korhonen
 */
public class HydraFileHandler {

    // Does not require HydraKey. HydraKey is mainly methods of splitting the file and storing the metadata
    // TODO: might try to support unprotected stores if password == null
    /**
     * Saves a key to a file in the filesystem
     * 
     * @param secretkey the key (SecretKey, HydraKey) to be saved
     * @param file The desired file-name to store key (in JCE KeyStore)
     * @param password A password
     * @param entryname The name of given key in KeyStore. Defaults to "defaultKey"
     * @throws KeyStoreException
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     */
    public static void saveSingleKeyToFile(SecretKey secretkey, File file, char[] password, String entryname)
            throws KeyStoreException, NoSuchAlgorithmException, CertificateException, IOException {

        if (entryname == null)
            entryname = "defaultKey";

        KeyStore keystore = KeyStore.getInstance("JCEKS"); // JCEKS (Java Cryptography Extension Key Store) needed to
                                                           // store symmetric keys
        keystore.load(null, password); // initializes an empty keystore

        KeyStore.SecretKeyEntry skEntry = new KeyStore.SecretKeyEntry(secretkey);

        // if a single key is stored, save it with "defaultKey" name
        keystore.setEntry(entryname, skEntry, new KeyStore.PasswordProtection(password));

        // write key to file
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            keystore.store(fos, password);
        } finally {
            if (fos != null) {
                fos.close();
            }
        }

    }

    /**
     * Helper to load a KeyStore-object from File
     * 
     * @param file The file-path of KeyStore-file
     * @param password Password to open it
     * @return A KeyStore-object in JCEKS format
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws IOException
     * @throws KeyStoreException
     */
    public static KeyStore loadKeyStoreFromFile(File file, char[] password) throws NoSuchAlgorithmException,
            CertificateException, IOException, KeyStoreException {
        InputStream fileInputStream = new FileInputStream(file);
        KeyStore keystore = KeyStore.getInstance("JCEKS");

        keystore.load(fileInputStream, password); // loads the keystore from file
        return keystore;
    }

    /**
     * Loads a single key as a SecretKey.
     * 
     * @param file The path to the file to be loaded
     * @param password The password locking the JCEKS file
     * @param entryname The name of entry. if null, defaults to "defaultKey"
     * @return a HydraKey-object contained within file with the name
     * @throws NoSuchAlgorithmException
     * @throws CertificateException
     * @throws KeyStoreException
     * @throws IOException
     * @throws UnrecoverableEntryException
     */
    public static SecretKey loadSingleKeyFromFile(File file, char[] password, String entryname)
            throws NoSuchAlgorithmException, CertificateException, KeyStoreException, IOException,
            UnrecoverableEntryException {
        if (entryname == null)
            entryname = "defaultKey"; // default to defaultKey if entryname = null

        KeyStore keystore = loadKeyStoreFromFile(file, password);
        SecretKeyEntry keyEntry = (SecretKeyEntry) keystore.getEntry(entryname, new KeyStore.PasswordProtection(
                password)); // assumes we are storing a SecretKey object

        return keyEntry.getSecretKey();
    }

}
