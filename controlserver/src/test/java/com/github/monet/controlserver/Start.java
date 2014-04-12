package com.github.monet.controlserver;

import java.io.File;
import java.io.IOException;

import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

import org.apache.wicket.util.time.Duration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.bio.SocketConnector;
import org.eclipse.jetty.server.ssl.SslSocketConnector;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.webapp.WebAppContext;

import com.github.monet.common.CommonArgumentParser;
import com.github.monet.common.Config;
import com.github.monet.common.logging.LogEvent;
import com.github.monet.common.logging.LoggingListener;
import com.github.monet.controlserver.BundleManager;
import com.github.monet.controlserver.ControlServer;
import com.github.monet.worker.ComAppender;
import com.github.monet.worker.WorkerMain;

public class Start {

	private static class ContolServerArgumentParser extends
			CommonArgumentParser {

		public ContolServerArgumentParser() {
			super("MONetControlServer",
					"Server for controlling monet workers, creating "
							+ "experiments and querying measured data");
		}

	}

	public static void main(String[] args) throws Exception {
		int timeout = (int) Duration.ONE_HOUR.getMilliseconds();

		/* Additional allowed arguments. */
		CommonArgumentParser argumentParser = new ContolServerArgumentParser();
		ArgumentParser argparse = argumentParser.getArgumentParser();
		argparse.addArgument("-w", "--start-worker")
				.action(Arguments.storeTrue())
				.help("Start a worker on the localhost.");
		argparse.addArgument("-u", "--upload").action(Arguments.storeTrue())
				.help("upload bundles from the cache directory");

		/* Overwrite configuration parameters with command line parameters. */
		Namespace ns = null;
		try {
			ns = argumentParser.parse(args);
		} catch (ArgumentParserException e) {
			System.err.println(e.getLocalizedMessage());
			argparse.handleError(e);
		} catch (IOException e) {
			System.err.println(e.getLocalizedMessage());
			System.exit(CommonArgumentParser.RETURN_CONTROLSERVER_IO_ERROR);
		}

		if (Config.getInstance().getConfigFile() != null
				&& Config.getInstance().getConfigFile().exists()) {
			System.err.println("Loaded configuration from: "
					+ Config.getInstance().getConfigFile().getAbsolutePath());
		}

		if (ns.getBoolean("upload")) {
			BundleManager.getInstance().uploadBundleOrBundlesInDirectory(
					new File(Config.getInstance().getBundleCacheDir()));
		}

		if (ns.getBoolean("start_worker")) {
			startWorker();
		}

		Server server = new Server();
		SocketConnector connector = new SocketConnector();

		// Set some timeout options to make debugging easier.
		connector.setMaxIdleTime(timeout);
		connector.setSoLingerTime(-1);
		connector.setPort(8081);
		server.addConnector(connector);
		/* start the controlserver backend */
		ControlServer controlServer = ControlServer.getInstance();
		ComAppender.controlPublisher = ControlServer.getControlLog();
		// print logging to console
		ControlServer.getControlLog().addLoggingListener(new LoggingListener() {
			/**
			 *
			 */
			private static final long serialVersionUID = -3046411306854009494L;

			@Override
			public void logEvent(LogEvent event) {
				System.out.println(event.toString());
			}
		});
		Thread serverThread = new Thread(controlServer, "server");
		serverThread.start();

		Resource keystore = Resource.newClassPathResource("/keystore");
		if (keystore != null && keystore.exists()) {
			// if a keystore for a SSL certificate is available, start a SSL
			// connector on port 8443.
			// By default, the quickstart comes with a Apache Wicket Quickstart
			// Certificate that expires about half way september 2021. Do not
			// use this certificate anywhere important as the passwords are
			// available in the source.

			connector.setConfidentialPort(8444);

			SslContextFactory factory = new SslContextFactory();
			factory.setKeyStoreResource(keystore);
			factory.setKeyStorePassword("wicket");
			factory.setTrustStoreResource(keystore);
			factory.setKeyManagerPassword("wicket");
			SslSocketConnector sslConnector = new SslSocketConnector(factory);
			sslConnector.setMaxIdleTime(timeout);
			sslConnector.setPort(8444);
			sslConnector.setAcceptors(4);
			server.addConnector(sslConnector);

			System.out
					.println("SSL access to the quickstart has been enabled on port 8444");
			System.out
					.println("You can access the application using SSL on https://localhost:8444");
			System.out.println();
		}

		WebAppContext bb = new WebAppContext();
		bb.setServer(server);
		bb.setContextPath("/");
		bb.setWar("src/main/webapp");

		// START JMX SERVER
		// MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
		// MBeanContainer mBeanContainer = new MBeanContainer(mBeanServer);
		// server.getContainer().addEventListener(mBeanContainer);
		// mBeanContainer.start();

		server.setHandler(bb);

		try {
			System.out
					.println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP");
			server.start();
			System.in.read();
			System.out.println(">>> STOPPING EMBEDDED JETTY SERVER");
			server.stop();
			server.join();
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}

	}

	/**
	 * Start a worker thread.
	 */
	private static void startWorker() {
		Runnable run = new Runnable() {
			@Override
			public void run() {
				System.out.println("Starting a worker on localhost.");
				try {
					final WorkerMain worker = new WorkerMain();
					worker.start();
					Runtime.getRuntime().addShutdownHook(new Thread() {
						@Override
						public void run() {
							worker.shutdownApplication();
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
					System.err.println(e);
				}
			}
		};
		Thread workerThread = new Thread(run, "worker");
		workerThread.start();
	}
}
