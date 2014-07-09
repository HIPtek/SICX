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

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

/**
 * A graphical user-interface to demonstarte JavaHydra. Mostly
 * auto-generated with WindowBuilder from Google. The event-handlers
 * are customized.
 * 
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 * @author Eetu Korhonen
 */
public class Gui {

    private JFrame frmJhydraClientGui;
    private JTextField targetFileField;
    private JTextField keyFileField;

    final JTextArea gui_log = new JTextArea();
    /**
     * @wbp.nonvisual location=11,365
     */

//    private String log = "JHydra Client 0.1";
    private JTextField settingsXmlField;
    private JTextField serverCertificateField;
    private JTextField clientCertificateField;
    private JTextField cryptFileField;

    private char[] clientCertificatePassword;
    private char[] serverCertificatePassword;

    // Allows you to give settings and certificates as presets
    private String presetsettingsxml = "resources/hydra-settings.xml";
    private String presetclientcert = null;
    private String presetservercert = null;
    
    static private HydraSettings hydraConnections = null;
    
    public HydraSettings getHydraConnections() throws IOException, GeneralSecurityException{
        if(hydraConnections == null){
            hydraConnections = new HydraSettings();
        }
        return hydraConnections;
    }
    

    /**
     * Launch the application.
     */
    public static void main(String[] args) {
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                try {
                    Gui window = new Gui();
                    window.frmJhydraClientGui.setVisible(true);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    /**
     * Create the application.
     */
    public Gui() {
        initialize();
    }

    /**
     * Initialize the contents of the frame.
     */
    private void initialize() {

        frmJhydraClientGui = new JFrame();
        frmJhydraClientGui.setResizable(false);
        frmJhydraClientGui.setSize(new Dimension(480, 320));
        frmJhydraClientGui.setTitle("JHydra Client 0.1");
        frmJhydraClientGui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        GridBagLayout gridBagLayout = new GridBagLayout();
        gridBagLayout.rowWeights = new double[] { 0.0, 0.0, 0.0, 0.0, 1.0 };
        gridBagLayout.rowHeights = new int[] { 0, 0, 0, 0, 125 };
        gridBagLayout.columnWeights = new double[] { 1.0 };
        gridBagLayout.columnWidths = new int[] { 0 };

        frmJhydraClientGui.getContentPane().setLayout(gridBagLayout);

        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        GridBagConstraints gbc_tabbedPane = new GridBagConstraints();
        gbc_tabbedPane.anchor = GridBagConstraints.NORTH;
        gbc_tabbedPane.fill = GridBagConstraints.HORIZONTAL;
        gbc_tabbedPane.gridheight = 4;
        gbc_tabbedPane.gridy = 0;

        frmJhydraClientGui.getContentPane().add(tabbedPane, gbc_tabbedPane);

        JPanel hydraCryptPanel = new JPanel();

        tabbedPane.addTab("Hydra Crypt", null, hydraCryptPanel, null);
        GridBagLayout gbl_hydraCryptPanel = new GridBagLayout();
        gbl_hydraCryptPanel.columnWidths = new int[] { 110, 0 };
        gbl_hydraCryptPanel.rowHeights = new int[] { 35, 0, 0 };
        gbl_hydraCryptPanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gbl_hydraCryptPanel.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
        hydraCryptPanel.setLayout(gbl_hydraCryptPanel);

        JPanel cryptFilePanel = new JPanel();
        GridBagConstraints gbc_cryptFilePanel = new GridBagConstraints();
        gbc_cryptFilePanel.anchor = GridBagConstraints.NORTHEAST;
        gbc_cryptFilePanel.insets = new Insets(0, 0, 5, 0);
        gbc_cryptFilePanel.gridx = 0;
        gbc_cryptFilePanel.gridy = 0;
        hydraCryptPanel.add(cryptFilePanel, gbc_cryptFilePanel);
        cryptFilePanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JLabel lblFile = new JLabel("File");
        cryptFilePanel.add(lblFile);

        cryptFileField = new JTextField();
        cryptFileField.setEditable(false);
        cryptFilePanel.add(cryptFileField);
        cryptFileField.setColumns(20);

        JButton btnHydraCryptOpen = new JButton("Open File");
        btnHydraCryptOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * HydraCrypt Open -button
                 */
                try {
                    File hydraCryptFile = openFile("open", null);
                    cryptFileField.setText(hydraCryptFile.getPath());
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    writeToGuiLog(e1.getMessage());
                }
            }
        });
        cryptFilePanel.add(btnHydraCryptOpen);

