package com.github.monet.controlserver;

import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * Iterator capable of iterating over one list and then over another.
 *
 * @author Max Günther
 *
 * @param <T>
 */
public class ConjoinedIterator<T extends Serializable> implements Iterator<T>, Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -8324527405679359922L;
	private Iterator<T> it1;
	private Iterator<T> it2;
	private Iterator<T> it;

	public ConjoinedIterator(List<T> list1, List<T> list2) {
		super();
		this.it1 = list1.iterator();
		this.it2 = list2.iterator();
		this.it = this.it1;
	}

	@Override
	public synchronized boolean hasNext() {
		boolean ret = this.it.hasNext();
		if (!ret && (this.it != this.it2)) {
			this.it = this.it2;
			ret = this.it.hasNext();
		}
		return ret;
	}

	@Override
	public synchronized T next() {
		if (hasNext()) {
			return this.it.next();
		} else {
			throw new NoSuchElementException();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
