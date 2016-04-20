/*
 * Copyright (c) NASK, NCSC
 *
 * This file is part of HoneySpider Network 2.0.
 *
 * This is a free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.

 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package pl.nask.hsn2.os;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.os.commands.Command;
import pl.nask.hsn2.os.commands.CommandFactory;

import com.google.protobuf.InvalidProtocolBufferException;

public class ObjectStoreConnector implements Runnable {

	private static final Logger LOGGER = LoggerFactory.getLogger(ObjectStoreConnector.class);
	private final ConnectorImpl connector;

	private CommandFactory commandFactory;

	public ObjectStoreConnector(String objectStoreQueueName, String jobFinishedIgnore) {
		try {
			connector = new ConnectorImpl();
			connector.createServicesConsumer(objectStoreQueueName);
		} catch (BusException e) {
			throw new IllegalStateException("Can not establish connection!",e);
		}
		commandFactory = new CommandFactory(connector, jobFinishedIgnore);
	}

	public final void run() {
		if (connector == null || !connector.isConnected()) {
			throw new IllegalStateException("Connections aren't established");
		}

		LOGGER.info("Object store started.");
		while (!Thread.interrupted()) {

			LOGGER.debug("Waiting for the message");
			try {
				byte[] msg = connector.serviceReceive();
				String msgType = connector.getMsgType();
				Command cmd = commandFactory.commandFor(msg, msgType);
			    cmd.execute();
			} catch (BusException e) {
				LOGGER.error("Problem with connection: " + e.getMessage(), e);
				connector.sendError("Problem with connection: " + e.getMessage());
			} catch (UnsupportedOperationException | InvalidProtocolBufferException e) {
				LOGGER.warn("Message ignored. {}", e.getMessage());
				LOGGER.debug(e.getMessage(),e);
			} catch (Throwable e) {
				LOGGER.error(e.getMessage(), e);
				connector.sendError(e.getMessage());
			}
			LOGGER.debug("Message processed");
		}
	}
}
