package com.github.monet.controlserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Serializable;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.LinkedList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.monet.common.MonitorObject;
import com.github.monet.common.logging.LoggingPublisher;
import com.github.monet.worker.Communicator;

public class WorkerClientThread extends Thread implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = 349132542059021169L;
	private final static Logger log = LogManager
			.getFormatterLogger(WorkerClientThread.class);
	private Socket socket;
	private ControlServer server;
	private int ID;
	private BufferedReader bufferedReader;
	private PrintWriter printWriter;
	private boolean active;
	private WorkerDescriptor wd;

	private MonitorObject monitorObject;
	private boolean wasSignalled;

	public WorkerClientThread(ControlServer server, Socket socket,
			WorkerDescriptor wd) throws IOException {
		super(String.format("server-%d", socket.getPort()));
		this.wd = wd;
		this.server = server;
		this.socket = socket;
		socket.setSoTimeout(2* Communicator.PING_INTERVAL);
		this.ID = socket.getPort();
		this.active = true;
		monitorObject = new MonitorObject();
		wasSignalled = true;
	}

	@Override
	public void run() {
		log.info("Server Thread " + ID + " running.");
		LinkedList<String> listOfMessages = new LinkedList<String>();
		while (active) {
			String message = "";
			try {
				message = bufferedReader.readLine();
			} catch (SocketTimeoutException e) {
				if (wd.isResponding()) {
					wd.setResponsive(false);
					try {
						socket.setSoTimeout(Communicator.PING_RESPONSE_TIME);
					} catch (SocketException e1) {
						break;
					}
				}
				log.info("Worker timed out.");
				break;
			} catch (IOException e) {
				e.printStackTrace();
				log.error(e);
				break;
			}
			if (message == null) {
				/* Connection lost. */
				active = false;
			} else if (!message.equals("")) {
				if (message.equals(Communicator.KEY_MESSAGE_END)) {
					this.server.handleMessages(listOfMessages, this);
					listOfMessages.clear();
				} else {
					if (message.matches("\\.+")) {
						message = message.substring(1);
					}
					if (!listOfMessages.contains(message)) {
						listOfMessages.add(message);
					}
				}
			}
		}
		this.close();
		this.server.handleConnectionLoss(this.wd);
	}

	public void open() throws IOException {
		this.bufferedReader = new BufferedReader(new InputStreamReader(
				this.socket.getInputStream()));
		this.printWriter = new PrintWriter(this.socket.getOutputStream(), true);
	}

	public void close() {
		this.active = false;
		try {
			if (this.bufferedReader != null) {
				this.bufferedReader.close();
			}
		} catch (IOException e) {

		}
		if (this.printWriter != null) {
			this.printWriter.close();
		}
		try {
			if (this.socket != null) {
				this.socket.close();
			}
		} catch (IOException e) {

		}
	}

	public void sendMessage(String message) {
		for (String line : message.split(System.lineSeparator())) {
			if (line.matches("\\.+")) {
				/* XXX WHY THE FUCK? */
				this.printWriter.println(line + ".");
			} else {
				this.printWriter.println(line);
			}
		}
	}

	public void sendEndingMessage() {
		this.printWriter.println(Communicator.KEY_MESSAGE_END);
		this.doNotify();
	}

	public WorkerDescriptor getWorkerDescriptor() {
		return this.wd;
	}

	public LoggingPublisher getLoggingPublisher() {
		return this.wd.getLoggingPublisher();
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
