package com.github.monet.controlserver.webgui.panel;

import org.apache.wicket.markup.html.basic.Label;

import com.github.monet.controlserver.webgui.DocumentationLoader.DocumentationPage;

/**
 * A simple content panel with a single label that inherits the CSS from the
 * general project and displays a markdown file as its content rendered as html.
 */
public class DocumentationPanel extends ContentPanel {
	/**
	 *
	 */
	private static final long serialVersionUID = 4627437902944208156L;

	/**
	 * Create a documentation panel for the given documentation page.
	 *
	 * @param docpage
	 *            the documentation page to render
	 *
	 * @see DocumentationPage
	 */
	public DocumentationPanel(DocumentationPage docpage) {
		super();

		Label content = new Label("markup", docpage.load());
		content.setEscapeModelStrings(false);
		add(content);
	}
}
