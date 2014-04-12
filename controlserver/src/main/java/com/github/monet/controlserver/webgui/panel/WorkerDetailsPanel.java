package com.github.monet.controlserver.webgui.panel;

import java.util.Iterator;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.CompoundPropertyModel;

import com.github.monet.common.logging.LogEvent;
import com.github.monet.controlserver.WorkerDescriptor;

public class WorkerDetailsPanel extends ContentPanelWithLog {

	/**
	 * Generated serial version UID.
	 */
	private static final long serialVersionUID = -5672152934226509895L;

	private WorkerDescriptor workerDescriptor;

	public WorkerDetailsPanel(WorkerDescriptor workerDesc) {
		super();
		this.workerDescriptor = workerDesc;

		if (workerDescriptor != null) {
			setDefaultModel(new CompoundPropertyModel<WorkerDescriptor>(
					workerDescriptor));
			add(new Label("name"));
			add(new Label("state"));
			add(new Label("ip"));
			add(new Label("cpu"));
			add(new Label("ram"));
		} else {
			// TODO [Qnk] display "cannot be found" instead of throwing an
			// exception
			throw new IllegalArgumentException(
					"WorkerDescriptor must not be null!");
		}

		// Find & add log
		LogViewer logViewer = new LogViewer(this, workerDescriptor.getLog()
				.iterator());
		logViewer.addLogListView();
	}

	@Override
	public Iterator<LogEvent> getLogEventIterator() {
		return workerDescriptor.getLog().iterator();
	}
}
