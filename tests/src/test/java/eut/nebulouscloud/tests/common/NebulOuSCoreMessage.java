package eut.nebulouscloud.tests.common;

import java.util.Date;
import java.util.Map;

/**
 * An object that represents a message sent through the NebulOuS message broker. 
 * The prefix "topic://" is removed from the topic value (if exists).
 */
public class NebulOuSCoreMessage {
	public Date receptionDate;
	public String topic;
	public Map<String,Object> payload;
	public String applicationId;
	public String correlationId;
	public NebulOuSCoreMessage(Date receptionDate, String topic, Map<String, Object> payload, String applicationId,
			String correlationId) {
		super();
		this.receptionDate = receptionDate;
		this.topic = topic!=null?topic.replaceFirst("topic://", ""):null;
		this.payload = payload;
		this.applicationId = applicationId;
		this.correlationId = correlationId;
	}
	
	
	

}
