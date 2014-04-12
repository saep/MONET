package com.github.monet.controlserver.webgui.panel;

import org.apache.wicket.markup.html.link.Link;

import com.github.monet.controlserver.webgui.MonetMainPage;

public class DataManagementSidebarPanel extends SidebarPanel {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = -6943289702841543645L;

	/**
	 * Constructor.
	 */
	public DataManagementSidebarPanel() {
		super();

		add(new Link<String>("link.upload") {
			private static final long serialVersionUID = 8631341445969367787L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new DataManagementPanel(),
						new DataManagementSidebarPanel());
			}
		});

		add(new Link<String>("link.bundlelist") {
			private static final long serialVersionUID = -3737931103036239487L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new DataManagementBundleListPanel(),
						new DataManagementSidebarPanel());
			}
		});


		add(new Link<String>("link.algorithmlist") {
			private static final long serialVersionUID = -6583681833692184530L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new DataManagementAlgorithmListPanel(),
						new DataManagementSidebarPanel());
			}
		});

		add(new Link<String>("link.graphbundlelist") {
			private static final long serialVersionUID = -6583681833692184530L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new DataManagementGraphBundleListPanel(),
						new DataManagementSidebarPanel());
			}
		});

		add(new Link<String>("link.parserbundlelist") {
			private static final long serialVersionUID = -3737931103036239487L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new DataManagementParserBundleListPanel(),
						new DataManagementSidebarPanel());
			}
		});


		add(new Link<String>("link.graphlist") {
			private static final long serialVersionUID = -6583681833692184530L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new DataManagementGraphListPanel(),
						new DataManagementSidebarPanel());
			}
		});
	}
}
