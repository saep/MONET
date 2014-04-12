package com.github.monet.controlserver.webgui.panel;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.Model;

import com.github.monet.controlserver.webgui.DocumentationLoader;
import com.github.monet.controlserver.webgui.MonetMainPage;
import com.github.monet.controlserver.webgui.DocumentationLoader.DocumentationPage;

/**
 * The navigation bar to the left of the documentation sub page.
 */
public class DocumentationSidebarPanel extends SidebarPanel {
	/**
	 *
	 */
	private static final long serialVersionUID = 2260429025126236976L;

	/**
	 * @see DocumentationLoader
	 */
	private DocumentationLoader docLoader;

	/**
	 * Create a side bar panel filled with information provided by the given
	 * documentation loader.
	 *
	 * @param docLoader
	 *            the documentation loader
	 * @see DocumentationLoader
	 */
	public DocumentationSidebarPanel(DocumentationLoader docLoader) {
		super();
		this.docLoader = docLoader;
		RepeatingView chapterlinks = new RepeatingView("chapters");
		add(chapterlinks);

		for (DocumentationPage p : docLoader.chapters()) {
			WebMarkupContainer container = new WebMarkupContainer(
					chapterlinks.newChildId());

			Link<DocumentationPage> link = new DocPageLink("link", p);
			link.setBody(Model.of(p.getTitle()));
			container.add(link);
			chapterlinks.add(container);

		}
	}

	/**
	 * Helper class for a link object that contains a documentation page field.
	 *
	 * @see DocumentationPage
	 */
	private class DocPageLink extends Link<DocumentationPage> {

		/**
		 *
		 */
		private static final long serialVersionUID = 5613228182613614809L;

		private DocumentationPage docPage;

		/**
		 * This is just a link with a documentation page field
		 *
		 * @param id
		 *            the wicket id for the component
		 * @param docPage
		 *            the documentation page
		 * @see Link
		 * @see DocuementationPage
		 */
		public DocPageLink(String id, DocumentationPage docPage) {
			super(id);
			this.docPage = docPage;
		}

		@Override
		public void onClick() {
			MonetMainPage.renderWithPanel(new DocumentationPanel(docPage),
					new DocumentationSidebarPanel(docLoader));
		}

	}
}
