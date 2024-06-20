package eut.nebulouscloud.tests.mqttapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class MQTTProcessorAPPWorkloadSimulator {
	MQTTProcessorAppMessageBrokerInterface appInterface;
	String mqttAppInputTopic;
	static Logger LOGGER = LoggerFactory.getLogger(MQTTProcessorAPPWorkloadSimulator.class);
	protected ObjectMapper om = new ObjectMapper();

	private Timer mTimer;

	public MQTTProcessorAPPWorkloadSimulator(MQTTProcessorAppMessageBrokerInterface appInterface,
			String mqttAppInputTopic) {
		super();
		this.appInterface = appInterface;
		this.mqttAppInputTopic = mqttAppInputTopic;
	}

	private String sendJobRequestToAPP(int durationSeconds) throws Exception {
		Map<String, Object> inferenceRequest = new HashMap<String, Object>();
		inferenceRequest.put("timestamp", new SimpleDateFormat("YYYY-MM-dd HH:mm:ssZ").format(new Date()));
		inferenceRequest.put("job_timestamp", inferenceRequest.get("timestamp"));
		inferenceRequest.put("inference_duration", durationSeconds);
		String jobId = UUID.randomUUID().toString();
		inferenceRequest.put("job_id", jobId);
		String payload = om.writeValueAsString(inferenceRequest);
		// Send the request
		appInterface.publish(mqttAppInputTopic, payload);
		LOGGER.info(String.format("Sending job request to %s: %s",mqttAppInputTopic,payload));
		return jobId;
	}

	public void set(int frequencySeconds, int jobDuration) {
		if(mTimer != null)
		{
			mTimer.cancel();			 
		}
		mTimer = new Timer();
		mTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					sendJobRequestToAPP(jobDuration);
				} catch (Exception ex) {
					LOGGER.error("",ex);
				}
			}
		}, 0, frequencySeconds*1000);

	}

	public void stop() {
		if(mTimer != null)
		{
			mTimer.cancel();			 
		}
	}

}
