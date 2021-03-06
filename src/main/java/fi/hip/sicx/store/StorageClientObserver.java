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

/**
 * StorageClientObserver
 *
 * To which the storage clients report their progress
 *
 * @author Joakim Koskela <jookos@gmail.com>
 * @author Seppo Heikkila <seppo.heikkila@cern.ch>
 */
public interface StorageClientObserver {

    public enum StorageClientState {
        QUEUED, IDLE, INITIALIZING, ACTIVE, INTERMEDIATE_RESULT, COMPLETE, ERROR, ERROR_PATH
    }
	
    /**
     * Reports that progress for the currently executing task has been
     * made
     */
    public void progressMade(int progressTotal);
    
    /**
     * Reports that progress for the currently executing task has been
     * made
     */
    public void progressMade(int progressTotal, StorageClientState uploadStatus);
}
