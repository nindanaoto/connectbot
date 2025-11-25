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
import java.net.Socket;

import com.trilead.ssh2.Connection;
import com.trilead.ssh2.LocalStreamForwarder;
import com.trilead.ssh2.ProxyData;

/**
 * ProxyData implementation that tunnels connections through an SSH jump host.
 * This enables SSH ProxyJump functionality by creating direct-tcpip channels
 * through an established SSH connection.
 *
 * Usage:
 * 1. Establish and authenticate an SSH connection to the jump host
 * 2. Create JumpHostProxyData with that connection
 * 3. Set as ProxyData on the target host's Connection before connecting
 */
public class JumpHostProxyData implements ProxyData {
	private final Connection jumpConnection;

	/**
	 * Create a new JumpHostProxyData.
	 *
	 * @param jumpConnection An established and authenticated SSH connection to the jump host.
	 *                       This connection must remain open for the duration of the tunneled connection.
	 */
	public JumpHostProxyData(Connection jumpConnection) {
		if (jumpConnection == null) {
			throw new IllegalArgumentException("Jump host connection cannot be null");
		}
		this.jumpConnection = jumpConnection;
	}

	/**
	 * Opens a tunneled connection to the target host through the jump host.
	 *
	 * @param hostname The target hostname to connect to
	 * @param port The target port to connect to
	 * @param connectTimeout Connection timeout in milliseconds (not used for tunneled connections)
	 * @return A Socket wrapping the SSH tunnel to the target host
	 * @throws IOException If the tunnel cannot be established
	 */
	@Override
	public Socket openConnection(String hostname, int port, int connectTimeout) throws IOException {
		// Create a direct-tcpip channel through the jump host to the target
		LocalStreamForwarder forwarder = jumpConnection.createLocalStreamForwarder(hostname, port);

		// Wrap the forwarder in a Socket for use by the SSH library
		return new StreamSocket(forwarder, hostname, port);
	}

	/**
	 * Get the underlying jump host connection.
	 * This can be used to check the connection status or close it when done.
	 *
	 * @return The SSH connection to the jump host
	 */
	public Connection getJumpConnection() {
		return jumpConnection;
	}
}
