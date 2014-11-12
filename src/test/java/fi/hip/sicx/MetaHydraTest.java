package fi.hip.sicx;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;

import org.bouncycastle.crypto.CryptoException;
import org.hydra.server.HydraServer;
import org.joni.test.meta.MetaDataAPI;
import org.joni.test.meta.server.MetaServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fi.hip.sicx.srp.HandshakeException;
import fi.hip.sicx.srp.SRPAPI;
import fi.hip.sicx.srp.SRPClient;
import fi.hip.sicx.srp.hessian.HessianSRPProxyFactory;

public class MetaHydraTest {

    public static final String TRUSTED_CLIENT_CONFIG_FILE = "src/test/client.conf";
    private static final String HYDRA1_PURGE_CONFIG_FILE = "src/test/hydra1-purge.conf";
    private static final String HYDRA2_PURGE_CONFIG_FILE = "src/test/hydra2-purge.conf";
    private static final String HYDRA3_PURGE_CONFIG_FILE = "src/test/hydra3-purge.conf";
    private static final String META_PURGE_CONFIG_FILE = "src/test/meta-purge.conf";
    private HydraServer hydra1;
    private HydraServer hydra2;
    private HydraServer hydra3;
    private MetaServer metaServer;

    @Before
    public void setupServers() throws Exception {
        System.out.println("Starting hydras....");
        hydra1 = new HydraServer();
        hydra1.configure(HYDRA1_PURGE_CONFIG_FILE);
        hydra1.start();
        hydra2 = new HydraServer();
        hydra2.configure(HYDRA2_PURGE_CONFIG_FILE);
        hydra2.start();
        hydra3 = new HydraServer();
        hydra3.configure(HYDRA3_PURGE_CONFIG_FILE);
        hydra3.start();
        System.out.println("Starting meta....");
        metaServer = new MetaServer();
        metaServer.configure(META_PURGE_CONFIG_FILE);
        metaServer.start();

    }
    
    public void addUserSRP(String username, String passwordString, boolean addUser, String url){
    // client
//    HessianSRPProxyFactory factory = HessianSRPProxyFactory.getFactory(TRUSTED_CLIENT_CONFIG_FILE);
//    String srpUrl = "https://localhost:40666/SRPService";
//    SRPAPI srpService = (SRPAPI) factory.create(SRPAPI.class, srpUrl);
//
//    SRPClient.putVerifier(srpService, username, passwordString);

    }
    
    @Test
    public void testFilePut() throws Exception {
        
    }
    
    @After
    public void stopServers() throws Exception {
        System.out.println("****Stop");
        if (hydra1 != null) {
            hydra1.stop();
            hydra1 = null;
        }
        if (hydra2 != null) {
            hydra2.stop();
            hydra2 = null;
        }
        if (hydra3 != null) {
            hydra3.stop();
            hydra3 = null;
        }
        if (metaServer != null) {
            metaServer.stop();
            metaServer = null;
        }
    }

}
