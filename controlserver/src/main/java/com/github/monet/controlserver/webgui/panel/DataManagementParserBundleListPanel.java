package com.github.monet.controlserver.webgui.panel;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;

import com.github.monet.common.BundleDescriptor;
import com.github.monet.common.DependencyManager;

public class DataManagementParserBundleListPanel extends ContentPanel {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = 2808246373913403488L;

	public DataManagementParserBundleListPanel() {
		Collection<BundleDescriptor> bundleCollection = DependencyManager
				.getInstance().getAllBundles();
		final ArrayList<BundleDescriptor> bundleList = new ArrayList<BundleDescriptor>();
		for (BundleDescriptor b : bundleCollection) {
			switch (b.kind()) {
			case PARSER:
				bundleList.add(b);
			default:
				break;
			}
		}

		WebMarkupContainer container = new WebMarkupContainer("container.list") {
			private static final long serialVersionUID = -9018737059875928203L;

			@Override
			public boolean isVisible() {
				return !bundleList.isEmpty();
			}
		};
		add(container);

		container.add(new ListView<BundleDescriptor>("list.parserbundle",
				bundleList) {
			private static final long serialVersionUID = -8066076875313030423L;

			@Override
			protected void populateItem(ListItem<BundleDescriptor> item) {
				item.add(new Label("label.name", new PropertyModel<String>(item
						.getModel(), "name")));
				item.add(new Label("label.version", new PropertyModel<String>(
						item.getModel(), "version")));
			}
		});

		WebMarkupContainer containerNoList = new WebMarkupContainer(
				"container.listempty") {
			private static final long serialVersionUID = -6701266188414120455L;

			@Override
			public boolean isVisible() {
				return bundleList.isEmpty();
			}
		};
		add(containerNoList);
	}
}
