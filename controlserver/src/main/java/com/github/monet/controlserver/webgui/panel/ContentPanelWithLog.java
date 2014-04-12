package com.github.monet.controlserver.webgui.panel;

import java.util.Iterator;

import com.github.monet.common.logging.LogEvent;

public abstract class ContentPanelWithLog extends ContentPanel {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = -181500687720199298L;

	public abstract Iterator<LogEvent> getLogEventIterator();

}
