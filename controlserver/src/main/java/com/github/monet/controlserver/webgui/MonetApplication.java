package com.github.monet.controlserver.webgui;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.wicket.core.util.file.WebApplicationPath;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.protocol.http.WebApplication;

import com.github.monet.common.logging.LogEvent;
import com.github.monet.common.logging.LoggingListener;
import com.github.monet.controlserver.ControlServer;
import com.github.monet.worker.ComAppender;

/**
 * Application object for your web application. If you want to run this
 * application without deploying, run the Start class.
 *
 * @see monet.Start#main(String[])
 */
public class MonetApplication extends WebApplication {
	private static Logger LOG = LogManager.getLogger(MonetApplication.class);

	/**
	 * @see org.apache.wicket.Application#getHomePage()
	 */
	@Override
	public Class<? extends WebPage> getHomePage() {
		return MonetMainPage.class;
	}

	/**
	 * @see org.apache.wicket.Application#init()
	 */
	@Override
	public void init() {
		super.init();
		// add your configuration here
		getResourceSettings().getResourceFinders().add(
				new WebApplicationPath(getServletContext(), "/templates/"));

		/* start the controlserver backend */
		ControlServer controlServer = ControlServer.getInstance();
		ComAppender.controlPublisher = ControlServer.getControlLog();
		/* print logging to console */
		ControlServer.getControlLog().addLoggingListener(new LoggingListener() {
			/**
			 *
			 */
			private static final long serialVersionUID = -1694498929583641796L;

			@Override
			public void logEvent(LogEvent event) {
				LOG.info(event.toString());
			}
		});
		Thread serverThread = new Thread(controlServer, "server");
		serverThread.start();
	}
}
