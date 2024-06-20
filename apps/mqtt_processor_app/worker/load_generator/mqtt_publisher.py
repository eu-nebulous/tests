import logging
import random
import time
from uuid import uuid4

import paho.mqtt.client as mqtt


class MqttClient:

    def __init__(self,camera_id, ip, port, username, password, topic, id=""):
        super().__init__()
        self.camera_id = camera_id
        self.ip = ip
        self.port = int(port)
        self.username = username
        self.password = password
        self.topic = topic
        self.construct_client(id=id)

    def construct_client(self, id=""):
        def on_connect(client, userdata, flags, rc):
            logging.info(f"Connected with result code {rc}")

        def on_message(client, userdata, msg):
            pass

        self.client = mqtt.Client(f"CameraID:{self.camera_id}{str(random.random())}")
        self.client.on_connect = on_connect
        self.client.on_message = on_message
        if all(map(lambda x: x is not None, [self.username, self.password])):
            self.client.username_pw_set(self.username, self.password)
        else:
            logging.debug("Username and password not set in MQTT Client")
        try:
            self.client.connect(self.ip, self.port)
        except Exception as ex:
            print("error")
            print(ex)
            print(ex.__traceback__)
        logging.debug("Trying to connect MQTT")
        self.client.loop_start()
        time.sleep(2)
        while not self.client.is_connected():
            logging.warning("NOT CONNECTED YET")
            time.sleep(2)
            pass
        logging.debug("MQTT CONNECTED")

    def publish(self, payload):
        self.client.publish(self.topic, payload)