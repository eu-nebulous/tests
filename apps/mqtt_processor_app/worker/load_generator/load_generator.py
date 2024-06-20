import datetime
import json
import sys
import uuid
from dataclasses import dataclass
import yaml
from dotenv import load_dotenv
import os
import time
import logging
import io
from mqtt_publisher import MqttClient

load_dotenv()


@dataclass
class Config:
    mqtt_ip: str
    mqtt_port: int
    mqtt_topic: str
    interval: float
    inference_duration: float
    
def main(config):
    mqtt_client = MqttClient("", config.mqtt_ip, config.mqtt_port, os.getenv("MQTT_USERNAME"),
                             os.getenv("MQTT_PASSWORD"), config.mqtt_topic)
    
    while True:         
        payload = {}                
        payload['timestamp'] = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S%z")
        payload['job_timestamp'] = payload['timestamp']
        payload['inference_duration'] = config.inference_duration
        print("Publishing ",payload)
        mqtt_client.publish( json.dumps(payload))        
        time.sleep(config.interval)


if __name__ == '__main__':
    config_dict = {        
        "mqtt_ip": "broker.emqx.io",
        "mqtt_port": 1883,
        "mqtt_topic": "neb/test/input",
        "interval": 3,
        "inference_duration": 10,
    }
    if len(sys.argv) != 1:
        with open(sys.argv[1], 'r') as config_file:
            print(sys.argv[1])
            config_dict = yaml.safe_load(config_file)
    config: Config = Config(**config_dict)    
    main(config)