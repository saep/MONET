package com.github.monet.controlserver.webgui;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.github.monet.controlserver.webgui.panel.FooterPanel;
import com.github.monet.controlserver.webgui.panel.HeaderPanel;
import com.github.monet.controlserver.webgui.panel.SidebarPanel;

public class MonetMainTemplate extends WebPage {
	private static final long serialVersionUID = -7978128002262460000L;

	public static final String HEADER_ID = "headerPanel";
	public static final String SIDEBAR_ID = "sidebarPanel";
	public static final String CONTENT_ID = "contentPanel";
	public static final String FOOTER_ID = "footerPanel";

	private Component headerPanel;
	private Component sidebarPanel;
	private Component footerPanel;

	public MonetMainTemplate(PageParameters parameters) {
		super(parameters);
		add(headerPanel = new HeaderPanel());
		add(sidebarPanel = new SidebarPanel());
		add(new Label(CONTENT_ID, "No content to display"));
		add(footerPanel = new FooterPanel());

	}

	public Component getSidebarPanel() {
		return sidebarPanel;
	}

	public Component getFooterPanel() {
		return footerPanel;
	}

	public Component getHeaderPanel() {
		return headerPanel;
	}
}
