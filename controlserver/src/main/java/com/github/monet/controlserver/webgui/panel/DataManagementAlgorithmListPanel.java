package com.github.monet.controlserver.webgui.panel;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;

import com.github.monet.common.AlgorithmBundleDescriptor;
import com.github.monet.common.BundleDescriptor;
import com.github.monet.common.DependencyManager;

public class DataManagementAlgorithmListPanel extends ContentPanel {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = 2808246373913403488L;

	public DataManagementAlgorithmListPanel() {
		Collection<BundleDescriptor> bundleCollection = DependencyManager
				.getInstance().getAllBundles();
		final ArrayList<AlgorithmBundleDescriptor> bundleList = new ArrayList<>();
		for (BundleDescriptor b : bundleCollection) {
			if (b instanceof AlgorithmBundleDescriptor) {
				bundleList.add((AlgorithmBundleDescriptor) b);
			}
		}

		WebMarkupContainer container = new WebMarkupContainer("container.list") {
			private static final long serialVersionUID = 5098233070031898166L;

			@Override
			public boolean isVisible() {
				return !bundleList.isEmpty();
			}
		};
		add(container);

		container.add(new ListView<AlgorithmBundleDescriptor>("list.bundle",
				bundleList) {
			private static final long serialVersionUID = -8066076875313030423L;

			@Override
			protected void populateItem(ListItem<AlgorithmBundleDescriptor> item) {
				item.add(new Label("label.algorithmType",
						new PropertyModel<String>(item.getModel(),
								"algorithmType")));
				item.add(new Label("label.arity", new PropertyModel<Integer>(
						item.getModel(), "arity")));
				item.add(new Label("label.name", new PropertyModel<String>(item
						.getModel(), "name")));
				item.add(new Label("label.version", new PropertyModel<String>(
						item.getModel(), "version")));
			}
		});

		WebMarkupContainer containerNoList = new WebMarkupContainer(
				"container.listempty") {
			private static final long serialVersionUID = 2351079651853453569L;

			@Override
			public boolean isVisible() {
				return bundleList.isEmpty();
			}
		};
		add(containerNoList);
	}
}
