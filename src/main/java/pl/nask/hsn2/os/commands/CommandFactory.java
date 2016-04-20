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

package pl.nask.hsn2.os.commands;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.os.Connector;
import pl.nask.hsn2.protobuff.Jobs.JobFinished;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectRequest.RequestType;

import com.google.protobuf.InvalidProtocolBufferException;

public class CommandFactory {
	private static final Logger LOGGER = LoggerFactory.getLogger(CommandFactory.class);
	private String jobFinishedIgnore;
	private Connector connector;

	public CommandFactory(Connector connector, String jobFinishedIgnore) {
		this.jobFinishedIgnore = jobFinishedIgnore;
		this.connector = connector;
	}

	private CommandWithResponse commandInstanceFor(RequestType type) {
		switch (type) {
			case PUT:
				return new SimplePutCommand();
			case PUT_RAW:
				return new RawPutCommand();
			case UPDATE:
				return new UpdateCommand();
			case GET:
				return new GetCommand();
			case QUERY:
				return new QueryCommand();
			default:
				throw new UnsupportedOperationException("Unsupported ObjectRequest type: " + type.name());
		}
	}

	public final Command commandFor(byte[] msg, String msgType) throws InvalidProtocolBufferException {
		switch(msgType){
			case "ObjectRequest":
				ObjectRequest objectRequest = ObjectRequest.parseFrom(msg);
			    LOGGER.debug("Received message: {}", objectRequest);
			    CommandWithResponse cmd = commandInstanceFor(objectRequest.getType());
				cmd.setObjectRequest(objectRequest);
				cmd.setConnector(connector);
				return cmd;
			case "JobFinished":
				JobFinished jobFinished = JobFinished.parseFrom(msg);
				LOGGER.debug("Received message: {}", jobFinished);
				return new JobFinishedCommand(jobFinished, jobFinishedIgnore);
			default:
				throw new UnsupportedOperationException("Unsupported message type: " + msgType);
		}
	}
}
