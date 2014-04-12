package com.github.monet.controlserver.webgui;

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.markup.html.panel.Panel;

import com.github.monet.controlserver.webgui.panel.ContentPanel;
import com.github.monet.controlserver.webgui.panel.SidebarPanel;
import com.github.monet.controlserver.webgui.panel.WelcomePanel;

public class MonetMainPage extends MonetMainTemplate {
	private static final long serialVersionUID = 1L;

	private static MonetMainPage INSTANCE;

	private Panel contentPanel, sidebarPanel;

	public MonetMainPage(final PageParameters parameters) {
		this(parameters, new WelcomePanel(), null);
		getSidebarPanel().setVisible(false);
	}

	public MonetMainPage(final PageParameters parameters,
			Panel actualContentPanel, Panel actualSidebarPanel) {
		super(parameters);
		setStatelessHint(true);

		INSTANCE = this;

		contentPanel = actualContentPanel;
		replace(contentPanel);

		if (actualSidebarPanel != null) {
			sidebarPanel = actualSidebarPanel;
			replace(sidebarPanel);
		}
	}

	public static void renderWithPanel(ContentPanel newContentPanel,
			SidebarPanel newSidebarPanel) {
		MonetMainPage newMonetMainPage = new MonetMainPage(
				INSTANCE.getPageParameters(), newContentPanel, newSidebarPanel);
		newMonetMainPage.getSidebarPanel().setVisible(newSidebarPanel != null);
		INSTANCE.setResponsePage(newMonetMainPage);
	}

	public SidebarPanel getActualSidebarPanel() {
		return (SidebarPanel) sidebarPanel;
	}

	public static MonetMainPage getInstance() {
		return INSTANCE;
	}
}
