package com.github.monet.controlserver.webgui.panel;

import org.apache.wicket.markup.html.link.Link;

import com.github.monet.common.Config;
import com.github.monet.controlserver.webgui.DocumentationLoader;
import com.github.monet.controlserver.webgui.MonetMainPage;
import com.github.monet.controlserver.webgui.DocumentationLoader.DocumentationPage;

public class WelcomePanel extends ContentPanel {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = 8103025087588632109L;
	private DocumentationLoader documentationLoader = null;
	
	/**
	 * Constructor.
	 */
	public WelcomePanel() {
		super();
		this.documentationLoader = new DocumentationLoader(Config.getInstance()
				.getDocumentationrootDirectory());
				
		/*
		 * Add links for EXPLORE area
		 */
		
		add(new Link<String>("link.explore.workerlist") {
			private static final long serialVersionUID = -2431663381907120146L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new WorkerListPanel(), null);
			}
		});

		add(new Link<String>("link.explore.explist") {
			private static final long serialVersionUID = -9210462312324396290L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new ExperimentListPanel(), null);
			}
		});
		

		add(new Link<String>("link.explore.datamanagement") {
			private static final long serialVersionUID = 1323909037556343588L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new DataManagementPanel(), new DataManagementSidebarPanel());
			}
		});
		
		add(new Link<String>("link.explore.documentation") {
			private static final long serialVersionUID = 6017051641592245829L;

			@Override
			public void onClick() {
				DocumentationPage def = documentationLoader.chapters().get(0);
				MonetMainPage.renderWithPanel(new DocumentationPanel(def),
						new DocumentationSidebarPanel(documentationLoader));
				return;
			}
		});
		
		add(new Link<String>("link.documentation") {
			private static final long serialVersionUID = 6017051641592245829L;

			@Override
			public void onClick() {
				DocumentationPage def = documentationLoader.chapters().get(0);
				MonetMainPage.renderWithPanel(new DocumentationPanel(def),
						new DocumentationSidebarPanel(documentationLoader));
				return;
			}
		});
	}
	
	
}