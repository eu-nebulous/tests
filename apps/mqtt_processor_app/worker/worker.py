import paho.mqtt.client as mqtt
import stomp
import os.path
import logging
import threading
import queue
import time
import datetime
import json
from uuid import uuid4
import sys
import traceback
print("Starting dummy app worker")

shared_stack = queue.Queue()
worker_id = str(uuid4())
# MQTT Broker details
mqtt_broker_address = os.getenv("mqtt_ip")
mqtt_port = int(os.getenv("mqtt_port"))
mqtt_topic = os.getenv("mqtt_subscribe_topic")
mqtt_publish_topic = os.getenv("mqtt_publish_topic")

# STOMP Broker details
report_metrics_to_ems = os.getenv("report_metrics_to_ems")
stomp_broker_address = os.getenv("nebulous_ems_ip")
stomp_port = int(os.getenv("nebulous_ems_port"))
stomp_destination = os.getenv("nebulous_ems_metrics_topic")
stomp_user = os.getenv("nebulous_ems_user")
stomp_pass = os.getenv("nebulous_ems_password")

def map_value(old_value, old_min, old_max, new_min, new_max):
    return ( (old_value - old_min) / (old_max - old_min) ) * (new_max - new_min) + new_min


# MQTT callback function
def on_message(client, userdata, message):
    try:
        payload = message.payload.decode("utf-8")
        print("Recieved MQTT message",payload)
        print("Message added to stack. Current length:",shared_stack.qsize())
        shared_stack.put(payload)
        backpressure = map_value(min(shared_stack.qsize(),10),0,10,0,2)
        print("Backpressure: ",backpressure)
        if backpressure>0:
            time.sleep(backpressure)
    except Exception as e:
        print("Error",e)
        sys.exit(1)

def process_messages():
    while True:
        try:
            # Get message from the shared stack
            payload = shared_stack.get()
            payload = json.loads(payload)
            print("Processing ",payload)        
            print("Proceed to simulate an inference of ",payload["inference_duration"])
            time.sleep(payload["inference_duration"])          
            date_timestamp = datetime.datetime.strptime(payload['job_timestamp'], "%Y-%m-%d %H:%M:%S%z")
            total_job_duration = int((datetime.datetime.now(datetime.timezone.utc) - date_timestamp).total_seconds())
            print(f"total_job_duration: {total_job_duration}")
            json_msg = {
                "metricValue": total_job_duration,
                "level": 1,
                "timestamp": int(datetime.datetime.now().timestamp())
            }

            
            payload["worker_id"] = worker_id
            payload["total_job_duration"] = total_job_duration
            payload["job_completion_timestamp"] = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S%z")
            mqtt_client.publish(mqtt_publish_topic,json.dumps(payload),2)
            

            if "True" == report_metrics_to_ems:
                print("send_metric ",json_msg)
                print(json.dumps(json_msg))
                stomp_client.send(body=json.dumps(json_msg), headers={'type':'textMessage', 'amq-msg-type':'text'}, destination=stomp_destination)
            else:
                print("EMS reporting is disabled.")
        except Exception as e:
            print("Error",e)
            sys.exit(1)

# STOMP connection callback
def on_connect_stomp():
    print("Connected to STOMP broker")

# STOMP error callback
def on_error_stomp():
    print("Error in STOMP connection")

logging.basicConfig(level=logging.DEBUG)
logger = logging.getLogger(__name__)






print("Connecting to MQTT")
# Initialize MQTT client
mqtt_client = mqtt.Client()
mqtt_client.on_message = on_message
mqtt_client.connect(mqtt_broker_address, mqtt_port)
mqtt_client.subscribe(mqtt_topic)
mqtt_client.enable_logger(logger)
publish_thread = threading.Thread(target=process_messages)
publish_thread.daemon = True  # Daemonize the thread so it will exit when the main thread exits
publish_thread.start()
print("Done")

if "True" == report_metrics_to_ems:
    print("Connecting to STOMP")
    try:
        stomp_client = stomp.Connection12(host_and_ports=[(stomp_broker_address, stomp_port)])
        stomp_client.set_listener('', stomp.PrintingListener())
        stomp_client.connect(stomp_user, stomp_pass, wait=True)
        stomp_client.subscribe(stomp_destination,str(uuid4()))
    except Exception as e:
        traceback.print_exc() 
        mqtt_client.publish(mqtt_publish_topic,"Error in STOMP connection",2)
        sys.exit(1)
    print("Done")
print("Start MQTT Loop")
# Start the MQTT client loop
mqtt_client.loop_forever()
print("App ended")