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

package fi.hip.sicx.store;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;

import org.joni.test.meta.*;
import org.joni.test.meta.client.*;

import fi.hip.sicx.srp.hessian.TMHostnameVerifier;
import fi.hip.sicx.vaadin.LocalProperties;

import org.glite.security.trustmanager.ContextWrapper;

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.TMHessianURLConnectionFactory;
import com.eaio.uuid.UUID;

import org.glite.security.util.DNHandler;

/**
 * MetaHandler
 *
 * Class for initializing and managing the metadata service connection
 * and communication
 *
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Joakim Koskela <jookos@gmail.com>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 */
public class MetaHandler {

    public static final String ENDPOINT_OPT = "metaService";

    // yes, more singletons:
    private static MetaHandler instance = new MetaHandler();
    public static MetaHandler getInstance() {
        return instance;
    }

    // instance-specific things:

    private MetaDataAPI service = null;
    private String localUserName;
    
    public MetaHandler() { }

    public void init() 
        throws IOException, GeneralSecurityException {
        
        LocalProperties props = LocalProperties.getInstance();
        Enumeration<Object> en = props.elements();
        while(en.hasMoreElements()){
            System.out.println(en.nextElement());
        }
        props.list(System.out);


        // from joni's MetaClient;

        ContextWrapper wrapper = new ContextWrapper(props, true);
        TMHostnameVerifier verifier = new TMHostnameVerifier();         
        
        String url = props.getProperty(ENDPOINT_OPT, "https://localhost:40669/MetaService");
        HessianProxyFactory factory = new HessianProxyFactory();
        TMHessianURLConnectionFactory connectionFactory = new TMHessianURLConnectionFactory();
        connectionFactory.setWrapper(wrapper);
        connectionFactory.setVerifier(verifier);
        connectionFactory.setHessianProxyFactory(factory);
        factory.setConnectionFactory(connectionFactory);
        service = (MetaDataAPI) factory.create(MetaDataAPI.class, url);
        
        System.out.println("Metadata handler initialized, Server version: " + service.getVersion());

        // fetch my own name from the cert
        X509KeyManager keyman = wrapper.getKeyManager();
        String aliases[] = keyman.getClientAliases("RSA", null);
        if(aliases == null || aliases.length == 0)
            aliases = keyman.getServerAliases("RSA", null);
        if(aliases != null && aliases.length > 0) {
            X509Certificate chain[] = keyman.getCertificateChain(aliases[0]);
            localUserName = DNHandler.getSubject(chain[0]).getRFCDNv2();
            System.out.println("Username from certificate: '" + localUserName + "'.");
            ensureLocalUserRoot();
        }
    }

    public MetaDataAPI getService() {
        
        // btw.. how do we check if the service is still connected.. ?

        return service;
    }

    /** 
     * Ensures that the local user exists in the meta service, and has
     * a valid root entry
     */
    public UUID ensureLocalUserRoot() 
        throws IOException {
        
        List<UUID> roots = null;
        UserInfo user = service.getUserInfo();
        if (user == null) {
            throw new IOException("User does not exist in sicx.");
        } else {
            roots = user.getRoots();
        }
        
        if (roots.size() == 0) {
            return null;
        }
        return roots.get(0);
    }

    
    /**
     * (from Jonis code) Initializes a user to the meta data
     * service. Adds a root item as well.
     */
    public void addUser(String userName, String rootName)
        throws IOException {
        
            UserInfo info = new UserInfo();
            info.setName(userName);
            
            MetaFile root = null;
            if (rootName != null){
                root = new MetaFileImpl();
                root.setDirectory(true);
                root.setName(rootName);
                root.addACLItem(new ACLItem(userName, true, true));
                List<UUID> roots = new ArrayList<UUID>();
                roots.add(root.getId());
                info.setRoots(roots);
            }
        
            service.addUser(info);
            if(root != null){
                service.putFile(root);
            }
    }
}
