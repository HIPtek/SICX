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
package fi.hip.sicx.slcs;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.glite.slcs.SLCSBaseClient;
import org.glite.slcs.SLCSException;
import org.glite.slcs.SLCSInit;
import org.glite.slcs.config.SLCSClientConfiguration;
import org.glite.slcs.shibclient.ShibbolethCredentials;

/**
 * SLCSClient class.
 *
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Henri Mikkonen <henri.mikkonen@nimbleidm.com>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 */
public class SLCSClient extends SLCSBaseClient {
	
	public static boolean slcsLogin(String username, String password, String privateKeyPassword) {
		return slcsLogin(username, password, privateKeyPassword, "/tmp", "");
	}
	
	public static boolean slcsLogin(String username, String password, String privateKeyPassword, String storeDirectory, String userPrefix) {
		String config = "slcs-init.xml";
		String idpProviderId = "sicx";
		int keySize = 2048;
		
		// create client
        SLCSInit client = null;
        try {
            SLCSClientConfiguration configuration = SLCSClientConfiguration.getInstance(config);
            //System.out.println("CONF: " + configuration.getString("url"));
            ShibbolethCredentials credentials = new ShibbolethCredentials(
                    username, password, idpProviderId);
            client = new SLCSInit(configuration, credentials);
            client.setStoreDirectory(storeDirectory);
            client.setUserPrefix(userPrefix);
        } catch (SLCSException e) {
            System.err.println("ERROR: Failed to create SLCS client: " + e);
            return false;
        }

        // client shibboleth login
        try {
            client.shibbolethLogin();
        } catch (SLCSException e) {
            System.err.println("ERROR (shibboleth): " + e);
            return false;
        }

        // SLCS login request, get DN and authToken
        try {
            client.slcsLogin();
        } catch (SLCSException e) {
            System.err.println("ERROR (SLCS): " + e);
            return false;
        }

        // generate key and CSR
        try {
            if (keySize != -1) {
                client.setKeySize(keySize);
            }
            keySize = client.getKeySize();
            client.generateCertificateKeys(keySize, privateKeyPassword.toCharArray());
            client.generateCertificateRequest();
        } catch (GeneralSecurityException e) {
            System.err.println("ERROR (key): " + e);
            return false;
        }
        System.out.println("Certificate generated: " + client.getKeySize() + "(keySize), dir:" + client.getStoreDirectory());
        System.out.println("file:" + client.getUserCertFilename());
        System.out.println("key:" + client.getUserKeyFilename());
        System.out.println("keypk:" + client.getUserPKCS12Filename());
        
        // submit CSR
        try {
            client.slcsCertificateRequest();
        } catch (SLCSException e) {
            System.err.println("ERROR (CSR): " + e);
            return false;
        }

        // store key + cert
        try {
            client.storePrivateKey();
            client.storeCertificate();

        } catch (IOException e) {
            System.err.println("ERROR (cert): " + e);
            return false;
        }
        return true;
	}

}
