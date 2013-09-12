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
import java.io.FileReader;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Properties;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import fi.hip.sicx.vaadin.LocalProperties;

/**
 * Class to keep track of the settings of given Hydra Setup and
 * interact with HydraServers as single entity.
 * 
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Eetu Korhonen
 */
public class HydraSettings {

    public static final String SERVER_LIST = "servers";
    public static final String URL_SETTING = ".url";
    public static final String HHYDRA_CONFIG_FILE = "hydraConfig";
    
    private ArrayList<HydraConnection> endpoints;
    private String userid;

    /**
     * The Constructor. Reads UserId from the Systems username property. Also stores the endpoints as an array
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public HydraSettings() throws IOException, GeneralSecurityException {
        
        this.endpoints = new ArrayList<HydraConnection>();
        readHydraSettings(LocalProperties.getInstance());
        this.userid = System.getProperty("user.name"); // uses unix user name as default
    }

    /**
     * The Constructor. Reads UserId from the Systems username property. Also stores the endpoints as an array
     * @throws GeneralSecurityException 
     * @throws IOException 
     */
    public HydraSettings(Properties props) throws IOException, GeneralSecurityException {
        
        this.endpoints = new ArrayList<HydraConnection>();
        readHydraSettings(props);
        this.userid = System.getProperty("user.name"); // uses unix user name as default
    }

    /**
     * Add a individual HydraConnection to the settings-instance
     * 
     * @param hc The established HydraConnection to be added
     */
    public void addConnectionToSettings(HydraConnection hc) {
        this.endpoints.add(hc);
    }

    /**
     * Lists the endpoints in an arrayList
     * 
     * @return The HydraConnections of current settings instance
     */
    public ArrayList<HydraConnection> getEndpointsArray() {
        return this.endpoints;
    }

    /**
     * List Endpoints of settings-instance
     * 
     * @return returns the endpoints in HydraConnection[] array
     */

    public HydraConnection[] getEndpoints() {
        HydraConnection[] returnpoints = new HydraConnection[this.endpoints.size()];
        for (int i = 0; i < this.endpoints.size(); i++) {
            returnpoints[i] = this.endpoints.get(i);
        }
        return returnpoints;
    }

    /**
     * Sets the current userid of settings-instance. Eg. username or derived hash from certificate
     * 
     * @param userid
     */
    public void setUserId(String userid) {
        this.userid = userid;
    }

    /**
     * Returns the userid attached to this settings-instance
     * 
     * @return
     */
    public String getUserId() {
        return this.userid;
    }

    /**
     * Simple, dumb parser to read a predefined settings.xml file. Does a bad job on distincting specifically
     * hydra-services.
     * 
     * @param filename
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws IOException
     * @throws ServiceException 
     * @throws GeneralSecurityException 
     */
    public void readHydraSettingsXml(File filename, Properties sslProps) throws ParserConfigurationException, SAXException, IOException, GeneralSecurityException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        // Using factory get an instance of document builder
        DocumentBuilder db = dbf.newDocumentBuilder();
        // parse using builder to get DOM representation of the XML file
        Document dom = db.parse(filename);
        Element docEle = dom.getDocumentElement();
        // <service>
        NodeList nl = docEle.getElementsByTagName("service");

        // parse endpoints
        // UGLY. There has to be a better way to parse XML...
        if (nl != null && nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Element el = (Element) nl.item(i);
                // <service name="name-of-server">
                String serviceName = el.getAttribute("name");
                NodeList params = el.getElementsByTagName("endpoint");

                String endpoint = null;
                // so far in this level...
                // <service name="hydra-server">
                // <parameters>
                // <endpoint>...
                if (params != null && params.getLength() > 0) {
                    endpoint = params.item(0).getTextContent();
                    // System.out.println(endpoint);
                }
                // System.out.println(serviceName);
                HydraConnection hc = new HydraConnection(endpoint, serviceName, sslProps);
                this.endpoints.add(hc);
            }
        }
    }
    /**
     * Reads the hydra settings from the given properties file and opens connections to them.
     * 
     * @param filename The properties file to read.
     * @param sslProps The ssl configuration to use.
     * @throws IOException Thrown in case the properties file reading fails or connection opening fails.
     * @throws GeneralSecurityException Thorn in case there are ssl problems.
     */
    public void readHydraSettings(Properties props) throws IOException, GeneralSecurityException {
        String hydraConfigFile = props.getProperty(HHYDRA_CONFIG_FILE);
        if (hydraConfigFile == null){
            throw new IOException("Variable " + HHYDRA_CONFIG_FILE + " undefined, can't read hydra settings.");
        }
        
        File hydraFile = new File(hydraConfigFile);
        
        if (!hydraFile.exists()){
            throw new IOException("Hydra configuration file: " + hydraFile.getPath() + " does not exist.");
        }
        if (!hydraFile.canRead()){
            throw new IOException("No rights to read hydra configuration file: " + hydraFile.getPath() + ".");
        }
        if (!hydraFile.isFile()){
            throw new IOException("The file : "  + hydraFile.getPath() + " given as hydra configuration file is not a file.");
        }
        
        Properties hydraProps = new Properties();
        hydraProps.load(new FileReader(hydraFile.getPath()));
        String serverList = hydraProps.getProperty(SERVER_LIST);
        if (serverList == null || serverList.trim().length() == 0){
            throw new IOException("No hydra servers defined, please define " + SERVER_LIST + " variable in " + hydraFile.getPath() + " with list of servers");
        }
        
        String servers[] = serverList.split(",");
        
        for(String server:servers){
            String url = hydraProps.getProperty(server + URL_SETTING);
            if(url == null){
                throw new IOException("no url defined for the hydra " + server);
            }
           System.out.println(url + " server: " + server);
            HydraConnection hc = new HydraConnection(url, server, props);
            this.endpoints.add(hc);
        }
        
    }
    
}
