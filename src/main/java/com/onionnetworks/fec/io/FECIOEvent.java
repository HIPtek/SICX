package com.onionnetworks.fec.io;

import java.util.*;

/**
 * Superclass for all FEC IO related events.
 *
 * (c) Copyright 2001 Onion Networks
 * (c) Copyright 2000 OpenCola
 *
 * @author Justin F. Chapweske (justin@chapweske.com)
 */
public class FECIOEvent extends EventObject {
	private static final long serialVersionUID = 965815774640363803L;

	public FECIOEvent(Object source) {
	super(source);
    }
}
