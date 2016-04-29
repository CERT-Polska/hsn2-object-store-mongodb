/*
 * Copyright (c) NASK, NCSC
 *
 * This file is part of HoneySpider Network 2.1.
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

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pl.nask.hsn2.protobuff.ObjectStore.ObjectResponse;
import pl.nask.hsn2.protobuff.ObjectStore.ObjectResponse.ResponseType;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.ShutdownSignalException;

/**
 *
 * (C) Copyright 2011 NASK
 * Software Research & Development Department
 *
 */
public class ConnectorImpl implements Connector {

	private static final String DEFAULT_CONTENT_TYPE = "application/hsn2+protobuf";
	private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorImpl.class);
	private static volatile Connection connection = null;

	private static final int CLOSE_TIMEOUT = 100;

	private Channel channel = null;
	private QueueingConsumer servicesConsumer = null;
	private String corrId = null;
	private String serviceReplyQueueName = null;
	private String msgType;

	public static void initConnection(String connectorAddress) throws BusException {
	    if (connection == null) {
	        ConnectionFactory factory = new ConnectionFactory();
	        factory.setHost(connectorAddress);
	        try {
				connection = factory.newConnection();
			} catch (IOException e) {
				throw new BusException("Can't create connection.", e);
			}
	    } else {
	        throw new IllegalStateException("Connection already initialized");
	    }
	}

	public ConnectorImpl() throws BusException {
	    if (connection == null) {
	        throw new IllegalStateException("Connection not initialized");
	    } else {
	        getNewChannel();
	    }
	}

	private void getNewChannel() throws BusException {
	    try {
			channel = connection.createChannel();
			channel.basicQos(1);
		} catch (IOException e) {
			throw new BusException("Can't create channel.", e);
	    }
	}

	public static void close() {
		try {
		    if (connection != null){
		        connection.close(CLOSE_TIMEOUT);
		    }
		} catch (IOException e) {
			LOGGER.error("Can not close connection!", e);
		}
	}

	/* (non-Javadoc)
     * @see pl.nask.hsn2.objectStore.Connector#serviceReceive()
     */
	@Override
    public final byte[] serviceReceive() throws BusException {
		QueueingConsumer.Delivery delivery;
		try {
			delivery = servicesConsumer.nextDelivery();
		} catch (ShutdownSignalException e) {
			throw new BusException("Broker has been closed.", e);
		} catch (InterruptedException e) {
			throw new BusException("Connection to broker interuppted.", e);
		}
		String contextType = delivery.getProperties().getContentType();
		if(!contextType.equals(DEFAULT_CONTENT_TYPE)){
			throw new IllegalArgumentException("Unknow context: " + contextType);
		}

		serviceReplyQueueName = delivery.getProperties().getReplyTo();
		corrId = delivery.getProperties().getCorrelationId();
		msgType = delivery.getProperties().getType();
		LOGGER.info("Receives message. type: {}, corrID: {}", msgType, corrId);
		return delivery.getBody();
	}

	public final void createServicesConsumer(String queueName) throws BusException{
		servicesConsumer = new QueueingConsumer(channel);
		LOGGER.debug("Service queue: {}", queueName);
		try {
			channel.basicConsume(queueName, true, servicesConsumer);
		} catch (IOException e) {
			servicesConsumer = null;
			throw new BusException("Can't create consumer.", e);
		}
	}

	/* (non-Javadoc)
     * @see pl.nask.hsn2.objectStore.Connector#sendReply(pl.nask.hsn2.protobuff.ObjectStore.ObjectResponse)
     */
	@Override
    public final void sendReply(ObjectResponse objectResponse) throws BusException {
		BasicProperties properties = new BasicProperties.Builder()
				.contentType(DEFAULT_CONTENT_TYPE)
				.type("ObjectResponse")
				.correlationId(corrId)
				.build();
		try {
			channel.basicPublish("", serviceReplyQueueName , properties, objectResponse.toByteArray());
			LOGGER.info("Reply was sent to {}. corrID: {}", serviceReplyQueueName, corrId);
		} catch (IOException e) {
			throw new BusException("Can't send message.", e);
		}

	}

	/* (non-Javadoc)
     * @see pl.nask.hsn2.objectStore.Connector#sendError(java.lang.String)
     */
	@Override
    public final void sendError(String msg) {
		ObjectResponse objectResponse = ObjectResponse.newBuilder()
	        .setError(msg)
	        .setType(ResponseType.FAILURE)
	        .build();
		try {
			sendReply(objectResponse);
		} catch (BusException e) {
			LOGGER.error("Can not send taskError.",e);
		}
	}

	/* (non-Javadoc)
     * @see pl.nask.hsn2.objectStore.Connector#isConnected()
     */
	@Override
    public final boolean isConnected(){
		return servicesConsumer != null && channel.isOpen();
	}

	public final String getMsgType() {
		return msgType;
	}
}
