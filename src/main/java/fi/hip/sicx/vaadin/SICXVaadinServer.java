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

package fi.hip.sicx.vaadin;

import java.io.IOException;
import java.security.GeneralSecurityException;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import com.vaadin.terminal.gwt.server.ApplicationServlet;

/**
 * SICXVaadinServer class.
 *
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 */
public class SICXVaadinServer {

    private Server _server = null;
    public static final String PORT_OPT = "port";
    public static final String HOST_OPT = "host";
    
    public SICXVaadinServer(){
    }
    
    public void join() throws InterruptedException{
        _server.join();
    }
    
    public void stop() throws Exception{
        if(_server != null){
            _server.stop();
        }
    }
    
    public void start() throws Exception{
        _server.start();
    }
    
    public void configure() throws IOException, GeneralSecurityException{
        _server = new Server(50668);
        _server.setSendServerVersion(false);
        _server.setSendDateHeader(false);
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        _server.setHandler(context);
        
        ServletHolder holder = new ServletHolder(new ApplicationServlet());
//        Map<String, String> map = new HashMap<String, String> ();
        holder.setInitParameter("application", "fi.hip.sicx.vaadin.FileguiApplication");
        context.addServlet(holder, "/*");

    }

    /**
     * @param args
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        SICXVaadinServer server = new SICXVaadinServer();
        server.configure();
        server.start();
        server.join();        
    }

}
