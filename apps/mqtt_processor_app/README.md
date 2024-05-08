Source code of the mqtt processor app used for testing.

On startup, the application connects to the configured MQTT broker and waits for messages on the topic APP_MQTT_INPUT_TOPIC. When a well structured message on said topic, the application simulates some work and sends a message to APP_MQTT_OUTPUT_TOPIC.

The structure of the input message is:
- job_id: An unique UUID assigned to the job
- timestamp: Timestamp of the request with the format YYYY-MM-dd HH:mm:ssZ
- job_timestamp: Same as timestamp
- inference_duration: Time in seconds that the worker processing this job will sleep to simulate a time consuming inference process.

The worker needs the following environment variables to work:
- mqtt_ip: The IP/host of the MQTT broker 
- mqtt_port: The port of the MQTT broker
- mqtt_subscribe_topic: The topic to subscribe to and recieve requests
- mqtt_publish_topic: The topic to connect to and publish results

- report_metrics_to_ems: Flag to indicate if metrics should be published to EMS
- nebulous_ems_ip: EMS IP
- nebulous_ems_port: EMS port
- nebulous_ems_user: EMS user
- nebulous_ems_password: EMS password
- nebulous_ems_metrics_topic: EMS topic to use to report metrics

 