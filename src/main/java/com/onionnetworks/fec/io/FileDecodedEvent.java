package com.onionnetworks.fec.io;

/**
 * This event signifies that the file has been completely decoded.
 *
 * (c) Copyright 2001 Onion Networks
 * (c) Copyright 2000 OpenCola
 *
 * @author Justin F. Chapweske (justin@chapweske.com)
 */
public class FileDecodedEvent extends FECIOEvent {

	private static final long serialVersionUID = 8778435804724696964L;

	public FileDecodedEvent(Object source) {
	super(source);
    }
}
	
