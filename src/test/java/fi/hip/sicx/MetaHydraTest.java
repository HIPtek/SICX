package fi.hip.sicx;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.Properties;

import org.bouncycastle.crypto.CryptoException;
import org.emi.hydra.client.HydraConnection;
import org.hydra.HydraAPI;
import org.hydra.KeyPiece;
import org.hydra.server.HydraServer;
import org.joni.test.meta.ACLItem;
import org.joni.test.meta.MetaDataAPI;
import org.joni.test.meta.server.MetaServer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import fi.hip.sicx.srp.HandshakeException;
import fi.hip.sicx.srp.SRPAPI;
import fi.hip.sicx.srp.SRPClient;
import fi.hip.sicx.srp.SessionKey;
import fi.hip.sicx.srp.SessionToken;
import fi.hip.sicx.srp.hessian.HessianSRPProxy;
import fi.hip.sicx.srp.hessian.HessianSRPProxyFactory;
import fi.hip.sicx.store.MetaHandler;

public class MetaHydraTest {

    private static final String TRUSTED_CLIENT_CONFIG_FILE = "src/test/client.conf";
    private static final String HYDRA1_PURGE_CONFIG_FILE = "src/test/hydra1-purge.conf";
    private static final String HYDRA2_PURGE_CONFIG_FILE = "src/test/hydra2-purge.conf";
    private static final String HYDRA3_PURGE_CONFIG_FILE = "src/test/hydra3-purge.conf";
    private static final String META_PURGE_CONFIG_FILE = "src/test/meta-purge.conf";
    private HydraServer hydra1;
    private HydraServer hydra2;
    private HydraServer hydra3;
    private MetaServer metaServer;
    private static final String hydra1Url = "https://localhost:8991/";
    private static final String hydra2Url = "https://localhost:8992/";
    private static final String hydra3Url = "https://localhost:8993/";
    private static final String metaUrl = "https://localhost:40666/";
    
    private static final String username = "TEstUser";
    private static final String password = "PassWordd";

    @Before
    public void setupServers() throws Exception {
        hydra1 = new HydraServer();
        hydra1.configure(HYDRA1_PURGE_CONFIG_FILE);
        hydra1.start();
        hydra2 = new HydraServer();
        hydra2.configure(HYDRA2_PURGE_CONFIG_FILE);
        hydra2.start();
        hydra3 = new HydraServer();
        hydra3.configure(HYDRA3_PURGE_CONFIG_FILE);
        hydra3.start();
        metaServer = new MetaServer();
        metaServer.configure(META_PURGE_CONFIG_FILE);
        metaServer.start();

    }

    public void addUserSRP(String username, String passwordString) throws FileNotFoundException, IOException,
            GeneralSecurityException {
        // client
        HessianSRPProxyFactory factory = HessianSRPProxyFactory.getFactory(TRUSTED_CLIENT_CONFIG_FILE);
        SRPAPI hydra1SrpService = (SRPAPI) factory.create(SRPAPI.class, hydra1Url + "SRPService");
        SRPClient.putVerifier(hydra1SrpService, username, passwordString);
        SRPAPI hydra2SrpService = (SRPAPI) factory.create(SRPAPI.class, hydra2Url + "SRPService");
        SRPClient.putVerifier(hydra2SrpService, username, passwordString);
        SRPAPI hydra3SrpService = (SRPAPI) factory.create(SRPAPI.class, hydra3Url + "SRPService");
        SRPClient.putVerifier(hydra3SrpService, username, passwordString);
        SRPAPI metaSrpService = (SRPAPI) factory.create(SRPAPI.class, metaUrl + "SRPService");
        SRPClient.putVerifier(metaSrpService, username, passwordString);

    }

    @Test
    public void testFilePut() throws Exception {
        addUserSRP(username, password);
        
        HessianSRPProxyFactory factory = HessianSRPProxyFactory.getFactory(TRUSTED_CLIENT_CONFIG_FILE);
        SRPAPI hydra1SrpService = (SRPAPI) factory.create(SRPAPI.class, hydra1Url + "SRPService");
        SessionKey hydra1Session = SRPClient.login(hydra1SrpService, username, password);
        
        HydraAPI service = (HydraAPI) factory.create(HydraAPI.class, hydra1Url + "HydraService");
        HessianSRPProxy proxy = (HessianSRPProxy) Proxy.getInvocationHandler(service);
        proxy.setSession(new SessionToken(username, hydra1Session.getK()).toString());
        
        KeyPiece piece = new KeyPiece();
        piece.keyPiece = BigInteger.valueOf(666);
        piece.addACLItem(new ACLItem(username, true, true));
        service.putKeyPiece("aa", piece);
        
        KeyPiece out = service.getKeyPiece("aa");
        assertEquals(out.keyPiece, piece.keyPiece);
        
    }

    @Test
    public void testHydraConnection() throws IOException, GeneralSecurityException, CryptoException, HandshakeException{
        HessianSRPProxyFactory factory = HessianSRPProxyFactory.getFactory(TRUSTED_CLIENT_CONFIG_FILE);
        SRPAPI hydra1SrpService = (SRPAPI) factory.create(SRPAPI.class, hydra1Url + "SRPService");
        SRPClient.putVerifier(hydra1SrpService, username, password);
        
        Properties props = new Properties();
        props.load(new FileReader(TRUSTED_CLIENT_CONFIG_FILE));
        HydraConnection hydraConnection = new HydraConnection(hydra1Url, "test", props);
        hydraConnection.login(username, password);
        hydraConnection.getServerVersion();
    }
    
    @Test
    public void testMetaHandler() throws FileNotFoundException, IOException, GeneralSecurityException, CryptoException, HandshakeException{
        addUserSRP(username, password);
        MetaHandler metaHandler = MetaHandler.getInstance();
        Properties props = new Properties();
        props.load(new FileReader(TRUSTED_CLIENT_CONFIG_FILE));
        metaHandler.init(props, username, password);
        MetaDataAPI service = metaHandler.getService();
        service.getVersion();
        
        
        
    }
    
    @After
    public void stopServers() throws Exception {
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
