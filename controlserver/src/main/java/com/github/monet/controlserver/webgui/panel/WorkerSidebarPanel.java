package com.github.monet.controlserver.webgui.panel;

import org.apache.wicket.markup.html.link.Link;

import com.github.monet.controlserver.webgui.MonetMainPage;

public class WorkerSidebarPanel extends SidebarPanel {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = -6943289702841543645L;

	/**
	 * Constructor.
	 */
	public WorkerSidebarPanel() {
		super();

		add(new Link<String>("link.list") {
			private static final long serialVersionUID = -5461170024771153101L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new WorkerListPanel(),
						new WorkerSidebarPanel());
			}
		});
	}
}
