package com.github.monet.controlserver.webgui.panel;

import org.apache.wicket.markup.html.link.Link;

import com.github.monet.controlserver.webgui.MonetMainPage;

public class ExperimentSidebarPanel extends SidebarPanel {

	/**
	 *
	 */
	private static final long serialVersionUID = -6943289702841543645L;

	public ExperimentSidebarPanel() {
		super();

		add(new Link<String>("link.explist") {
			/**
			 *
			 */
			private static final long serialVersionUID = -3419261702193686066L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new ExperimentListPanel(),
						new ExperimentSidebarPanel());
			}
		});

		add(new Link<String>("link.expnew") {
			/**
			 *
			 */
			private static final long serialVersionUID = -1374401967026622317L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new ExperimentNewPanel(),
						new ExperimentSidebarPanel());
			}
		});
	}
}
