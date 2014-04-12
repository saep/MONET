package com.github.monet.controlserver.webgui.panel;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.Link;

import com.github.monet.common.logging.LogEvent;
import com.github.monet.controlserver.webgui.MonetMainPage;

public class LogDetailsPanel extends ContentPanel {

	private static final long serialVersionUID = -1581007677482481200L;

	public LogDetailsPanel(final LogEvent logEvent,
			final ContentPanel goBackPanel) {
		super();

		WebMarkupContainer container = new WebMarkupContainer("container") {
			private static final long serialVersionUID = -2585184474964185942L;

			@Override
			public boolean isVisible() {
				return (logEvent != null);
			}
		};
		add(container);

		if (logEvent != null) {
			SimpleDateFormat dateFormat = new SimpleDateFormat(
					"yyyy-MM-dd HH:mm:ss.SSS");
			Long timestamp = logEvent.getMillis();
			Date timestampDate = new Date(timestamp);
			String timestampString = dateFormat.format(timestampDate);

			container.add(new Label("timestamp", timestampString));
			container.add(new Label("channel", logEvent.getChannel()));
			container.add(new Label("logger", logEvent.getLogger()));
			container.add(new Label("threadname", logEvent.getThreadName()));

			// Error specific log values
			final String errorName = logEvent.getErrorName();
			final String errorMessage = logEvent.getErrorMessage();
			final String errorStacktrace = logEvent.getErrorStacktrace();

			WebMarkupContainer containerError = new WebMarkupContainer(
					"container.error") {
				private static final long serialVersionUID = 1L;

				@Override
				public boolean isVisible() {
					return !((errorName == null)
							|| (errorMessage == null)
							|| (errorStacktrace == null));
				}
			};
			containerError.add(new Label("errorname", errorName));
			containerError.add(new Label("errormessage", errorMessage));
			containerError.add(new Label("errorstacktrace", errorStacktrace));
			container.add(containerError);
			// End of error specific

			container.add(new Label("message", logEvent.getMessage()));
		}

		WebMarkupContainer containerNotFound = new WebMarkupContainer(
				"container.notfound") {
			private static final long serialVersionUID = -2585184474964185942L;

			@Override
			public boolean isVisible() {
				return (logEvent == null);
			}
		};
		add(containerNotFound);

		add(new Link<String>("link.back") {
			private static final long serialVersionUID = -2806986689782531048L;

			@Override
			public void onClick() {
				MonetMainPage.renderWithPanel(goBackPanel, MonetMainPage
						.getInstance().getActualSidebarPanel());
			}

		});
	}
}
