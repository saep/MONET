package com.github.monet.worker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.monet.common.Config;
import com.github.monet.common.ExceptionUtil;
import com.github.monet.common.MonitorObject;

public class Communicator extends Thread implements Observer, Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6224638755629913796L;
	private static final Logger log = LogManager
			.getFormatterLogger(Communicator.class);
	/**
	 * String to separate the descriptor from the value in Strings received from
	 * the Controlserver.
	 */
	public static final String KEY_SEPARATOR = ": ";

	/**
	 * String describing the end of a message.
	 */
	public static final String KEY_MESSAGE_END = ".";

	/**
	 * String used to inform the Controlserver of a Statechange by the Job.
	 */
	public static final String NET_STATECHANGE = "JobStateChange";

	/**
	 * String sent by the Controlserver to terminate the Worker
	 */
	public static final String NET_TERMINATION = "TerminateWorker";

	/**
	 * String sent by the Controlserver to reset the Worker
	 */
	public static final String NET_RESET = "ResetWorker";

	/**
	 * String sent by the Controlserver to request attendance verification of a
	 * Worker.
	 */
	public static final String NET_PING = "Ping";
	public static final String NET_PONG = "Pong";

	/**
	 * String sent by the Worker to request a bundleFile, which then has to be
	 * uploaded to the requesting worker's MongoDB via network
	 */
	public static final String NET_REQUESTBUNDLE = "RequestBundle";

	/**
	 * String sent by the Controlserver to indicate a new Job on the
	 * ServerSockets InputStream.
	 */
	public static final String NET_NEWJOB = "NewJob";

	/**
	 * String sent to the Controlserver to indicate a failed Job initiation due
	 * to busyness
	 */
	public static final String NET_JOBREFUSED = "JobRefused";

	/**
	 * String sent to the Controlserver to indicate a failed Job initiation due
	 * to failure.
	 */
	public static final String NET_JOBFAILED = "JobFailed";

	/**
	 * String sent to the Controlserver to indicate a finished and therefore
	 * successful Job.
	 */
	public static final String NET_JOBFINISHED = "JobFinished";

	/**
	 * String sent to the Controlserver to verify that the worker accepted the
	 * received job and now starts to execute it.
	 */
	public static final String NET_JOBACCEPTED = "JobAccepted";

	/**
	 * String sent by the Controlserver to cancel the current Job on an employed
	 * worker.
	 */
	public static final String NET_CANCELJOB = "CancelJob";

	public static final String NET_LOGGING = "logging";
	public static final String NET_LOG_MESSAGE = "Logmessage";
	public static final String NET_LOG_TIME = "Time";
	public static final String NET_LOG_CHANNEL = "Channel";
	public static final String NET_LOG_THREADNAME = "Threadname";
	public static final String NET_LOG_LEVEL = "Level";
	public static final String NET_LOG_LOGGER = "Logger";
	public static final String NET_LOG_ERRORNAME = "errorName";
	public static final String NET_LOG_ERRORMESSAGE = "errorMessage";
	public static final String NET_LOG_ERRORSTACKTRACE = "errorStacktrace";

	/**
	 * Amount of miliseconds the communicator waits before trying to reconnect
	 * to the last control server. Currently 1 minutes.
	 */
	public static final int PING_INTERVAL = 60000;
	public static final int PING_RESPONSE_TIME = 5000;

	/**
	 * The Communicator uses this socket to establish a connection to a
	 * Controlserver.
	 */
	private Socket clientSocket;

	/**
	 * Attributes to realize network communication
	 */
	private BufferedReader inboundMessageReader;
	private PrintWriter outboundMessageWriter;

	/**
	 * Attribute used to stop the worker. As long as this is true, the worker
	 * will be doing what it's supposed to.
	 */
	private boolean active;

	/**
	 * Determines whether the Worker is currently connected to a Controlserver
	 * or not.
	 */
	private boolean connected;

	private boolean waitingForPong;

	private Experimentor experimentor;
	
	private MonitorObject monitorObject;
	private boolean wasSignalled;

	/**
	 * Constructor
	 * 
	 * @param Experimentor
	 *            the experimentor used to execute Algorithms
	 * @throws UnknownHostException
	 * @throws IOException
	 * @see Experimentor
	 */
	public Communicator(Experimentor experimentor) throws IOException {
		super("communicator");
		ComAppender.centralCom = this;
		this.experimentor = experimentor;
		active = true;
		connected = false;
		waitingForPong = false;
		monitorObject = new MonitorObject();
		wasSignalled = true;
	}

	/**
	 * Main method of the Communicator used to handle all occuring Events
	 * relevant to the Communicator. Has to be called from outside to get the
	 * worker starting
	 */
	@Override
	public void run() {
		//log.info("Communicator running");
		while (active) {
			connected = connect();
			if (!connected) {
				try {
					Thread.sleep(PING_INTERVAL);
				} catch (InterruptedException e) {
					/* boring */
				}
				continue;
			}

			List<String> listOfMessages = new LinkedList<String>();
			while (connected && active) {
				String message = "";
				try {
					message = inboundMessageReader.readLine();
				} catch (SocketTimeoutException timeout) {
					connected = handleTimeout();
					if (!connected) {
						try {
							Thread.sleep(PING_INTERVAL);
						} catch (InterruptedException e) {
							/* boring */
						}
						break;
					}
					continue;
				} catch (IOException e) {
					log.debug(e.getLocalizedMessage(), e);
					connected = false;
					break;
				}
				if (message == null) {
					connected = false;
					break;
				}

				if (message.isEmpty()) {
					continue;
				}

				if (message.equals(KEY_MESSAGE_END)) {
					handleMessages(listOfMessages);
					listOfMessages.clear();
				} else {
					/* XXX Why the fuck aren't duplicate messages allowed? */
					listOfMessages.add(message);
				}
			}
		}
		try {
			inboundMessageReader.close();
		} catch (IOException e1) {
			/* we want to quit anyway */
		} finally {
			outboundMessageWriter.close();
			try {
				clientSocket.close();
			} catch (IOException e) {
				/* we want to quit anyway */
			}
			//log.debug("Communicator finished running");
		}
	}

	private boolean handleTimeout() {
		if (waitingForPong) {
			//log.info("Disconnected from Controlserver due to a time out.");
			return false;
		}

		waitingForPong = true;
		try {
			clientSocket.setSoTimeout(PING_RESPONSE_TIME);
			this.doWait();
			sendString(NET_PING);
			sendEndingMessage();
		} catch (SocketException e) {
			e.printStackTrace();
			active = false;
			return false;
		}
		return true;
	}

	/**
	 * Connects the Worker to a Controlserver.
	 * 
	 * @return true if the connection could be established
	 */
	public boolean connect() {
		String hostAddress = Config.getInstance().getHost();
		int port = Config.getInstance().getControlPort();
		//log.info("connecting to %s:%d", hostAddress, port);
		if (clientSocket != null) {
			try {
				clientSocket.close();
			} catch (IOException e) {
				/* boring */
			}
		}
		try {
			clientSocket = new Socket(hostAddress, port);
		} catch (IOException e) {
			return false;
		}
		try {
			clientSocket.setSoTimeout(2 * PING_INTERVAL);
		} catch (SocketException e) {
			return false;
		}
		if (inboundMessageReader != null) {
			try {
				inboundMessageReader.close();
			} catch (IOException e) {
				/* boring */
			}
		}
		try {
			inboundMessageReader = new BufferedReader(new InputStreamReader(
					clientSocket.getInputStream()));
		} catch (IOException e) {
			return false;
		}
		if (outboundMessageWriter != null) {
			outboundMessageWriter.close();
		}
		try {
			outboundMessageWriter = new PrintWriter(
					clientSocket.getOutputStream(), true);
		} catch (IOException e) {
			return false;
		}
		// TODO Testing on more systems
		this.doWait();
		try {
			outboundMessageWriter.println("CPU" + KEY_SEPARATOR
					+ SysInformation.getProcInfo());
			outboundMessageWriter.println("RAM" + KEY_SEPARATOR
					+ SysInformation.getMemInfo());
			outboundMessageWriter.println("Name" + KEY_SEPARATOR
					+ SysInformation.getHostName());
		} catch (IOException e) {
			return false;
		}
		sendEndingMessage();
		//log.info("connected to %s:%d", hostAddress, port);
		return true;
	}

	/**
	 * Send a logging message.
	 * 
	 * @param channel
	 *            the channel to use, e.g. "worker" or "job-4" etc. (never null)
	 * @param millis
	 *            the time in milliseconds since 1970
	 * @param threadName
	 *            the name of the thread doing the logging, never null
	 * @param level
	 *            the log level
	 * @param logger
	 *            the name of the logger, never null
	 * @param message
	 *            the message logged, never null
	 * @param error
	 *            an error that was logged, possibly null
	 */
	public void sendLoggingMessage(String channel, long millis,
			String threadName, Level level, String logger, String message,
			Throwable error) {
		String errorName = "";
		String errorMessage = "";
		String errorStacktrace = "";

		this.doWait();
		sendString(NET_LOGGING);
		sendString(NET_LOG_CHANNEL + KEY_SEPARATOR + channel);
		sendString(NET_LOG_THREADNAME + KEY_SEPARATOR + threadName);
		sendString(NET_LOG_TIME + KEY_SEPARATOR + millis);
		if (level != null) {
			sendString(NET_LOG_LEVEL + KEY_SEPARATOR + level.toString());
		}
		sendString(NET_LOG_LOGGER + KEY_SEPARATOR + logger);
		sendString(NET_LOG_MESSAGE + KEY_SEPARATOR + message);
		if (error != null) {
			errorName = error.getClass().getName();
			errorMessage = error.getMessage();
			errorStacktrace = ExceptionUtil.stacktraceToString(error);
			sendString(NET_LOG_ERRORNAME + KEY_SEPARATOR + errorName);
			sendString(NET_LOG_ERRORMESSAGE + KEY_SEPARATOR + errorMessage);
			sendString(NET_LOG_ERRORSTACKTRACE + KEY_SEPARATOR
					+ errorStacktrace);
		}
		sendEndingMessage();
	}

	/**
	 * Method called when a String can be read on the clientSockets InputStream
	 */
	private void handleMessages(List<String> listOfMessages) {
		String firstMessage = listOfMessages.remove(0);

		switch (firstMessage) {
		case NET_NEWJOB:
			if (!experimentor.isBusy()) {
				WorkerJob newJob = parseJobFromMessageList(listOfMessages);
				try {
					experimentor.startJob(newJob);
					newJob.addObserver(this);
				} catch (Exception e) {
					log.error(e);
					e.printStackTrace();
					this.doWait();
					sendString(NET_JOBFAILED + KEY_SEPARATOR + e.toString());
					sendEndingMessage();
				}
			} else {
				this.doWait();
				sendString(NET_JOBREFUSED);
				sendEndingMessage();
			}
			break;
		case NET_TERMINATION:
			experimentor.killJob();
			active = false;
			System.exit(0);
			break;
		case NET_CANCELJOB:
			experimentor.killJob();
			break;
		case NET_PING:
			this.doWait();
			sendString(NET_PONG);
			sendEndingMessage();
			break;
		case NET_PONG:
			waitingForPong = false;
			try {
				clientSocket.setSoTimeout(2 * PING_INTERVAL);
			} catch (SocketException e) {
				e.printStackTrace();
				active = false;
				connected = false;
			}
			break;
		case NET_RESET:
			/*
			 * TODO NICELY NOT DONE FEATURE restart worker. Just start a new one
			 * an terminate the actual one, of course also cancel the actual job
			 * if given. Also delete bundles and reload them.
			 */
			/* XXX temporarily treat it as termination */
			experimentor.killJob();
			active = false;
			break;
		default:
			for (String m : listOfMessages) {
				log.error("Unhandled message: " + m);
			}
			break;
		}
	}

	/**
	 * Method used to parse a Job from a List of Strings.
	 * 
	 * @param listOfMessages
	 *            The list of Strings from which the new Job is to be parsed
	 * @return the parsed Job
	 */
	private WorkerJob parseJobFromMessageList(List<String> listOfMessages) {
		Map<String, Object> map = new HashMap<String, Object>();
		for (String message : listOfMessages) {
			/*
			 * every String has to be of the form "<description>: <value>" with
			 * <description> being a parameterkey expected by the algorithm to
			 * be found in the parametermap so it can get the value demanded by
			 * the user for this very execution of the algorithm. We here demand
			 * the message to contain exactly one ': ' which separates the
			 * descriptor from the value.
			 */
			log.trace("received String " + message);
			String[] tokens = message.split(KEY_SEPARATOR);
			if (tokens.length == 2) {
				if (describesProtocolKey(tokens[0])) {
					String key = tokens[0];
					String value = tokens[1];
					map.put(key, value);
				}
			} else if (tokens.length == 3) {
				String componentKey = tokens[0];
				String parameterKey = tokens[1];
				String value = tokens[2];
				putParameter(map, componentKey, parameterKey, value);
			}
		}
		WorkerJob newJob = new WorkerJob(map);
		// job always arrives in state SCHEDULED
		newJob.setState(Job.State.SCHEDULED);
		return newJob;
	}

	/**
	 * This method sets a parameter for a specific component, like the parser or
	 * some service which requires parameters.
	 * 
	 * @param componentKey
	 *            The component requiring the parameter
	 * @param parameterKey
	 *            the String in which the component expects to find the
	 *            parameter
	 * @param value
	 *            the value of the parameter
	 */
	private void putParameter(Map<String, Object> map, String componentKey,
			String parameterKey, Object value) {
		@SuppressWarnings("unchecked")
		Map<String, Object> parameters = (Map<String, Object>) map
				.get(componentKey);
		if (parameters == null) {
			parameters = new HashMap<String, Object>();
			map.put(componentKey, parameters);
		}
		assert (parameters.get(parameterKey) != null) : String.format(
				"%s in %s already set!", parameterKey, componentKey);
		parameters.put(parameterKey, value);
	}

	/**
	 * Method used to determine wether a used keystring is defined in the
	 * Protocol or not.
	 * 
	 * @param key
	 *            String to control
	 * @return
	 */
	private boolean describesProtocolKey(String key) {
		switch (key) {
		case WorkerJob.KEY_ALGORITHM:
		case WorkerJob.KEY_GRAPHFILE:
		case WorkerJob.KEY_GRAPHPARSER:
		case WorkerJob.KEY_JOB_ID:
			return true;
		default:
			return false;
		}
	}

	/**
	 * This method deals with occuring Events from different sources and sends
	 * accurate messages to the controlserver, dependend on the source of the
	 * Event and the context of the Event.
	 */
	@Override
	public void update(Observable o, Object arg) {
		/*
		 * update is called by a Job whenever it changes it's state.
		 */
		if (o instanceof Job) {
			Job observedJob = (Job) o;
			this.doWait();
			sendString(NET_STATECHANGE);
			sendString(observedJob.getState().toString());
			sendEndingMessage();
		} else if ((o instanceof Experimentor) && arg != null) {
			if (arg.equals(Experimentor.JOB_STARTED)) {
				this.doWait();
				sendString(NET_JOBACCEPTED);
				sendEndingMessage();
			} else if (arg.equals(Experimentor.JOB_FINISHED)) {
				this.doWait();
				sendString(NET_JOBFINISHED);
				sendEndingMessage();
			}
		} else {
			o.deleteObserver(this);
		}
	}

	private void sendString(String message) {
		for (String line : message.split(System.lineSeparator())) {
			if (line.matches("\\.+")) {
				outboundMessageWriter.println(line + ".");
			} else if (outboundMessageWriter != null) {
				outboundMessageWriter.println(line);
			} else {
				log.error("out is null and sendstring is called");
			}
		}
	}

	private void sendEndingMessage() {
		outboundMessageWriter.println(KEY_MESSAGE_END);
		outboundMessageWriter.flush();
		this.doNotify();
	}

	public void doWait() {
		synchronized (monitorObject) {
			if (!wasSignalled) {
				try {
					monitorObject.wait();
				} catch (InterruptedException e) {
				}
			}
			// clear signal and continue running.
			wasSignalled = false;
		}
	}

	public void doNotify() {
		synchronized (monitorObject) {
			wasSignalled = true;
			monitorObject.notify();
		}
	}

}
