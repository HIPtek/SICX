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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import com.vaadin.Application;
import com.vaadin.terminal.DownloadStream;
import com.vaadin.ui.Window;

/**
 * FileguiApplication class.
 *
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class FileguiApplication extends Application {

    private static final long serialVersionUID = 3322934315338786489L;
    private GUIComponent guicompo;
	
	/**
	 * Custom URI request handler.
	 */
	public DownloadStream handleURI(URL context, String relativeUri) {
		System.out.println("FileguiApplication processing request: '" + relativeUri + "'.");
		if(relativeUri.equals("admin") ||
	       relativeUri.equals("admin/")) {
			return super.handleURI(context, relativeUri);
		}
		
		String str = "<html><body>";
		str += "The path that you tried to access is not valid: " + relativeUri + ".<br>\n";
		str += "SICX has two interfaces you can access:<br>\n";
		str += "<ul>";
		str += "<li> Admin GUI in: <a href=\"admin/\">/admin/</a><br>\n";
		str += "<li> WebDAV interface: <a href=\"webdav/\">/webdav/</a><br>\n";
		str += "</ul>";
		str += "</body></html>";
		InputStream inp = new ByteArrayInputStream(str.getBytes());
			
		DownloadStream ret = new DownloadStream(inp, null, null);
		return ret;
	}
	
	@Override	
	public void init() {	
		Window mainWindow = new Window("FileGUI Application");
		guicompo = new GUIComponent();
		mainWindow.setContent(guicompo);
		guicompo.openLoginWindow(); // Open login GUI
		setMainWindow(mainWindow);		
	}

}