        JPanel selectCipherPanel = new JPanel();
        GridBagConstraints gbc_selectCipherPanel = new GridBagConstraints();
        gbc_selectCipherPanel.anchor = GridBagConstraints.NORTHEAST;
        gbc_selectCipherPanel.gridx = 0;
        gbc_selectCipherPanel.gridy = 1;
        hydraCryptPanel.add(selectCipherPanel, gbc_selectCipherPanel);

        JLabel lblCipher = new JLabel("Cipher");
        selectCipherPanel.add(lblCipher);

        final JComboBox<String> selectCipherComboBox = new JComboBox<String>();
        selectCipherComboBox.addItem("Blowfish 128bits");
        selectCipherComboBox.addItem("AES 256bits");
        selectCipherPanel.add(selectCipherComboBox);

        JButton btnCryptdecrypt = new JButton("Crypt/Decrypt");

        btnCryptdecrypt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * This is where the UI-magic happens
                 */
                File filename = new File(cryptFileField.getText());

                // check if we are doing decrypt or encrypt
                boolean decrypt = true;
                HydraKey hk = null;
                try {
                    hk = HydraConnection.gatherKey(getHydraConnections(), filename.getName(), getHydraConnections().getUserId());
                    writeToGuiLog("HydraKey Collected");
                } catch (IOException eIO) {
                    // switch to crypt mode
                    if (eIO.getClass().toString().contains("NotExistsException")) {
                        writeToGuiLog("No matching entry in Hydra. Switching to Encrypt.");
                        decrypt = false;
                    }

                    // writeToGuiLog(af.dumpToString());
                } catch (NoSuchAlgorithmException e1) {

                    writeToGuiLog(e1.toString());

                } catch (GeneralSecurityException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }  

