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
package fi.hip.sicx.streaming;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;

import org.emi.hydra.client.HydraKey;

import fi.hip.sicx.store.StorageClientObserver;

/**
 * EncryptingFECer class.
 *
 * @author Joni Hahkala <joni.hahkala@cern.ch>
 */
public class EncryptingFECer {
    private File _inputFile = null;
    private OutputStream _outStreams[] = null;
    private int _minStripes = 0;
    private StorageClientObserver _observer = null;
    private HydraKey _key = null;
    private String _algorithm = "Blowfish/CBC/PKCS5Padding";
    
    public EncryptingFECer(String filename, OutputStream outS[], int minStripes, StorageClientObserver obs) throws IOException{
        _inputFile = new File(filename);
        if (!_inputFile.exists()){
            throw new FileNotFoundException("File " + filename + " not found.");
        }
        if (_inputFile.isDirectory()){
            throw new IOException("File " + filename + " is a directory, cannot transfer directory.");
        }
        if (!_inputFile.canRead()){
            throw new IOException("File " + filename + " is inaccessible. No rights to read it.");
        }
        
        _outStreams = outS;
        _minStripes = minStripes;
        _observer = obs;
    }
    
    public HydraKey getKey(){
        return _key;
    }
    
    public void setAlgorithm(String algorithm){
        _algorithm = algorithm;
    }
    
    public boolean transfer(){
        
        
        return false;
    }
}
