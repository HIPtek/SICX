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
package fi.hip.sicx.webdav;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.AbstractHandler;

import com.bradmcevoy.http.Response;
import com.bradmcevoy.http.AuthenticationService;
import com.bradmcevoy.http.ServletHttpManager;
import com.bradmcevoy.http.webdav.DefaultWebDavResponseHandler;
import com.bradmcevoy.http.webdav.WebDavResponseHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Handler of WebDAV servlet requests.
 *
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 */
public class WebdavHandler extends AbstractHandler
{
    private ServletHttpManager httpManager;
    private static AuthenticationService authenticationService;
    private static WebdavResourceFactory resourceFactory;
    private WebDavResponseHandler responseHandler;
    
    public WebdavHandler() {}
    
    /**
     * 
     * @throws ServletException
     */
    public void initHandler() throws ServletException {
        if (authenticationService == null) {
            authenticationService = new AuthenticationService();
            resourceFactory = new WebdavResourceFactory();
        }
        responseHandler = new DefaultWebDavResponseHandler(authenticationService); 
        httpManager = new ServletHttpManager(resourceFactory, responseHandler, authenticationService);
    }
	
    /**
     * Handles the servlet requests.
     */
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) 
        throws IOException, ServletException
    {
    	if(target.startsWith("/webdav") == true) {
	    System.out.println("WebdavHandler req " + baseRequest.getMethod() + " : '" + target + "'." );
	    com.bradmcevoy.http.Request miltonRequest = new com.bradmcevoy.http.ServletRequest(request,null);
	    Response miltonResponse = new com.bradmcevoy.http.ServletResponse(response);
	    httpManager.process(miltonRequest, miltonResponse);
	    baseRequest.setHandled(true);
    	}
    }
}
