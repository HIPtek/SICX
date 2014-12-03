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
import java.lang.reflect.Proxy;
import java.security.GeneralSecurityException;
import java.util.*;
import java.security.cert.X509Certificate;

import javax.net.ssl.X509KeyManager;

import org.joni.test.meta.*;
import org.joni.test.meta.client.*;

import fi.hip.sicx.srp.HandshakeException;
import fi.hip.sicx.srp.SRPAPI;
import fi.hip.sicx.srp.SRPClient;
import fi.hip.sicx.srp.SessionKey;
import fi.hip.sicx.srp.SessionToken;
import fi.hip.sicx.srp.hessian.HessianSRPProxy;
import fi.hip.sicx.srp.hessian.HessianSRPProxyFactory;
import fi.hip.sicx.srp.hessian.TMHostnameVerifier;
import fi.hip.sicx.vaadin.LocalProperties;

import org.bouncycastle.crypto.CryptoException;
import org.glite.security.trustmanager.ContextWrapper;

import com.caucho.hessian.client.HessianProxyFactory;
import com.caucho.hessian.client.TMHessianURLConnectionFactory;
import com.eaio.uuid.UUID;

import org.glite.security.util.DNHandler;
import org.hydra.HydraAPI;

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

    public void init(Properties props, String username, String password) 
        throws IOException, GeneralSecurityException, CryptoException, HandshakeException {
        
//        props.list(System.out);
        String metaUrl = props.getProperty(ENDPOINT_OPT, "https://localhost:40669/");

        HessianSRPProxyFactory factory = HessianSRPProxyFactory.getFactory(props);
        SRPAPI hydra1SrpService = (SRPAPI) factory.create(SRPAPI.class, metaUrl + "SRPService");
        SessionKey hydra1Session = SRPClient.login(hydra1SrpService, username, password);
        
        service = (MetaDataAPI) factory.create(MetaDataAPI.class, metaUrl + "MetaService");
        HessianSRPProxy proxy = (HessianSRPProxy) Proxy.getInvocationHandler(service);
        proxy.setSession(new SessionToken(username, hydra1Session.getK()).toString());

        System.out.println("Metadata handler initialized, Server version: " + service.getVersion());

//        ensureLocalUserRoot();
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
