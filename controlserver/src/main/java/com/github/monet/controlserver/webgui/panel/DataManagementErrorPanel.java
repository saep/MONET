package com.github.monet.controlserver.webgui.panel;

import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.PropertyModel;

public class DataManagementErrorPanel extends ContentPanel {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = -7422120852387061588L;

	public DataManagementErrorPanel(Map<String, String> errorMap) {
		ArrayList<Entry<String, String>> entryList = new ArrayList<Entry<String, String>>(
				errorMap.entrySet());

		add(new ListView<Entry<String, String>>("list.errors", entryList) {
			private static final long serialVersionUID = 1L;

			protected void populateItem(
					final ListItem<Entry<String, String>> item) {
				item.add(new Label("label.key", new PropertyModel<String>(item
						.getModel(), "key")));
				item.add(new Label("label.value", new PropertyModel<String>(
						item.getModel(), "value")));
			}
		});
	}

}
