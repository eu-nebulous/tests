package eut.nebulouscloud.tests.common;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.qpid.protonj2.client.Message;
import org.apache.qpid.protonj2.client.exceptions.ClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.ObjectMapper;

import eu.nebulouscloud.exn.Connector;
import eu.nebulouscloud.exn.core.Consumer;
import eu.nebulouscloud.exn.core.Context;
import eu.nebulouscloud.exn.core.Handler;
import eu.nebulouscloud.exn.handlers.ConnectorHandler;
import eu.nebulouscloud.exn.settings.StaticExnConfig;

public class NebulOuSMessageBrokerListener {

	static Logger LOGGER = LoggerFactory.getLogger(NebulOuSMessageBrokerListener.class);

	private static NebulOuSMessageBrokerListener instance;
	final String brokerHost = Optional.ofNullable(System.getenv("NEBULOUS_BROKER_HOST")).orElse("localhost"); 
	final int brokerPort = Integer.parseInt(Optional.ofNullable(System.getenv("NEBULOUS_BROKER_PORT")).orElse("5672")); 
	final String brokerUser = Optional.ofNullable(System.getenv("NEBULOUS_BROKER_USER")).orElseThrow(() -> new IllegalStateException("NEBULOUS_BROKER_USER env var is not defined"));
	final String brokerPassword = Optional.ofNullable(System.getenv("NEBULOUS_BROKER_PASSWORD")).orElseThrow(() -> new IllegalStateException("NEBULOUS_BROKER_PASSWORD env var is not defined"));
	
	final String APP_ID ="0918402606rest-processor-app1719386320641";
	
	protected ObjectMapper om = new ObjectMapper();

	private Connector myEXNClient;

	private class MyConsumerHandler extends Handler {
		@Override
		public void onMessage(String key, String address, Map body, Message message, Context context) {
			String to = "??";
			try {
				to = message.to() != null ? message.to() : address;
			} catch (Exception ex) {
				ex.printStackTrace();
			}
			Map<Object, Object> props = new HashMap<Object, Object>();
			try {
				message.forEachProperty((k, v) -> props.put(k, v));
			} catch (ClientException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String subject = "?";
			try {
				subject = message.subject();
			} catch (ClientException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if(APP_ID!=null && !APP_ID.isEmpty() && !APP_ID.equals(subject)) return;
			

			LOGGER.info("\r\n{}\r\nsubject:{}\r\npayload:{}\r\nproperties:{}", to, subject, body, props);
			try {
				LOGGER.info(om.writeValueAsString(body));
			} catch (JsonProcessingException e) {
			}
		}
	}

	private class MyConnectorHandler extends ConnectorHandler {
		public void onReady(AtomicReference<Context> context) {
			LOGGER.info("Optimiser-controller connected to ActiveMQ");
		}
	}

	private NebulOuSMessageBrokerListener() {
		Consumer cons1 = new Consumer("monitoring", "eu.nebulouscloud.>", new MyConsumerHandler(), true, true);
		myEXNClient = new Connector("thisINotImportant", new MyConnectorHandler(), List.of(), List.of(cons1), true,
				true, new StaticExnConfig(brokerHost, brokerPort, brokerUser, brokerPassword));
		myEXNClient.start();
	}

	public void stop() {
		// myEXNClient.stop();
		LOGGER.info("Ciao!");
		System.exit(0);// Don't know how to stop properly....
	}

	public static void main(String[] args) throws StreamReadException, DatabindException, IOException {
		NebulOuSMessageBrokerListener tester = new NebulOuSMessageBrokerListener();

		while (true) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}
		}
	}

}
