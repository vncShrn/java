package com.aos.me.models;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Streams {

	public Streams(ObjectOutputStream oos, ObjectInputStream ois) {
		super();
		this.oos = oos;
		this.ois = ois;
	}

	ObjectOutputStream oos;
	ObjectInputStream ois;
	
	public ObjectOutputStream getOos() {
		return oos;
	}
	public ObjectInputStream getOis() {
		return ois;
	}

}
