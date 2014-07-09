package com.onionnetworks.util;

import java.util.EventObject;

public class InvokeEvent extends EventObject {
	private static final long serialVersionUID = -2332520506167021608L;
	Runnable r;
    public InvokeEvent(Object source, Runnable r) {
	super(source);
	this.r = r;
    }
    
    public Runnable getRunnable() {
	return r;
    }
}
