package com.github.monet.controlserver.webgui.panel;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.AjaxSelfUpdatingTimerBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.time.Duration;

import com.github.monet.common.logging.LogEvent;
import com.github.monet.controlserver.webgui.MonetMainPage;

public class LogViewer implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = -5175325763440793051L;

	private static int LOG_LIST_MAX_MSG_LEN = 240;

	private ContentPanelWithLog creatingContentPanel;
	private ArrayList<LogEvent> eventList;
	private Iterator<LogEvent> eventIterator;

	public LogViewer(ContentPanelWithLog panel, Iterator<LogEvent> eventIterator) {
		this.creatingContentPanel = panel;
		this.eventIterator = eventIterator;
		this.eventList = iteratorToList(this.eventIterator);
	}

	public void addLogListView() {

		WebMarkupContainer container = new WebMarkupContainer("container.log") {
			private static final long serialVersionUID = -8299048861356107225L;

			@Override
			public boolean isVisible() {
				return (eventList.size() >= 0);
			}
		};

		Collections.sort(eventList, new LogComparator());
		final ListView<LogEvent> genListView = new ListView<LogEvent>(
				"list.log", eventList) {
			private static final long serialVersionUID = -381293271735267174L;

			SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss.SSS");

			@Override
			protected void populateItem(ListItem<LogEvent> item) {
				final LogEvent logEvent = (LogEvent) item.getModelObject();
				// Format timestamp
				long millisecs = logEvent.getMillis();
				Date timestamp = new Date(millisecs);
				String timestampFormatted = dateFormat.format(timestamp);

				// Concatenate error message
				boolean hasError = false;
				String message = logEvent.getMessage();
				String errorStacktrace = logEvent.getErrorStacktrace();
				if (errorStacktrace != null && !errorStacktrace.isEmpty()) {
					message = "ErrorStacktrace:" + errorStacktrace + "\n"
							+ message;
					hasError = true;
				}
				String errorMessage = logEvent.getErrorMessage();
				if (errorMessage != null && !errorMessage.isEmpty()) {
					message = "ErrorMessage: " + errorMessage + "\n" + message;
					hasError = true;
				}
				String errorName = logEvent.getErrorName();
				if (errorName != null && !errorName.isEmpty()) {
					message = "Error: " + errorName + "\n" + message;
					hasError = true;
				}
				int fstWordWrap = message.indexOf("\n");
				if (fstWordWrap > -1) {
					String subMessage = message.substring(fstWordWrap + 1,
							message.length() - 1);
					if (subMessage.contains("\n")) {
						int sndWordWrap = subMessage.indexOf("\n");
						message = message.substring(0, fstWordWrap
								+ sndWordWrap);
					}
				}
				if (message.length() > LOG_LIST_MAX_MSG_LEN) {
					message = message.substring(0, LOG_LIST_MAX_MSG_LEN - 3)
							+ "...";
				}

				// Mark errors via red background
				if (hasError) {
					item.add(new AttributeAppender("id", new Model<String>("log_error"), " "));
				}

				item.add(new Label("timestamp", timestampFormatted));

				Link<LogEvent> linkDetails = new Link<LogEvent>(
						"link.logdetails", item.getModel()) {
					private static final long serialVersionUID = -8421848153729370079L;

					@Override
					public void onClick() {
						MonetMainPage.renderWithPanel(new LogDetailsPanel(
								logEvent, creatingContentPanel), MonetMainPage
								.getInstance().getActualSidebarPanel());
					}
				};
				item.add(linkDetails);
				linkDetails.add(new Label("message", message));
			}
		};
		genListView.setOutputMarkupId(true);
		container.add(genListView);

		creatingContentPanel.add(container);

		// Self updating log list (AJAX)
		container.add(new AjaxSelfUpdatingTimerBehavior(Duration.seconds(5)) {
			private static final long serialVersionUID = -2980306740198208811L;

			@Override
			protected void onPostProcessTarget(AjaxRequestTarget target) {
				eventList = iteratorToList(creatingContentPanel
						.getLogEventIterator());
				Collections.sort(eventList, new LogComparator());
				genListView.setDefaultModel(Model.ofList(eventList));
				genListView.modelChanged();
			}
		});
	}

	private synchronized ArrayList<LogEvent> iteratorToList(
			Iterator<LogEvent> iterator) {
		ArrayList<LogEvent> returnList = new ArrayList<LogEvent>();
		while (iterator.hasNext()) {
			returnList.add(iterator.next());
		}
		return returnList;
	}

	private class LogComparator implements Comparator<LogEvent>, Serializable {
		/**
		 *
		 */
		private static final long serialVersionUID = -3748616121182152275L;

		@Override
		public int compare(LogEvent le1, LogEvent le2) {
			long time1 = (long) le1.getMillis();
			long time2 = (long) le2.getMillis();
			if (time1 < time2) {
				return 1;
			}
			if (time1 > time2) {
				return -1;
			}
			return -1;
		}
	}
}
