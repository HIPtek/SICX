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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;

/**
 * A class to handle the encryption operations.
 * 
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Eetu Korhonen
 */
public class HydraCrypt {
    /**
     * Encrypts an array of bytes with a hydrakey
     * 
     * @param target the array of bytes to be encrypted
     * @param key The key to be used
     * @param transformation A transformation in JCE format
     * @return The bytes in encrypted format
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     */
    public static byte[] encrypt(byte[] target, HydraKey key, String transformation) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        byte[] ciphertext = cipher.doFinal(target);

        // save IV to the encryption key
        byte[] iv = cipher.getIV();
        key.setInitializationVector(iv);

        return ciphertext;
    }

    /**
     * 
     * Decrypts an array of bytes with a hydrakey
     * 
     * @param target the array of bytes to be decrypted
     * @param key The key to be used
     * @param transformation A transformation in JCE format
     * @return The decrypted bytes
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws IllegalBlockSizeException
     * @throws BadPaddingException
     * @throws InvalidAlgorithmParameterException
     */
    public static byte[] decrypt(byte[] target, HydraKey key, String transformation) throws NoSuchAlgorithmException,
            NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
            InvalidAlgorithmParameterException {
        Cipher decipher = Cipher.getInstance(transformation);

        // Check that if key contains an IV, use it for initialization
        if (key.getInitializationVector() != null) {
            IvParameterSpec ivBytes = new IvParameterSpec(key.getInitializationVector());
            decipher.init(Cipher.DECRYPT_MODE, key, ivBytes);
        } else {
            decipher.init(Cipher.DECRYPT_MODE, key);
        }

        byte[] plaintext = decipher.doFinal(target);
        return plaintext;
    }

    // Encrypt & decrypt files

    /**
     * Encrypts a file at given path with a HydraKey
     * 
     * @param target The file to be encrypted
     * @param key The key to be used
     * @param transformation The transformation used in JCE format
     * @return An InputStream of encrypted file
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     */
    public static InputStream encryptFile(File target, HydraKey key, String transformation)
            throws FileNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
        // ByteArrayOutputStream returnvalue = new ByteArrayOutputStream();
        InputStream fileInput = new FileInputStream(target);

        Cipher cipher = Cipher.getInstance(transformation);
        cipher.init(Cipher.ENCRYPT_MODE, key);
        InputStream cipherInput = new CipherInputStream(fileInput, cipher);

        // save IV to the encryption key
        byte[] iv = cipher.getIV();
        key.setInitializationVector(iv);

        return cipherInput;
    }

    /**
     * Decrypts a file at given path with HydraKey
     * 
     * @param target The file to be decrypted
     * @param key The key to be used
     * @param transformation The transformation used in JCE format
     * @return An InputStream of decrypted file
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
    public static InputStream decryptFile(File target, HydraKey key, String transformation)
            throws FileNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException {
        // ByteArrayOutputStream returnvalue = new ByteArrayOutputStream();
        InputStream fileInput = new FileInputStream(target);
        Cipher decipher = Cipher.getInstance(transformation);

        // Check that if key contains an IV, use it for initialization
        if (key.getInitializationVector() != null) {
            IvParameterSpec ivBytes = new IvParameterSpec(key.getInitializationVector());
            decipher.init(Cipher.DECRYPT_MODE, key, ivBytes);
        } else {
            decipher.init(Cipher.DECRYPT_MODE, key);
        }
        InputStream cipherInput = new CipherInputStream(fileInput, decipher);

        return cipherInput;
    }

    /**
     * Decrypts the data and saves results to the target file.
     * 
     * @param target The file to be written
     * @param key The key to be used
     * @param transformation The transformation used in JCE format
     * @return An OutputStream of decrypting file
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
    public static OutputStream decryptingFile(File target, HydraKey key, String transformation)
            throws FileNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException {
        OutputStream fileOutput = new FileOutputStream(target);
        OutputStream cipherOutput = decryptingStream(fileOutput, key, transformation);

        return cipherOutput;
    }
    
    /**
     * Decrypts the data and saves results to the target file.
     * 
     * @param targetStream The stream where to write the decrypted data
     * @param key The key to be used
     * @param transformation The transformation used in JCE format
     * @return An OutputStream of decrypting file
     * @throws FileNotFoundException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     */
    public static OutputStream decryptingStream(OutputStream targetStream, HydraKey key, String transformation)
            throws FileNotFoundException, NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
            InvalidAlgorithmParameterException {
        Cipher decipher = Cipher.getInstance(transformation);
        
//        System.out.println("hydraKey iv: " + key.getInitializationVector());

        // Check that if key contains an IV, use it for initialization
        if (key.getInitializationVector() != null) {
            IvParameterSpec ivBytes = new IvParameterSpec(key.getInitializationVector());
            decipher.init(Cipher.DECRYPT_MODE, key, ivBytes);
        } else {
            decipher.init(Cipher.DECRYPT_MODE, key);
        }
        OutputStream cipherOutput = new CipherOutputStream(targetStream, decipher);

        return cipherOutput;
    }
}
