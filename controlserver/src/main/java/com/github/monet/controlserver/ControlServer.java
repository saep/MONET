package com.github.monet.controlserver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.FileAlreadyExistsException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.monet.common.CommonArgumentParser;
import com.github.monet.common.Config;
import com.github.monet.common.DBCollections;
import com.github.monet.common.RuntimeIOException;
import com.github.monet.common.Tuple;
import com.github.monet.common.logging.LogEvent;
import com.github.monet.common.logging.LoggingPublisher;
import com.github.monet.worker.Communicator;
import com.github.monet.worker.IllegalStateTransition;
import com.github.monet.worker.Job;
import com.github.monet.worker.Job.State;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSInputFile;

public class ControlServer implements Runnable, Observer, Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 3382276364029657074L;

	/**
	 * Singleton instance.
	 */
	private static ControlServer instance;

	private final static Logger log = LogManager
			.getFormatterLogger(ControlServer.class);

	/**
	 * The DummyControlserver listens to this port waiting for a message to come
	 * in. This will in all cases the registration of a new worker.
	 */
	private ServerSocket serverSocket;

	/**
	 * Hashmap for the Controlserver to find a WorkerClientThread corresponding
	 * to a given WorkerDescriptor
	 */
	private HashMap<WorkerDescriptor, WorkerClientThread> workerMap;

	/**
	 * Attribute used to stop the DummyControlserver. As long as this is true,
	 * the DummyControlServer will be doing what it's supposed to.
	 */
	private boolean isActive;

	/**
	 * Attributed used by the Controlserver to add Workers as they register
	 * themselves to the Controlserver
	 */
	private Scheduler scheduler;

	/**
	 * Attributes used to upload files to the MongoDB
	 */
	public DB db;

	/**
	 * GridFS object for accessing and storing graph input files.
	 */
	public GridFS graph_files;

	/**
	 * A loggingPublisher for all logging messages on the Controlserver (and not
	 * just this class, but all events on this execution unit, except for any
	 * logging messages from a worker that might have been started with this
	 * Controlserver).
	 */
	private static LoggingPublisher controlLog = new LoggingPublisher();

	private long graphCounter;

	private long attendanceTime;
	private long reconnectTime;

	private static int scheduleInterval = 5000;
	private static long reconnectInterval = 30000;

	/**
	 * Creates a ControlServer. This does however not create a singleton.
	 *
	 * @param scheduler
	 *            the scheduler
	 * @return the instantiated, but not running ControlServer
	 * @throws IOException
	 */
	private ControlServer() {
		try {
			this.serverSocket = new ServerSocket(Config.getInstance()
					.getControlPort());
			this.scheduler = new Scheduler();
			this.isActive = true;
			this.workerMap = new HashMap<WorkerDescriptor, WorkerClientThread>();
			this.db = Config.getDBInstance();
			this.graph_files = new GridFS(this.db, DBCollections.GRAPH_FILES);
			this.attendanceTime = System.currentTimeMillis()
					+ Communicator.PING_INTERVAL;
			// little tweak to make the controlserver connect to workers from db
			// immediately
			this.reconnectTime = System.currentTimeMillis() + 1;
			this.serverSocket.setSoTimeout(scheduleInterval);
			CommonArgumentParser.createCacheDirectoriesIfMissing();
		} catch (IOException e) {
			log.error(e.getLocalizedMessage());
			throw new RuntimeIOException(e);
		}
	}

	/**
	 * Returns the last singleton ControlServer created by
	 * {@link #getInstance(int, Scheduler, DB)}. Aforementioned method must have
	 * been called at least once.
	 *
	 * @return
	 * @throws IOException
	 */
	public synchronized static ControlServer getInstance() {
		if (instance == null) {
			instance = new ControlServer();
		}
		return instance;
	}

	/**
	 * Main method of the ControlServer used to handle all occurring Events
	 * relevant to the ControlServer. Has to be called from outside to get it
	 * starting
	 */
	@Override
	public void run() {
		while (this.isActive) {
			Socket client = null;
			try {
				client = this.serverSocket.accept();
				log.info("Controlserver: connection established");
				this.handleConnection(client);
			} catch (SocketTimeoutException to) {
				/*
				 * Whenever the serverSocket.accept stops blocking via timeout,
				 * try to schedule a Job to a Worker which is currently
				 * attended. This results in a schedule interval of
				 * approximately the blocking rate of the serverSocket.
				 * accept-Method, which is set in static attribute
				 * scheduleInterval
				 */
				log.trace("about to schedule");
				CSJob job = this.scheduler.schedule();
				if ((job != null) && !job.getState().isFinal()) {
					log.debug("scheduled " + job.getID());
					WorkerDescriptor wd = job.getWorker();
					if (wd != null) {
						job.addObserver(this);
						wd.setJob(job);
						sendJob(job, wd);
					}
				} else {
					log.trace("no jobs to schedule");
				}
				if (System.currentTimeMillis() > this.attendanceTime) {
					this.checkAttendance();
					this.attendanceTime = System.currentTimeMillis()
							+ Communicator.PING_INTERVAL;
				}
				if (System.currentTimeMillis() > this.reconnectTime) {
					log.trace("==connections attempted via loadWorkersFromDB==");
					WorkerDescriptor.loadWorkersFromDB();
					reconnectTime = System.currentTimeMillis()
							+ ControlServer.reconnectInterval;
				}
			} catch (IOException e) {
				e.printStackTrace();
				if (client != null) {
					try {
						client.close();
					} catch (IOException e2) {
					}
				}
			} catch (Exception ex) {
				// catch any kind of exception
				if (!this.isActive) {
					// ignore exceptions that occur during
					// shutdown
					log.warn(ex);
					break;
				} else {
					// log all other kinds of exceptions
					log.error(ex);
				}
			}
		}
		try {
			this.serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			this.stop();
		} catch (ExceptionContainer e) {
			for (Throwable f : e.getExceptions()) {
				f.printStackTrace();
			}
		}
	}

	/**
	 * Stop the server.
	 */
	public void stop() throws ExceptionContainer {
		this.isActive = false;
		ExceptionContainer cont = new ExceptionContainer();
		for (WorkerClientThread wct : this.workerMap.values()) {
			try {
				this.killWorker(wct.getWorkerDescriptor());
				// TODO: ControlServer.stop() requires more tests, David
			} catch (Exception e) {
				// ControlServer.stop(): gather all exceptions occurring
				// when stopping the controlserver and throw one including all
				// exceptions (probably needs an entirely new exception class)
				// e.printStackTrace();
				cont.add(e);
			}
		}
		if (cont.count() != 0) {
			throw cont;
		}
	}

	/**
	 * Method called to start a new thread corresponding to a single Worker
	 *
	 * @param wd
	 *            the WorkerDescriptor of the new Worker
	 * @param socket
	 *            the socket on which the Worker is connected
	 */
	private void addThread(WorkerDescriptor wd, Socket socket) {
		try {
			WorkerClientThread client = new WorkerClientThread(this, socket, wd);
			client.open();
			client.start();
			this.workerMap.put(wd, client);
		} catch (IOException ioe) {
			log.error("Error opening thread: " + ioe);
		}
	}

	/**
	 * Method called by a workerthread as soon as it received a series of
	 * Messages
	 *
	 * @param listOfMessages
	 *            the List of String sent by the Worker
	 * @param wd
	 *            the Description of the worker which sent the messages
	 */
	public synchronized void handleMessages(Iterable<String> listOfMessages,
			WorkerClientThread wct) {
		Iterator<String> it = listOfMessages.iterator();
		String netkey = it.next();
		WorkerDescriptor workerDescriptor = wct.getWorkerDescriptor();
		CSJob job = workerDescriptor.getJob();
		switch (netkey) {
		case Communicator.NET_LOGGING:
			this.handleLogMessage(it, wct);
			break;
		case Communicator.NET_JOBFINISHED:
			/*
			 * In this case, the worker confirms that the job is done, wether it
			 * is canceled, finished or failed. The state is already set by a
			 * separate statechange message.
			 */
			if (job != null) {
				job.deleteObserver(this);
				workerDescriptor.setJob(null);
			}
			workerDescriptor.setState(
					WorkerDescriptor.STATE_UNEMPLOYED);
			break;
		case Communicator.NET_STATECHANGE:
			String state = "";
			while (it.hasNext()) {
				state = state.concat(it.next());
			}
			if (job != null) {
				try {
					try {
						job.setState(State.valueOf(state));
					} catch (IllegalArgumentException a) {
						job.setState(state);
					}
				} catch (IllegalStateTransition e) {
					/*
					 * This should never be thrown at this point, because the
					 * state transition has already been done successfully on
					 * workerside.
					 */
				}
			}
			break;
		case Communicator.NET_JOBACCEPTED:
			log.info("Job accepted by " + workerDescriptor.getName());
			if (job != null) {
				job.setState(Job.State.INITIALIZING);
			}
			break;
		case Communicator.NET_JOBREFUSED:
			log.info("Job refused by " + workerDescriptor.getName());
			if (job != null) {
				job.setState(Job.State.NEW);
			}
			break;
		case Communicator.NET_PONG:
			workerDescriptor.setResponsive(true);
			break;
		case Communicator.NET_PING:
			wct.doWait();
			wct.sendMessage(Communicator.NET_PONG);
			wct.sendEndingMessage();
			workerDescriptor.setResponsive(true);
			break;
		default:
			/*
			 * actually, there shouldn't be any more cases. This is just to
			 * console-output it, in case there is.
			 */
			log.error("Unknown netkey: " + netkey);
			for (String message : listOfMessages) {
				log.info("received message " + message + " from "
						+ workerDescriptor.getName());
			}
		}
	}

	private void handleLogMessage(Iterator<String> it, WorkerClientThread wct) {
		String lastKey = "";
		String content = "";
		String channel = "";
		String level = "";
		String logger = "";
		String threadname = "";
		String message = "";
		String errorname = "";
		String errormessage = "";
		String errorstacktrace = "";
		long millis = 0;
		String lineSeparator = System.getProperty("line.separator");
		String next = "";
		while (it.hasNext()) {
			next = it.next();
			String tokens[] = next.split(Communicator.KEY_SEPARATOR, 2);
			if (tokens.length == 2) {
				if (tokens[0].equals(Communicator.NET_LOG_TIME)) {
					millis = Long.valueOf(tokens[1]).longValue();
				} else if (this.describesLogKey(tokens[0])) {
					lastKey = tokens[0];
					content = tokens[1];
				} else {
					content = next;
				}
			} else if (tokens.length == 1) {
				content = tokens[0];
			}
			if (content.isEmpty()) {
				continue;
			}
			switch (lastKey) {
			case Communicator.NET_LOG_CHANNEL:
				channel = content;
				break;
			case Communicator.NET_LOG_LEVEL:
				level = content;
				break;
			case Communicator.NET_LOG_LOGGER:
				logger = content;
				break;
			case Communicator.NET_LOG_THREADNAME:
				threadname = content;
				break;
			case Communicator.NET_LOG_ERRORNAME:
				if (!errorname.equals("")) {
					errorname = errorname.concat(lineSeparator);
				}
				errorname = errorname.concat(content);
				break;
			case Communicator.NET_LOG_ERRORMESSAGE:
				if (!errormessage.equals("")) {
					errormessage = errormessage.concat(lineSeparator);
				}
				errormessage = errormessage.concat(content);
				break;
			case Communicator.NET_LOG_ERRORSTACKTRACE:
				if (!errorstacktrace.equals("")) {
					errorstacktrace = errorstacktrace.concat(lineSeparator);
				}
				errorstacktrace = errorstacktrace.concat(content);
				break;
			case Communicator.NET_LOG_MESSAGE:
				if (!message.equals("")) {
					message = message.concat(lineSeparator);
				}
				message = message.concat(content);
				break;
			default:
				log.warn("Unknown key: " + lastKey);
			}
		}
		LogEvent logEvent;
		if (!(errorname + errorstacktrace + errormessage).equals("")) {
			logEvent = new LogEvent(channel, millis, threadname, level, logger,
					message, errorname, errormessage, errorstacktrace);
			System.err.println("Error occured on channel " + channel);
		} else {
			logEvent = new LogEvent(channel, millis, threadname, level, logger,
					message);
		}

		if (channel.equals("worker")) {
			wct.getLoggingPublisher().publishLoggingEvent(logEvent);
		} else if (channel.startsWith("job")) {
			CSJob job = wct.getWorkerDescriptor().getJob();
			job.getLogPublisher().publishLoggingEvent(logEvent);
		}
	}

	private boolean describesLogKey(String key) {
		switch (key) {
		case Communicator.NET_LOG_CHANNEL:
		case Communicator.NET_LOG_ERRORMESSAGE:
		case Communicator.NET_LOG_ERRORNAME:
		case Communicator.NET_LOG_ERRORSTACKTRACE:
		case Communicator.NET_LOG_LEVEL:
		case Communicator.NET_LOG_LOGGER:
		case Communicator.NET_LOG_MESSAGE:
		case Communicator.NET_LOG_THREADNAME:
		case Communicator.NET_LOG_TIME:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Method called when a Worker establishes a Connection to the
	 * DummyControlServer, which only can mean that it has to be registered to
	 * the Controlserver as new and available Worker.
	 *
	 * @param client
	 *            The Socket of the Worker which is connected.
	 * @throws IOException
	 */
	private void handleConnection(Socket client) throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(
				client.getInputStream()));
		String cpu = "";
		String ram = "";
		String name = "";
		String nextString = "";
		String[] messageTokens;
		while (!nextString.equals(Communicator.KEY_MESSAGE_END)) {
			nextString = in.readLine();
			if (nextString == null) {
				/* Connection lost. */
				// TODO something reasonable
				return;
			}
			messageTokens = nextString.split(Communicator.KEY_SEPARATOR);
			if (messageTokens.length == 2) {
				if (messageTokens[0].equals("CPU")) {
					cpu = messageTokens[1];
				}
				if (messageTokens[0].equals("RAM")) {
					ram = messageTokens[1];
				}
				if (messageTokens[0].equals("Name")) {
					name = messageTokens[1];
				}
			}
		}
		WorkerDescriptor wd = WorkerDescriptor.getWorkers().get(name);
		if (wd != null) {
			wd.setResponsive(true);
			wd.setState(WorkerDescriptor.STATE_UNEMPLOYED);
		} else {
			try {
				wd = new WorkerDescriptor(client.getInetAddress()
						.getHostAddress(), cpu, ram, name);
			} catch (ControlServerException e) {
				// Never happens, because the name is checked.
			}
		}
		this.scheduler.registerWorker(wd);
		this.addThread(wd, client);
		// Induce execution of InitiateDummyAlgo on worker
		log.info("Worker " + this.workerMap.get(wd).getName() + " added, "
				+ wd.ip);
		// this.initiateEA(wd);
		// log.info("DummyAlgo sent to " + name);
		// in.close();
	}


	/**
	 * Method used to send a Job to a worker
	 *
	 * @param job
	 *            the job to be
	 * @param wd
	 */
	public void sendJob(CSJob job, WorkerDescriptor wd) {
		if (job != null) {
			WorkerClientThread wct = this.workerMap.get(wd);
			Map<String, Object> jobmap = job.getParameters();

			wct.doWait();
			wct.sendMessage(Communicator.NET_NEWJOB);
			if (jobmap != null) {
				for (String componentKey : job.getParameterizedComponents()) {
					for (String parameterKey : job.getParameters(componentKey)
							.keySet()) {
						wct.sendMessage(componentKey
								+ Communicator.KEY_SEPARATOR
								+ parameterKey
								+ Communicator.KEY_SEPARATOR
								+ job.getParameters(componentKey).get(
										parameterKey));
					}
				}
			}
			wct.sendMessage(CSJob.KEY_JOB_ID + Communicator.KEY_SEPARATOR + job.getID());
			wct.sendMessage(CSJob.KEY_ALGORITHM + Communicator.KEY_SEPARATOR
					+ job.getAlgorithmDescriptor());
			wct.sendMessage(CSJob.KEY_GRAPHPARSER + Communicator.KEY_SEPARATOR
					+ job.getParserDescriptor());
			wct.sendMessage(CSJob.KEY_GRAPHFILE + Communicator.KEY_SEPARATOR
					+ job.getGraphDescriptor());
			wct.sendEndingMessage();
		}
	}

	/**
	 * Method used to kill a job, which is just canceling it.
	 *
	 * @param wd
	 *            the workerdescriptor of the worker working on the job which is
	 *            to be cancelled.
	 */
	public void killJob(WorkerDescriptor wd) {
		/*
		 * only do something if the worker really is employed
		 */
		if (wd.getState().equals(WorkerDescriptor.STATE_EMPLOYED)) {
			CSJob job = wd.getJob();
			if (job != null) {
				try {
					job.setState(Job.State.CANCELLING);
				} catch (IllegalStateTransition e) {
					/*
					 * At this point, the Job already should be finished and
					 * therefore shouldn't cause any trouble. This error is no
					 * problem at all. However, it is nice to know.
					 */
					log.info("killJob caused an illegal state transition");
				}
			}
		}
	}

	/**
	 * Method used to cancel a job.
	 *
	 * @param job
	 *            the job which is to be cancelled
	 */
	public void killJob(CSJob job) {
		if (job != null) {
			if (job.getState() == Job.State.NEW) {
				job.setState(Job.State.CANCELLED);
			} else {
				WorkerDescriptor wd = job.getWorker();
				if (wd != null) {
					this.killJob(wd);
				} else {
					job.setState(Job.State.CANCELLED);
				}
			}
		}
	}

	/**
	 * Method used to kill a worker
	 *
	 * @param wd
	 *            the Desciptor of the worker to be killed
	 */
	public void killWorker(WorkerDescriptor wd) {
		log.debug("killing worker " + wd.getName());
		if (wd != null) {
			if (wd.getState().equals(WorkerDescriptor.STATE_EMPLOYED)) {
				this.killJob(wd);
			}
			WorkerClientThread wct = this.workerMap.get(wd);
			if (wct != null) {
				wct.doWait();
				wct.sendMessage(Communicator.NET_TERMINATION);
				wct.sendEndingMessage();
				wct.close();
				this.scheduler.unregisterWorker(wd);
			} else {
				this.scheduler.unregisterWorker(wd);
				this.workerMap.remove(wct);
			}
		}
	}

	/**
	 * method used to check if a given Worker is still attended, meaning it
	 * responds to messages. Also disconnects from the worker if it's timed out.
	 *
	 * @param wd
	 *            the workerdescriptor for the worker which is to be checked for
	 */
	private void checkAttendance(WorkerDescriptor wd) {
		WorkerClientThread wct = this.workerMap.get(wd);
		if (wct == null) {
			this.workerMap.remove(wd);
		} else if (wd.isTimedOut()) {
			log.info(wd.getName()
					+ " is now unavailable due to unattendance.");
			wct.close();
			this.workerMap.remove(wd);
			this.scheduler.unregisterWorker(wd);
			CSJob job = wd.getJob();
			if (job != null) {
				try {
					job.setState(Job.State.FAILED);
				} catch (IllegalStateTransition e) {
					log.error("State transition " + job.getState() + " to "
							+ Job.State.FAILED.getName() + " is illegal.");
				}
			}
		} else {
			wct.doWait();
			wct.sendMessage(Communicator.NET_PING);
			wct.sendEndingMessage();
			wd.checkAttendance(wct);
		}
	}

	/**
	 * Method used to check if all Workers are still attended.
	 */
	public void checkAttendance() {
		for (WorkerDescriptor wd : this.workerMap.keySet()) {
			if (!wd.getState().equals(WorkerDescriptor.STATE_UNAVAILABLE)) {
				this.checkAttendance(wd);
			}
		}
	}

	/**
	 * Delivers a Collection of all Workerdescriptors corresponding to workers
	 * which failed last attendance check.
	 *
	 * @return ArrayList of WorkerDescriptors to unattended Workers.
	 */
	public Collection<WorkerDescriptor> getUnattendedWorkers() {
		ArrayList<WorkerDescriptor> result = new ArrayList<WorkerDescriptor>();
		for (WorkerDescriptor wd : workerMap.keySet()) {
			if (wd.isTimedOut()) {
				result.add(wd);
			}
		}
		return result;
	}

	/**
	 * Method used to upload a graph instance file which is found on
	 * {@code path}. It's safed inside the database with name {@code mongofile}
	 *
	 * @param path
	 *            the path where the instance is found
	 * @param mongofile
	 *            the name of the file as it is saved in mongodb
	 * @return id from the instance file inside mongodb or null, if no file is
	 *         found
	 * @throws IOException
	 *             Error from mongodb, to be fixed
	 */
	public String uploadGraphInstance(String path, String mongofile)
			throws IOException, FileAlreadyExistsException {

		// TODO run a parser on the graph instance to verify integrity
		if (this.graph_files.findOne(mongofile) != null) {
			throw new FileAlreadyExistsException(mongofile);
		}

		BasicDBObject query = new BasicDBObject();
		query.put("_id", Long.toString(this.graphCounter));
		while (this.graph_files.findOne(query) != null) {
			query.put("_id", Long.toString(++this.graphCounter));
		}

		File inputInstance = new File(path);
		if (inputInstance.exists() && inputInstance.canRead()) {
			final GridFSInputFile mongoFile = this.graph_files
					.createFile(inputInstance);
			mongoFile.setFilename(mongofile);
			mongoFile.put("_id", Long.toString(this.graphCounter++));
			mongoFile.save();
			mongoFile.validate();

			return mongoFile.getId().toString();
		} else {
			throw new FileNotFoundException(path);
		}
	}

	/**
	 * Method used to inform the Controlserver class about a connection
	 * interrupted
	 *
	 * @param wct
	 *            the WorkerClientThread which lost connection to its respective
	 *            worker
	 */
	public void handleConnectionLoss(WorkerDescriptor wd) {
		WorkerClientThread wct = this.workerMap.get(wd);
		log.info(wd.getName() + " disconnected.");
		if (wct != null) {
			wct.close();
			this.workerMap.remove(wd);
			this.scheduler.unregisterWorker(wd);
			CSJob job = wd.getJob();
			if (job != null) {
				try {
					job.setState(Job.State.FAILED);
				} catch (IllegalStateTransition e) {
					log.error("State transition " + job.getState() + " to "
							+ Job.State.FAILED + " is illegal.");
				}
			}
			wd.setState(WorkerDescriptor.STATE_UNAVAILABLE);
		} else {
			wd.setState(WorkerDescriptor.STATE_UNAVAILABLE);
		}
	}

	/**
	 * Retrieve all graph instances' meta information and return a list of
	 * tuples where the first element is the id of the graph in the database and
	 * the second is the name of the graph instance.
	 *
	 * @return a list of tuples where the first
	 */
	public List<Tuple<String, String>> getUploadedGraphInstances() {
		List<Tuple<String, String>> toReturn = new ArrayList<>();
		DBCursor cursor = this.graph_files.getFileList();

		while (cursor.hasNext()) {
			cursor.next();
			DBObject current = cursor.curr();
			toReturn.add(new Tuple<String, String>(//
			current.get("_id").toString(),//
			current.get("filename").toString()));
		}

		return toReturn;
	}

	/**
	 * Method used to send a Message to a Worker
	 *
	 * @param wd
	 *            The Description of the Worker to which the message is to be
	 *            sent to
	 * @param message
	 *            The Message to be sent
	 */
	public void sendString(WorkerDescriptor wd, String message) {
		WorkerClientThread wcd = workerMap.get(wd);
		if (wcd != null) {
			wcd.sendMessage(message);
		} else {
			/* TODO delete workerdescriptor or someting? */
		}
	}

	/**
	 * Method used to end a series of Messages
	 *
	 * @param wd
	 *            the Description of the Worker to which the ending message is
	 *            to be sent
	 */
	public void sendEndingMessage(WorkerDescriptor wd) {
		WorkerClientThread wcd = this.workerMap.get(wd);
		wcd.sendEndingMessage();
	}

	public static LoggingPublisher getControlLog() {
		ControlServer.getInstance();
		return controlLog;
	}

	/**
	 * The Controlserver disconnects from the worker by literally just dropping
	 * the connection. No further communication or anything. This method should
	 * only be used by the WorkerListPanel.
	 *
	 * @param wd
	 */
	public void disconnect(WorkerDescriptor wd) {
		wd.setState(WorkerDescriptor.STATE_UNAVAILABLE);
		WorkerClientThread wct = this.workerMap.get(wd);
		wct.close();
	}

	@Override
	/**
	 * The Controlserver observers CSJobs to be informed about
	 * status changes of the CSjob, in particular if it
	 * changes to "canceled". In this case, the Controlserver
	 * has to inform the Worker executing this job.
	 */
	public void update(Observable arg0, Object arg1) {
		if (arg0 instanceof CSJob) {
			CSJob job = (CSJob) arg0;
			if (job.getState() == CSJob.State.CANCELLING) {
				WorkerDescriptor wd = job.getWorker();
				sendString(wd, Communicator.NET_CANCELJOB);
				sendEndingMessage(wd);
				// wd.setJob(null);
				// wd.setState(WorkerDescriptor.STATE_UNEMPLOYED);
				// job.deleteObserver(this);
			} else if (job.getState().isFinal()) {
				WorkerDescriptor wd = job.getWorker();
				wd.setJob(null);
				wd.setState(WorkerDescriptor.STATE_UNEMPLOYED);
				job.deleteObserver(this);
			}
		}
	}

	public static synchronized Scheduler getScheduler() {
		return getInstance().scheduler;
	}

}
