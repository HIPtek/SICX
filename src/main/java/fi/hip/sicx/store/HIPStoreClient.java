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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.net.ssl.HttpsURLConnection;

import org.glite.security.trustmanager.ContextWrapper;
import org.json.JSONArray;
import org.json.JSONObject;

import fi.hip.sicx.vaadin.LocalProperties;

/**
 * HipStoreClient
 *
 * @author Joakim Koskela <jookos@gmail.com>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class HIPStoreClient
    implements StorageClient {

    public HIPStoreClient() 
        throws IOException, GeneralSecurityException {

        this.wrapper = new ContextWrapper(LocalProperties.getInstance(), false);
    }

    /* the context wrapper */
    private ContextWrapper wrapper;

    // whether the connection has failed, need to be reset (forbid reuse)
    private boolean failed = false;

    private HttpURLConnection getConnection(String path) 
        throws Exception {

        LocalProperties props = LocalProperties.getInstance();
        URL url = new URL(props.getProperty("hipstore.url", "https://localhost:7443/remotestore") + path);
        HttpURLConnection uc = (HttpURLConnection)url.openConnection();
        HttpURLConnection.setFollowRedirects(true);
    
        if (uc instanceof HttpsURLConnection) {
            HttpsURLConnection conn = (HttpsURLConnection)uc;
            conn.setSSLSocketFactory(wrapper.getSocketFactory());
        }
        //conn.setRequestProperty("Connection","Keep-Alive");
        return uc;
    }

    private String readInput(HttpURLConnection conn) {

        String ret = "";
        try {
            // read response
            InputStream inputstream = conn.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);
            String string = null;
            while ((string = bufferedreader.readLine()) != null) {
                ret += string + "\n";
            }
        } catch (Exception ex) {
        }
        return ret;
    }

    private void closeConnection(HttpURLConnection conn) {
        
        readInput(conn);
        try {
            conn.getInputStream().close();
            conn.getOutputStream().close();
        } catch (Exception ex) {
        }
    }

    @Override
    public boolean connect() {

        HttpURLConnection conn = null;
        boolean ret = false;
        try {
            conn = getConnection("/store/server");

            InputStream inputstream = conn.getInputStream();
            InputStreamReader inputstreamreader = new InputStreamReader(inputstream);
            BufferedReader bufferedreader = new BufferedReader(inputstreamreader);

            String string = null;
            while ((string = bufferedreader.readLine()) != null) {
                System.out.println("Received " + string);
            }

            ret = true;
        } catch (Exception ex) {
            System.out.println("Error connecting, " + ex);
            //ex.printStackTrace();
            failed = true;
        }
        closeConnection(conn);
        return ret;
    }
    
    /**
     * Returns OutputStream that can be used to write data to the specified filename 
     * in the cloud. The size of the file has to be known and given.
     * 
     * @param fileInTheCloud Name of the file in the cloud
     * @param datasize Size of the file
     * @param sco observer
     * @return OutputStream that can be used to write data to the file in the cloud
     */
    @Override
    public OutputStream writeData(String fileInTheCloud, int indatasize, StorageClientObserver sco) {
    	return null; // DUMMY
    }
    
    /**
     * Returns InputStream that can be read data from a wanted file 
     * in the cloud. 
     * Note! Might be that is not working asynchronously - or at least
     * buffers tens of megabytes of data.
     * 
     * @param fileInTheCloud Name of the file in the cloud
     * @param indatasize Tells how big the data data is which is to be read.
     * 					Used only to calculate progress of the transfer.
     * 					Note! This could be also asked from server?
     * @param sco observer
     * @return InputStream that can be used to read data from a file in the cloud
     */
    @Override
    public InputStream readData(String fileInTheCloud, int indatasize, StorageClientObserver sco) {
    	return null; // DUMMY
    }
    
    /**
     * If writeData has been called, this function can be used to wait
     * for the write to complete.
     * 
     * @param timeout Maximum time that we wait for the completion
     * @return true if wait was done, else false
     */
    public boolean readDataWaitToComplete(int timeout) {
    	return true; // DUMMY
    }
    
    /**
     * Tells if all the data has been written successfully, i.e.
     * the upload has finished.
     * 
     * @return true if upload has finished, otherwise false
     */
    public boolean writeDataIsCompleted() {
    	return true; // DUMMY
    }
    
    /**
     * Returns the percentage of required data written to OutputStream 
     * that was returned by writeData function.
     * 
     * @return Percentage done (0-100%)
     */
    @Override
    public int getTransferProgress() {
    	return 100; // DUMMY
    }
    
    /**
     * If writeData has been called, this function can be used to wait
     * for the write to complete.
     * 
     * @param timeout Maximum time that we wait for the completion
     * @return true if wait was done, else false
     */
    @Override
    public boolean writeDataWaitToComplete(int timeout) {
    	return true; // DUMMY
    }
    
    @Override
    public boolean storeFile(String localInputFilename, String fileInTheCloud, StorageClientObserver sco) {
        
        HttpURLConnection conn = null;
        boolean ret = false;
        String boundary = Long.toHexString(System.currentTimeMillis());
        String crlf = "\r\n";
        try {
            // create the preamble to the file stream (incl. the other parameters ..)
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            PrintWriter writer = new PrintWriter(new OutputStreamWriter(bos, "UTF-8"));

            Hashtable<String, String> params = new Hashtable<String, String>();
            params.put("permissions", "");
            params.put("path", fileInTheCloud); // this could be set in the filename

            for (Enumeration<String> e = params.keys(); e.hasMoreElements();) {
                String k = (String)e.nextElement();
                String v = params.get(k);
                writer.print("--" + boundary + crlf);
                writer.print("Content-Disposition: form-data; name=\"" + k + "\"" + crlf);
                writer.print("Content-Type: text/plain; charset=UTF-8" + crlf + crlf);
                writer.print(v + crlf);
            }
            
            writer.print("--" + boundary + crlf);
            writer.print("Content-Disposition: form-data; name=\"data\"; filename=\""+fileInTheCloud+"\"" + crlf);
            writer.print("Content-Type: application/octet-stream" + crlf + crlf);
            writer.close();
            byte[] preamble = bos.toByteArray();

            bos = new ByteArrayOutputStream();
            writer = new PrintWriter(new OutputStreamWriter(bos, "UTF-8"));
            writer.print(crlf + "--" + boundary + "--" + crlf);
            writer.close();
            byte[] postamble = bos.toByteArray();

            File f = new File(localInputFilename);
            long total = preamble.length + f.length() + postamble.length;
            long sent = 0;
            
            // do a multipart post
            conn = getConnection("/store/store");
            conn.setDoOutput(true); // do POST
            conn.setFixedLengthStreamingMode((int)total);
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

            OutputStream out = conn.getOutputStream();
            FileInputStream fis = new FileInputStream(f);
            byte[] buf = new byte[1024*64];
            int r = 0;

            /* note: we need to bundle the first bytes from the file with the preamble,
               becase the apache commons fileupload used on the other side is buggy. */
            while ((r = fis.read(buf)) > 0) {
                if (preamble != null) {
                    byte[] tmp = new byte[preamble.length + r];
                    System.arraycopy(preamble, 0, tmp, 0, preamble.length);
                    System.arraycopy(buf, 0, tmp, preamble.length, r);
                    
                    r = preamble.length + r;
                    out.write(tmp, 0, r);
                    preamble = null;
                } else {
                    out.write(buf, 0, r);
                }
                sent += r;
                sco.progressMade((int)((sent*100) / total));
            }

            fis.close();

            out.write(postamble);
            sent += postamble.length;
            sco.progressMade((int)((sent*100) / total));
            out.flush();

            ret = true;
        } catch (Exception ex) {
            System.out.println("Error connecting, " + ex);
            ex.printStackTrace();
            failed = true;
        }
        closeConnection(conn);
        return ret;
    }

    /**
     * Gets a file stored in the cloud and saves to a local disk.
     * 
     * @param cloudFile The file in the cloud that is to be saved.
     * @param localOutFile The saved file name.
     * @return true if file saved successfully, otherwise false
     */
    @Override
    public boolean getFile(String cloudFile, String localOutFile, StorageClientObserver sco) {

        boolean ok = false;
        HttpURLConnection conn = null;
        try {
            String data = URLEncoder.encode("path", "UTF-8") + "=" + URLEncoder.encode(cloudFile, "UTF-8");

            conn = getConnection("/store/fetch");
            conn.setDoOutput(true);
            conn.setDoInput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            
            if (conn.getResponseCode() == 200) {

                InputStream in = conn.getInputStream();

                long total = conn.getContentLength();
                long sent = 0;

                System.out.println("content len is " + total);
                
                FileOutputStream out = new FileOutputStream(localOutFile);
                byte[] buf = new byte[1024*64];
                int r = 0;
                while ((r = in.read(buf)) > -1) {
                    out.write(buf, 0, r);
                    sent += r;
                    sco.progressMade((int)((sent*100) / total));
                }
                out.close();
                ok = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
        }

        closeConnection(conn);
        return ok;
    }
   
    @Override
    public boolean checkFile(String cloudFile, StorageClientObserver sco) {

        boolean ok = false;
        HttpURLConnection conn = null;
        try {
            String data = URLEncoder.encode("path", "UTF-8") + "=" + URLEncoder.encode(cloudFile, "UTF-8");

            conn = getConnection("/store/list");
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            
            String inp = readInput(conn);
            JSONObject json = new JSONObject(inp);
            if (inp.length() > 10)
                ok = true;

            JSONArray arr = json.getJSONArray("elements");
            for (int i=0; !ok && i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                if (o.getString("path").equals(cloudFile))
                    ok = true;
            }

        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
        }

        closeConnection(conn);
        return ok;
    }

    @Override
    public boolean deleteFile(String cloudFile, StorageClientObserver sco) {

        boolean ok = false;
        HttpURLConnection conn = null;
        try {
            String data = URLEncoder.encode("path", "UTF-8") + "=" + URLEncoder.encode(cloudFile, "UTF-8");

            conn = getConnection("/store/erase");
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            ok = true;
        } catch (Exception e) {
            e.printStackTrace();
            failed = true;
        }

        closeConnection(conn);
        return ok;
    }
   
    @Override
    public boolean logout() {
        return true;
    }

    @Override
    public String getURI(String path) {
        return "hipstore:///" + path;
    }
    @Override
    public String getType() {
        return "hipstore";
    }
    @Override
    public String getVersion() {
        return "1.0";
    }

    /** tests whether this connection can be reused */
    @Override
    public boolean isReusable() {
        return !failed;
    }

	@Override
	public String setURI(String newURIStart) {
		// TODO Auto-generated method stub
		return null;
	}
}
