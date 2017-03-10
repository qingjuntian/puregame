package com.pgame;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public abstract class Server {

	public static final Log LOG = LogFactory.getLog(Server.class);

	private final String Separator = "__";

	private int readThreads = 20; // number of read threads

	private Listener listener;

	private int port = 10001; // port we listen on

	private String ip;

	volatile private boolean running = true; // true while server runs

	public Server(String ip, int port) {
		this(ip, port, 20);
	}

	public Server(String ip, int port, int readThreadNum) {
		this.ip = ip;
		this.port = port;
		this.readThreads = readThreadNum;
		listener = new Listener();
		listener.start();
	}

	public void stop() {
		this.running = false;
		listener.terminate();
	}

	public boolean isRunning() {
		return running;
	}

	/**
	 * A convenience method to bind to a given address and report better
	 * exceptions if the address is not a valid host.
	 *
	 * @param socket
	 *            the socket to bind
	 * @param backlog
	 *            the number of connections allowed in the queue
	 * @param address
	 *            the address to bind to
	 * @throws BindException
	 *             if the address can't be bound
	 * @throws UnknownHostException
	 *             if the address isn't a valid host name
	 * @throws IOException
	 *             other random errors from bind
	 */
	private void bind(ServerSocket socket, InetSocketAddress address,
			int backlog) throws IOException {
		try {
			socket.bind(address, backlog);
		} catch (BindException e) {
			BindException bindException = new BindException(
					"Problem binding to " + address + " : " + e.getMessage());
			bindException.initCause(e);
			throw bindException;
		} catch (SocketException e) {
			// If they try to bind to a different host's address, give a better
			// error message.
			if ("Unresolved address".equals(e.getMessage())) {
				throw new UnknownHostException("Invalid hostname for server: "
						+ address.getHostName());
			} else {
				throw e;
			}
		}
	}

	protected void doWrite(SelectionKey key) {
		try {
			SocketChannel channel = (SocketChannel) key.channel();
			ByteBuffer buf = (ByteBuffer) key.attachment();
			while (buf.hasRemaining()) {
				channel.write(buf);
			}
			buf.clear();
			key.interestOps(SelectionKey.OP_READ);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected abstract void processCommand(String strCommand, SelectionKey key)
			throws IOException;

	private class Listener extends Thread {

		private ServerSocketChannel acceptChannel;

		private Selector selector;

		private Reader[] readers = null;

		private int currentReader = 0;

		private int backlogLength = 128;

		private ExecutorService readPool;

		public Listener() {
			try {
				InetSocketAddress address = new InetSocketAddress(port);
				// Create a new server socket and set to non blocking mode
				acceptChannel = ServerSocketChannel.open();
				acceptChannel.configureBlocking(false);

				// Bind the server socket to the local host and port
				bind(acceptChannel.socket(), address, backlogLength);
				port = acceptChannel.socket().getLocalPort(); // Could be an
																// ephemeral
																// port
				// create a selector;
				selector = Selector.open();
				readers = new Reader[readThreads];
				readPool = Executors.newFixedThreadPool(readThreads);
				for (int i = 0; i < readThreads; i++) {
					Reader reader = new Reader();
					readers[i] = reader;
					readPool.execute(reader);
				}

				// Register accepts on the server socket with the selector.
				acceptChannel.register(selector, SelectionKey.OP_ACCEPT);
				this.setName("IPC Server listener on " + port);
				this.setDaemon(true);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		public void terminate() {
			selector.wakeup();
		}

		public void run() {
			LOG.info(getName() + ": starting");
			while (running) {
				SelectionKey key = null;
				try {
					selector.select();
					Iterator<SelectionKey> iter = selector.selectedKeys()
							.iterator();
					while (iter.hasNext()) {
						key = iter.next();
						iter.remove();
						if (key.isValid()) {
							if (key.isAcceptable()) {
								doAccept(key);
							}
						}
						key = null;
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			LOG.info("Stopping " + this.getName());

			synchronized (this) {
				try {
					acceptChannel.close();
					selector.close();
				} catch (IOException e) {
				}

				for (Reader reader : readers) {
					reader.terminate();
				}
				readPool.shutdown();
				selector = null;
				acceptChannel = null;
			}
		}

		private class Reader implements Runnable {
			private volatile boolean adding = false;

			private Selector readSelector = null;

			Reader() throws IOException {
				readSelector = Selector.open();
			}

			public void terminate() {
				if (readSelector != null) {
					readSelector.wakeup();
				}
			}

			private void doRead(SelectionKey key) {
				SocketChannel channel = (SocketChannel) key.channel();
				try {
					ByteBuffer buffer = ByteBuffer.allocate(48);
					byte[] barr = new byte[256];
					int totalRead = 0;
					int read = channel.read(buffer);
					while (read > 0) {
						buffer.flip();
						buffer.get(barr, totalRead, read);
						buffer.clear();

						totalRead += read;
						read = channel.read(buffer);
					}

					String command = new String(barr, 0, totalRead, "UTF-8");
					processCommand(command, key);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			public void run() {
				LOG.info("Starting SocketReader");
				try {
					synchronized (this) {
						while (running) {
							SelectionKey key = null;
							try {
								readSelector.select();
								while (adding) {
									this.wait(1000);
								}
								Iterator<SelectionKey> iter = readSelector
										.selectedKeys().iterator();
								while (iter.hasNext()) {
									key = iter.next();
									iter.remove();
									if (key.isValid()) {
										if (key.isReadable()) {
											doRead(key);
										}
									}
									key = null;
								}
							} catch (IOException ex) {
								LOG.error("Error in Reader", ex);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
						readSelector.close();
						readSelector = null;
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			public void startAdd() {
				adding = true;
				readSelector.wakeup();
			}

			public synchronized void finishAdd() {
				adding = false;
				this.notify();
			}

			public synchronized SelectionKey registerChannel(
					SocketChannel channel) throws IOException {
				return channel.register(readSelector, SelectionKey.OP_READ);
			}

		}

		private void doAccept(SelectionKey key) {
			ServerSocketChannel server = (ServerSocketChannel) key.channel();
			SocketChannel channel;
			try {
				while ((channel = server.accept()) != null) {
					channel.configureBlocking(false);
					channel.socket().setTcpNoDelay(false);
					Reader reader = getReader();
					reader.startAdd();
					SelectionKey readerKey = reader.registerChannel(channel);
					ByteBuffer buf = ByteBuffer.allocate(512);
					readerKey.attach(buf);
					reader.finishAdd();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		Reader getReader() {
			currentReader = (currentReader + 1) % readers.length;
			return readers[currentReader];
		}
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getIp() {
		return ip;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public String getSeparator() {
		return Separator;
	}

}
