package com.github.monet.controlserver;

import java.util.ArrayList;
import java.util.Collection;

public class ExceptionContainer extends Exception{
	private static final long serialVersionUID = 7916645150414367431L;
	private ArrayList<Throwable> list;

	public ExceptionContainer () {
		super();
		this.list = new ArrayList<Throwable>();
	}

	public Collection<Throwable> getExceptions() {
		return this.list;
	}

	public void add(Throwable e) {
		this.list.add(e);
	}

	public void remove(Throwable e) {
		if(this.list.contains(e)) {
			this.list.remove(e);
		}
	}

	public int count() {
		return list.size();
	}
}
