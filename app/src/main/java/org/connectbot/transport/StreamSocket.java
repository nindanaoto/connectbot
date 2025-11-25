/*
 * ConnectBot: simple, powerful, open-source SSH client for Android
 * Copyright 2025 Kenny Root
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.connectbot.transport;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;

import com.trilead.ssh2.LocalStreamForwarder;

/**
 * A Socket implementation that wraps a LocalStreamForwarder.
 * This allows SSH tunneled connections to be used with APIs that expect a Socket,
 * such as the ProxyData interface for SSH ProxyJump support.
 */
public class StreamSocket extends Socket {
	private final LocalStreamForwarder forwarder;
	private final InputStream inputStream;
	private final OutputStream outputStream;
	private final String remoteHost;
	private final int remotePort;
	private volatile boolean closed = false;

	/**
	 * Create a new StreamSocket wrapping a LocalStreamForwarder.
	 *
	 * @param forwarder The LocalStreamForwarder providing the tunneled connection
	 * @param remoteHost The remote host this socket is connected to (for informational purposes)
	 * @param remotePort The remote port this socket is connected to (for informational purposes)
	 */
	public StreamSocket(LocalStreamForwarder forwarder, String remoteHost, int remotePort) {
		this.forwarder = forwarder;
		this.inputStream = forwarder.getInputStream();
		this.outputStream = forwarder.getOutputStream();
		this.remoteHost = remoteHost;
		this.remotePort = remotePort;
	}

	@Override
	public InputStream getInputStream() throws IOException {
		if (closed) {
			throw new SocketException("Socket is closed");
		}
		return inputStream;
	}

	@Override
	public OutputStream getOutputStream() throws IOException {
		if (closed) {
			throw new SocketException("Socket is closed");
		}
		return outputStream;
	}

	@Override
	public synchronized void close() throws IOException {
		if (!closed) {
			closed = true;
			forwarder.close();
		}
	}

	@Override
	public boolean isClosed() {
		return closed;
	}

	@Override
	public boolean isConnected() {
		return !closed;
	}

	@Override
	public boolean isInputShutdown() {
		return closed;
	}

	@Override
	public boolean isOutputShutdown() {
		return closed;
	}

	@Override
	public InetAddress getInetAddress() {
		try {
			return InetAddress.getByName(remoteHost);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public int getPort() {
		return remotePort;
	}

	@Override
	public SocketAddress getRemoteSocketAddress() {
		return null; // Not applicable for tunneled connections
	}

	@Override
	public InetAddress getLocalAddress() {
		return null; // Not applicable for tunneled connections
	}

	@Override
	public int getLocalPort() {
		return 0; // Not applicable for tunneled connections
	}

	@Override
	public SocketAddress getLocalSocketAddress() {
		return null; // Not applicable for tunneled connections
	}

	// The following methods are not supported for stream-based sockets
	// but we provide reasonable defaults

	@Override
	public void setSoTimeout(int timeout) throws SocketException {
		// Timeout is not directly supported on the underlying stream
	}

	@Override
	public int getSoTimeout() throws SocketException {
		return 0;
	}

	@Override
	public void setTcpNoDelay(boolean on) throws SocketException {
		// Not applicable for tunneled connections
	}

	@Override
	public boolean getTcpNoDelay() throws SocketException {
		return false;
	}

	@Override
	public void setKeepAlive(boolean on) throws SocketException {
		// Not applicable for tunneled connections
	}

	@Override
	public boolean getKeepAlive() throws SocketException {
		return false;
	}

	@Override
	public void setSendBufferSize(int size) throws SocketException {
		// Not applicable for tunneled connections
	}

	@Override
	public int getSendBufferSize() throws SocketException {
		return 0;
	}

	@Override
	public void setReceiveBufferSize(int size) throws SocketException {
		// Not applicable for tunneled connections
	}

	@Override
	public int getReceiveBufferSize() throws SocketException {
		return 0;
	}
}
