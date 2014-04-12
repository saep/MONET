package com.github.monet.controlserver.webgui.panel;

import org.apache.wicket.markup.html.panel.Panel;

import com.github.monet.controlserver.webgui.MonetMainPage;

/**
 * Class which extends panel and sets the default content id as the panels id.
 * All classes which extend this class will be content panels.
 *
 * @author Johannes Kowald
 */
public class ContentPanel extends Panel {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 4326567434524675631L;

	/**
	 * Constructor which sets the default content id as the panels id.
	 */
	public ContentPanel() {
		super(MonetMainPage.CONTENT_ID);
	}
}
