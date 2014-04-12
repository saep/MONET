package com.github.monet.controlserver.webgui.panel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;

import com.github.monet.common.Tuple;
import com.github.monet.controlserver.ControlServer;

public class DataManagementGraphListPanel extends ContentPanel {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = -970060137572574295L;

	public DataManagementGraphListPanel() {
		ControlServer controlServer = ControlServer.getInstance();
		final List<GraphDescriptor> graphList = new ArrayList<GraphDescriptor>();

		for (Tuple<String, String> g : controlServer
				.getUploadedGraphInstances()) {
			graphList.add(new GraphDescriptor(g.getFirst(), g.getSecond()));
		}
		Collections.sort(graphList);

		WebMarkupContainer container = new WebMarkupContainer("container.list") {
			private static final long serialVersionUID = 8617491931805371628L;

			@Override
			public boolean isVisible() {
				return !graphList.isEmpty();
			}
		};
		add(container);

		container.add(new ListView<GraphDescriptor>("list.graph", graphList) {
			private static final long serialVersionUID = 1274985327676473518L;

			@Override
			protected void populateItem(ListItem<GraphDescriptor> item) {
				item.add(new Label("label.id", new PropertyModel<String>(item
						.getModel(), "id")));
				item.add(new Label("label.name", new PropertyModel<String>(item
						.getModel(), "name")));
			}
		});

		WebMarkupContainer containerNoList = new WebMarkupContainer(
				"container.listempty") {
			private static final long serialVersionUID = -8161683367718928590L;

			@Override
			public boolean isVisible() {
				return graphList.isEmpty();
			}
		};
		add(containerNoList);
	}

	private class GraphDescriptor implements Comparable<GraphDescriptor>,
			Serializable {

		/**
		 *
		 */
		private static final long serialVersionUID = -6241249300018749465L;

		public String id;

		@SuppressWarnings("unused")
		public String name;

		public GraphDescriptor(String newId, String newName) {
			id = newId;
			name = newName;
		}

		@Override
		public int compareTo(GraphDescriptor otherGraph) {
			Integer id = Integer.parseInt(this.id);
			Integer id2 = Integer.parseInt(otherGraph.id);
			return id.compareTo(id2);
		}

	}

}
