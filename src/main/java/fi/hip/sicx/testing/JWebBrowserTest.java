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
package fi.hip.sicx.testing;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import chrriis.common.UIUtils;
import chrriis.dj.nativeswing.swtimpl.NativeInterface;
import chrriis.dj.nativeswing.swtimpl.components.JWebBrowser;

/**
 * JWebBrowserTest class.
 *
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Henri Mikkonen <henri.mikkonen@nimbleidm.com>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 */
public class JWebBrowserTest extends JPanel {

	  public JWebBrowserTest() {
	    super(new BorderLayout());
	    JPanel webBrowserPanel = new JPanel(new BorderLayout());
	    webBrowserPanel.setBorder(BorderFactory.createTitledBorder("SICX Client"));
	    final JWebBrowser webBrowser = new JWebBrowser();
	    webBrowser.navigate("http://localhost:8081/sicx");
	    //webBrowser.navigate("http://demo.vaadin.com/sampler/#DragDropHtml5FromDesktop");
	    webBrowser.setBarsVisible(false);
	    webBrowserPanel.add(webBrowser, BorderLayout.CENTER);
	    add(webBrowserPanel, BorderLayout.CENTER);
	    // Create an additional bar allowing to show/hide the menu bar of the web browser.
	    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 4));
	    JCheckBox menuBarCheckBox = new JCheckBox("Menu Bar", webBrowser.isMenuBarVisible());
	    menuBarCheckBox.addItemListener(new ItemListener() {
	      public void itemStateChanged(ItemEvent e) {
	        webBrowser.setMenuBarVisible(e.getStateChange() == ItemEvent.SELECTED);
	      }
	    });
	    //buttonPanel.add(menuBarCheckBox);
	    //add(buttonPanel, BorderLayout.SOUTH);
	  }

	  /* Standard main method to try that test as a standalone application. */
	  public static void main(String[] args) {
	    UIUtils.setPreferredLookAndFeel();
	    NativeInterface.open();
	    SwingUtilities.invokeLater(new Runnable() {
	      public void run() {
	        JFrame frame = new JFrame("SICX Client");
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	        frame.getContentPane().add(new JWebBrowserTest(), BorderLayout.CENTER);
	        frame.setSize(800, 600);
	        frame.setLocationByPlatform(true);
	        frame.setVisible(true);
	      }
	    });
	    NativeInterface.runEventPump();
	  }
}