                // If we have an encryption key
                try {
                    if (hk != null && decrypt && getHydraConnections() != null) {

                        // set a filename for saving

                        try {
                            writeToGuiLog("Matching entry found, decrypting...");

                            File targetFile = openFile("save", null);
                            writeToGuiLog("Using " + hk.getAlgorithm() + "/CBC/PKCS5Padding");
                            InputStream plainTextStream = HydraCrypt.decryptFile(filename, hk, hk.getAlgorithm()
                                    + "/CBC/PKCS5Padding");
                            // target file is the same as the encrypted file
                            OutputStream target = new FileOutputStream(targetFile);
                            int cipherByte = -2;
                            // writing byte at a time. bad.
                            while (cipherByte != -1) {
                                cipherByte = plainTextStream.read();
                                if (cipherByte != -1)
                                    target.write(cipherByte);
                            }
                            target.close();
                            writeToGuiLog("Decrypted to " + targetFile.getName());
                            // removes old entry
                            HydraConnection.removeEntries(getHydraConnections(), filename.getName(), getHydraConnections().getUserId());
                            writeToGuiLog(filename.toString() + " entry removed from server.");
                            // TODO: Currently breaks down if one server is offline and does not remove its respective entry
                            //

                        } catch (Exception e1) {
                            // TODO Auto-generated catch block
                            writeToGuiLog(e1.toString());
                        }

                    } else if (!decrypt) {
                        // do encryption
                        try {
                            // generate Key
                            String algorithm = "Blowfish";
                            int length = 128;
                            int amount_of_servers = getHydraConnections().getEndpointsArray().size();
                            int required_slices = 2;
                            if (amount_of_servers > 2) {
                                required_slices = amount_of_servers - 1; // allow a single point of failure;
                            }
                            if (selectCipherComboBox.getSelectedItem().toString().contains("AES")) {
                                // to use this, you need to install the
                                // Java Cryptography Extension (JCE) Unlimited Strength Jurisdiction Policy Files
                                // Because too strong encryption is a weapon according to Dementors. :(
                                algorithm = "AES";
                                length = 256; // in bits.
                            }
                            hk = HydraKey.generateKey(algorithm, length);

                            // Select the file where we want to save the encryption
                            File targetFile = openFile("save", null);
                            // initialize cipher
                            writeToGuiLog("Using " + algorithm + "/CBC/PKCS5Padding");
                            InputStream cipherTextStream = HydraCrypt.encryptFile(filename, hk, algorithm
                                    + "/CBC/PKCS5Padding");
                            OutputStream outputFile = new FileOutputStream(targetFile);
                            int cipherByte = -2;
                            // writing byte at a time. bad.
                            while (cipherByte != -1) {
                                cipherByte = cipherTextStream.read();
                                if (cipherByte != -1)
                                    outputFile.write(cipherByte);
                            }
                            outputFile.close();
                            writeToGuiLog("Encrypted to " + targetFile.getName());
                            // Save key to hydra
                            // first, try to delete an entry if one exists
                            HydraConnection.forceRemoveEntries(getHydraConnections(), filename.getName(), getHydraConnections().getUserId());
                            writeToGuiLog("Removing existing traces of entry...");
                           // then, distribute key to hydra servers
                            writeToGuiLog("Entries created to Hydra...");
                            HydraConnection.distributeKey(getHydraConnections(), required_slices, hk, targetFile.getName(),
                                    getHydraConnections().getUserId());
                            writeToGuiLog("Key distributed to Hydra servers");

                        } catch (Exception e1) {
                            // TODO Auto-generated catch block
                            writeToGuiLog(e1.toString());
                            e1.printStackTrace();
                        }

                        //
                    }
                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                } catch (GeneralSecurityException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

            }
        });
        selectCipherPanel.add(btnCryptdecrypt);

        JPanel hydraSettingsPanel = new JPanel();
        tabbedPane.addTab("Hydra Settings", null, hydraSettingsPanel, null);
        GridBagLayout gbl_hydraSettingsPanel = new GridBagLayout();
        gbl_hydraSettingsPanel.columnWidths = new int[] { 359, 0 };
        gbl_hydraSettingsPanel.rowHeights = new int[] { 35, 35, 35, 0 };
        gbl_hydraSettingsPanel.columnWeights = new double[] { 1.0, Double.MIN_VALUE };
        gbl_hydraSettingsPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
        hydraSettingsPanel.setLayout(gbl_hydraSettingsPanel);

        JPanel settingsXMLpanel = new JPanel();
        GridBagConstraints gbc_settingsXMLpanel = new GridBagConstraints();
        gbc_settingsXMLpanel.anchor = GridBagConstraints.NORTHEAST;
        gbc_settingsXMLpanel.insets = new Insets(0, 0, 5, 0);
        gbc_settingsXMLpanel.gridx = 0;
        gbc_settingsXMLpanel.gridy = 0;
        hydraSettingsPanel.add(settingsXMLpanel, gbc_settingsXMLpanel);

        JLabel lblServicesxml = new JLabel("services.xml");
        settingsXMLpanel.add(lblServicesxml);

        settingsXmlField = new JTextField();
        settingsXmlField.setEditable(false);
        settingsXMLpanel.add(settingsXmlField);
        settingsXmlField.setColumns(20);

        JButton btnOpenSettingsXmlFile = new JButton("Open File");
        btnOpenSettingsXmlFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * Open settingsXML button
                 */
                try {
                    File settingsXmlFile = openFile("open", "xml");
                    settingsXmlField.setText(settingsXmlFile.getPath());
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    writeToGuiLog(e1.getMessage());
                }
            }
        });

        settingsXMLpanel.add(btnOpenSettingsXmlFile);

        JPanel clientCertificatePanel = new JPanel();
        GridBagConstraints gbc_clientCertificatePanel = new GridBagConstraints();
        gbc_clientCertificatePanel.anchor = GridBagConstraints.NORTHEAST;
        gbc_clientCertificatePanel.insets = new Insets(0, 0, 5, 0);
        gbc_clientCertificatePanel.gridx = 0;
        gbc_clientCertificatePanel.gridy = 1;
        hydraSettingsPanel.add(clientCertificatePanel, gbc_clientCertificatePanel);

        JLabel lblClientCertificate = new JLabel("Client Certificate");
        clientCertificatePanel.add(lblClientCertificate);

        clientCertificateField = new JTextField();
        clientCertificateField.setEditable(false);
        clientCertificatePanel.add(clientCertificateField);
        clientCertificateField.setColumns(20);

        JButton btnOpenClientCertificate = new JButton("Open File");
        btnOpenClientCertificate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * Open clientCertificate button
                 */
                try {
                    File clientCertificateFile = openFile("open", "certificate");
                    char[] password = askPassword();
                    if (password != null) {
                        clientCertificateField.setText(clientCertificateFile.getPath());
                        clientCertificatePassword = password;
                    } else {
                        writeToGuiLog("User cancelled Certificate Import");
                    }

                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    writeToGuiLog(e1.getMessage());
                }
            }
        });

        clientCertificatePanel.add(btnOpenClientCertificate);

        JPanel serverCertificatePanel = new JPanel();
        GridBagConstraints gbc_serverCertificatePanel = new GridBagConstraints();
        gbc_serverCertificatePanel.anchor = GridBagConstraints.NORTHEAST;
        gbc_serverCertificatePanel.gridx = 0;
        gbc_serverCertificatePanel.gridy = 2;
        hydraSettingsPanel.add(serverCertificatePanel, gbc_serverCertificatePanel);

        JLabel lblServerCertificate = new JLabel("Server Certificate");
        serverCertificatePanel.add(lblServerCertificate);

        serverCertificateField = new JTextField();
        serverCertificateField.setEditable(false);
        serverCertificatePanel.add(serverCertificateField);
        serverCertificateField.setColumns(20);

        JButton btnOpenServerCertificate = new JButton("Open File");
        btnOpenServerCertificate.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * Open serverCertificate button
                 */
                try {
                    File serverCertificateFile = openFile("open", "certificate");
                    char[] password = askPassword();
                    if (password != null) {
                        serverCertificateField.setText(serverCertificateFile.getPath());
                        serverCertificatePassword = password;
                    } else {
                        writeToGuiLog("User cancelled Certificate Import");
                    }

                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    writeToGuiLog(e1.getMessage());
                }
            }
        });
        serverCertificatePanel.add(btnOpenServerCertificate);

        JPanel localCryptPanel = new JPanel();
        tabbedPane.addTab("Local Crypt", null, localCryptPanel, null);
        GridBagLayout gbl_localCryptPanel = new GridBagLayout();
        gbl_localCryptPanel.columnWidths = new int[] { 479, 0 };
        gbl_localCryptPanel.rowHeights = new int[] { 57, 57, 0, 0 };
        gbl_localCryptPanel.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
        gbl_localCryptPanel.rowWeights = new double[] { 0.0, 0.0, 0.0, Double.MIN_VALUE };
        localCryptPanel.setLayout(gbl_localCryptPanel);

        JPanel file_selectors = new JPanel();

        GridBagConstraints gbc_file_selectors = new GridBagConstraints();
        gbc_file_selectors.ipadx = 3;
        gbc_file_selectors.anchor = GridBagConstraints.EAST;
        gbc_file_selectors.gridheight = 2;
        gbc_file_selectors.fill = GridBagConstraints.VERTICAL;
        gbc_file_selectors.gridx = 0;
        gbc_file_selectors.gridy = 0;
        localCryptPanel.add(file_selectors, gbc_file_selectors);
        GridBagLayout gbl_file_selectors = new GridBagLayout();
        gbl_file_selectors.columnWidths = new int[] { 447, 0 };
        gbl_file_selectors.rowHeights = new int[] { 29, 22, 0 };
        gbl_file_selectors.columnWeights = new double[] { 0.0, Double.MIN_VALUE };
        gbl_file_selectors.rowWeights = new double[] { 0.0, 0.0, Double.MIN_VALUE };
        file_selectors.setLayout(gbl_file_selectors);

        JPanel keyPanel = new JPanel();
        GridBagConstraints gbc_keyPanel = new GridBagConstraints();
        gbc_keyPanel.insets = new Insets(0, 0, 5, 0);
        gbc_keyPanel.anchor = GridBagConstraints.EAST;
        gbc_keyPanel.gridx = 0;
        gbc_keyPanel.gridy = 0;
        file_selectors.add(keyPanel, gbc_keyPanel);
        keyPanel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

        JLabel lblKey = new JLabel("Key");
        keyPanel.add(lblKey);

        keyFileField = new JTextField();
        keyFileField.setEditable(false);
        keyFileField.setColumns(20);
        keyPanel.add(keyFileField);

        JButton btnLoadKey = new JButton("Open File");
        keyPanel.add(btnLoadKey);

        btnLoadKey.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * Select keyfile-button
                 */
                try {
                    File keyFile = openFile("open", null);
                    keyFileField.setText(keyFile.getPath());
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    writeToGuiLog(e1.getMessage());
                }
            }
        });

        JPanel filePanel = new JPanel();
        FlowLayout flowLayout = (FlowLayout) filePanel.getLayout();
        flowLayout.setAlignment(FlowLayout.RIGHT);
        GridBagConstraints gbc_filePanel = new GridBagConstraints();
        gbc_filePanel.anchor = GridBagConstraints.EAST;
        gbc_filePanel.fill = GridBagConstraints.VERTICAL;
        gbc_filePanel.gridx = 0;
        gbc_filePanel.gridy = 1;
        file_selectors.add(filePanel, gbc_filePanel);

        JLabel lblTarget = new JLabel("Target");
        filePanel.add(lblTarget);

        targetFileField = new JTextField();
        targetFileField.setEditable(false);
        filePanel.add(targetFileField);
        targetFileField.setColumns(20);

        JButton btnLoadFile = new JButton("Open File");
        filePanel.add(btnLoadFile);
        btnLoadFile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * Select target field in local crypt
                 */
                try {
                    File targetFile = openFile("open", null);
                    targetFileField.setText(targetFile.getPath());
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    writeToGuiLog(e1.getMessage());
                }
            }
        });

        JPanel actionButtons = new JPanel();
        FlowLayout flowLayout_2 = (FlowLayout) actionButtons.getLayout();
        flowLayout_2.setHgap(15);
        GridBagConstraints gbc_actionButtons = new GridBagConstraints();
        gbc_actionButtons.fill = GridBagConstraints.BOTH;
        gbc_actionButtons.gridx = 0;
        gbc_actionButtons.gridy = 2;
        localCryptPanel.add(actionButtons, gbc_actionButtons);

        JButton btnDecrypt = new JButton("Decrypt");
        actionButtons.add(btnDecrypt);

        JButton btnEncrypt = new JButton("Encrypt");
        actionButtons.add(btnEncrypt);

        JButton btnGenerateKey = new JButton("Generate Key");
        actionButtons.add(btnGenerateKey);

        btnEncrypt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * Encrypt button
                 */
                try {
                    File targetFile = openFile("save", null);

                    File keyFile = new File(keyFileField.getText());
                    File plainTextFile = new File(targetFileField.getText());
                    char[] passwd = askPassword("Password for " + keyFile.getName());
                    HydraKey hk = (HydraKey) HydraFileHandler.loadSingleKeyFromFile(keyFile, passwd, null);

                    // write to a file
                    InputStream cipheredStream = HydraCrypt.encryptFile(plainTextFile, hk, "Blowfish"); // TODO: add
                                                                                                        // support for
                                                                                                        // algorithm
                                                                                                        // selection
                    OutputStream output = new FileOutputStream(targetFile);
                    int cipherByte = -2;
                    // writing byte at a time. bad.
                    while (cipherByte != -1) {
                        cipherByte = cipheredStream.read();
                        if (cipherByte != -1)
                            output.write(cipherByte);
                    }
                    output.close();
                    writeToGuiLog("Encrypted to " + targetFile.getName());
                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    writeToGuiLog(e1.getMessage());
                }
                // writeToGuiLog("Encrypt");
            }
        });
        btnDecrypt.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * Decrypt button
                 */
                try {
                    File targetFile = openFile("save", null);

                    File keyFile = new File(keyFileField.getText());
                    File cipherTextFile = new File(targetFileField.getText());
                    char[] passwd = askPassword("Password for " + keyFile.getName());
                    HydraKey hk = (HydraKey) HydraFileHandler.loadSingleKeyFromFile(keyFile, passwd, null);

                    // write to a file
                    InputStream decipheredStream = HydraCrypt.decryptFile(cipherTextFile, hk, "Blowfish"); // TODO: add
                                                                                                           // support
                                                                                                           // for
                                                                                                           // algorithm
                                                                                                           // selection
                    OutputStream output = new FileOutputStream(targetFile);
                    int cipherByte = -2;
                    // writing byte at a time. bad.
                    while (cipherByte != -1) {
                        cipherByte = decipheredStream.read();
                        if (cipherByte != -1)
                            output.write(cipherByte);
                    }
                    output.close();
                    writeToGuiLog("Decrypted to " + targetFile.getName());

                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    writeToGuiLog(e1.getMessage());
                }
                // writeToGuiLog("Encrypt");
            }
        });

        btnGenerateKey.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                /*
                 * Generate key button
                 */
                try {
                    HydraKey hk = HydraKey.generateKey();
                    File targetFile = openFile("save", null);
                    char[] password = askPassword("Password for " + targetFile.getName());
                    HydraFileHandler.saveSingleKeyToFile(hk, targetFile, password, null);
                    keyFileField.setText(targetFile.getPath());
                    writeToGuiLog("Blowfish-key " + targetFile.getName() + " generated.");
                } catch (Exception e2) {
                    writeToGuiLog(e2.getMessage());
                }
            }
        });
        gui_log.setColumns(1);
        gui_log.setLineWrap(true);

        // final JTextArea gui_log = new JTextArea();
        JScrollPane logScrollPane = new JScrollPane(gui_log);
        logScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        gui_log.setTabSize(4);
        gui_log.setText("JHydra Client 0.1");
        gui_log.setRows(5);
        gui_log.setEditable(false);
        GridBagConstraints gbc_logScrollPane = new GridBagConstraints();
        gbc_logScrollPane.gridy = 4;
        gbc_logScrollPane.gridx = 0;
        gbc_logScrollPane.fill = GridBagConstraints.BOTH;

        frmJhydraClientGui.getContentPane().add(logScrollPane, gbc_logScrollPane);

    }

    /**
     * Helper to write a newlined comment to gui_log
     */
    public void writeToGuiLog(String text) {
        gui_log.append("\n" + text);
    }

    public char[] askPassword(String label) {
        JPasswordField jpf = new JPasswordField();
        JOptionPane.showConfirmDialog(null, jpf, label, JOptionPane.OK_CANCEL_OPTION);
        return jpf.getPassword();
    }

    /**
     * A generalized open file-dialog. For DRY and simplicity
     * 
     * @throws Exception
     */
    public File openFile(String type, String filetypes) throws Exception {
        File returnValue = null;

        // TODO: Change the output to be a suitable generalization which works with common desktop-swing local files
        // and java web start fileopenservice
        // TODO: Ok, FileOpenService Seems to be VERY incompatible with our implementation :(
        /*
         * FileOpenService openFile = null; FileContents filecontent = null; String[] fileExtensions = null; if
         * (filetypes == "certificate") fileExtensions = new String[] {"p12", "jks", "ts"}; try { openFile =
         * (FileOpenService)ServiceManager.lookup("javax.jnlp.FileOpenService"); // TODO: Open and read this file if
         * (openFile != null) { filecontent = openFile.openFileDialog(null, fileExtensions); returnValue = new
         * File(filecontent.); }
         * 
         * 
         * } catch (UnavailableServiceException exc){
         */

        // backup to default Swing-FileChoosers if the webstart ain't properly set up
        // writeToGuiLog(exc.toString() + " Using Swing...");
        // reverting to backup
        JFileChooser targetFileChooser = new JFileChooser();

        int returnVal;
        if (type == "save") {
            returnVal = targetFileChooser.showSaveDialog(frmJhydraClientGui);
        } else {
            returnVal = targetFileChooser.showOpenDialog(frmJhydraClientGui);
        }

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            returnValue = targetFileChooser.getSelectedFile();
        } else {
            throw new Exception("Open command cancelled by user.");
        }

        // }
        return returnValue;
    }

    /**
     * Password-query dialog
     * 
     * @return the password given as char-array
     */
    public char[] askPassword() {
        JLabel label = new JLabel("Please enter certificate password:");
        JPasswordField jpf = new JPasswordField();

        int res = JOptionPane.showConfirmDialog(null, new Object[] { label, jpf }, "Password:",
                JOptionPane.OK_CANCEL_OPTION);

        if (res != JOptionPane.CANCEL_OPTION) {
            return jpf.getPassword();
        } else {
            return null;
        }
    }
}
