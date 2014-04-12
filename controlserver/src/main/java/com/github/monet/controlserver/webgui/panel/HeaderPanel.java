package com.github.monet.controlserver.webgui.panel;

import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;

import com.github.monet.common.Config;
import com.github.monet.controlserver.webgui.DocumentationLoader;
import com.github.monet.controlserver.webgui.MonetMainPage;
import com.github.monet.controlserver.webgui.MonetMainTemplate;
import com.github.monet.controlserver.webgui.DocumentationLoader.DocumentationPage;

public class HeaderPanel extends Panel {

	/**
	 * Serial version UID.
	 */
	private static final long serialVersionUID = -3630542737536482309L;
	private DocumentationLoader documentationLoader = null;

	public HeaderPanel() {
		super(MonetMainTemplate.HEADER_ID);

		this.documentationLoader = new DocumentationLoader(Config.getInstance()
				.getDocumentationrootDirectory());

		add(new Link<String>("link.logo.welcome") {
			/**
			 *
			 */
			private static final long serialVersionUID = 8903353607372257344L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new WelcomePanel(), null);
			}
		});

		add(new Link<String>("link.welcome") {
			/**
			 *
			 */
			private static final long serialVersionUID = 8903353607372257344L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new WelcomePanel(), null);
			}
		});

		add(new Link<String>("link.experiment") {
			/**
			 *
			 */
			private static final long serialVersionUID = -9046388370369317118L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new ExperimentListPanel(),
						null);
			}
		});

		add(new Link<String>("link.experiment.list") {
			/**
			 *
			 */
			private static final long serialVersionUID = -5405987971342437520L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new ExperimentListPanel(),
						null);
			}
		});

		add(new Link<String>("link.experiment.add") {
			/**
			 *
			 */
			private static final long serialVersionUID = -1628487382717367934L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new ExperimentNewPanel(),
						null);
			}
		});

		add(new Link<String>("link.worker") {
			/**
			 *
			 */
			private static final long serialVersionUID = -3884451626870182202L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new WorkerListPanel(),
						null);
			}
		});

		add(new Link<String>("link.datamanagement") {
			/**
			 *
			 */
			private static final long serialVersionUID = -9221939245077132362L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new DataManagementPanel(),
						new DataManagementSidebarPanel());
			}
		});

		add(new Link<String>("link.datamanagement.upload") {
			/**
			 *
			 */
			private static final long serialVersionUID = 6975892062784786336L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(new DataManagementPanel(),
						new DataManagementSidebarPanel());
			}
		});

		add(new Link<String>("link.datamanagement.bundlelist") {
			/**
			 *
			 */
			private static final long serialVersionUID = -4702864787256515786L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new DataManagementBundleListPanel(),
						new DataManagementSidebarPanel());
			}
		});

		add(new Link<String>("link.datamanagement.algorithmlist") {
			/**
			 *
			 */
			private static final long serialVersionUID = 2022050002957546416L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new DataManagementAlgorithmListPanel(),
						new DataManagementSidebarPanel());
			}
		});

		add(new Link<String>("link.datamanagement.graphbundlelist") {
			/**
			 *
			 */
			private static final long serialVersionUID = 5087854907519392646L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new DataManagementGraphBundleListPanel(),
						new DataManagementSidebarPanel());
			}
		});

		add(new Link<String>("link.datamanagement.parserbundlelist") {

			/**
			 *
			 */
			private static final long serialVersionUID = -3447680462214841204L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new DataManagementParserBundleListPanel(),
						new DataManagementSidebarPanel());
			}
		});

		add(new Link<String>("link.datamanagement.graphlist") {
			/**
			 *
			 */
			private static final long serialVersionUID = 6205487187830990142L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(
						new DataManagementGraphListPanel(),
						new DataManagementSidebarPanel());
			}
		});

		add(new Link<String>("link.documentation") {
			/**
			 *
			 */
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
