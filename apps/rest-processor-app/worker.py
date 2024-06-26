from datetime import date
from time import sleep
import time
from uuid import uuid4
import requests
import os
os.environ["MAX_IDLE_TIME"] = "10"
max_idle_time = int(os.getenv("MAX_IDLE_TIME"))
api_address = os.getenv("API_ADDRESS")
worker_id = str(uuid4())
def consume_api():
    url = api_address+"/accept"
    try:
        print("Polling master with worker_id: ",worker_id)
        params = {'worker_id':worker_id } 
        response = requests.post(url,params=params) #ENVIAR UID
        if response.status_code == 200:
            # Assuming the response contains an integer
            result = response.json()['message']
            if (isinstance(result, int) or result==None):
                return result
            else:
                print("Error: Unexpected response format. Expected an integer or null.")
        else:
            print("Error: Failed to fetch data. Status code:", response.status_code)
    except requests.RequestException as e:
        print("Error: Failed to make request:", e)

last_job_time = time.time() 
while True:
    # Example usage
    result = consume_api()
    if result is not None:
        print("Got a new job. Wait:",result)
        sleep(result)
        last_job_time -= time.time() 
    else:
        if max_idle_time > -1 and time.time() > last_job_time+max_idle_time:
            exit
        sleep(1)



