from datetime import date
from time import sleep
import time
import math 
from uuid import uuid4
import multiprocessing
import requests
import os
import sys
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



def generate_cpu_load(cpu_idx,interval,utilization):
    "Generate a utilization % for a duration of interval seconds"
    print("start generate_cpu_load cpu_idx:",cpu_idx," interval:",interval," utilization:",utilization)
    start_time = time.time()
    for i in range(0,int(interval)):        
        while time.time()-start_time < utilization/100.0:
            a = math.sqrt(64*64*64*64*64)        
        time.sleep(1-utilization/100.0)
        start_time += 1
    print("done generate_cpu_load cpu_idx:",cpu_idx)
last_job_time = time.time() 
while True:
    # Example usage
    result = consume_api()
    if result is not None:    
        print("Got a new job. Make CPUs work at 70% for :",result," seconds")
        print("No of cpu:", multiprocessing.cpu_count())        
        processes = []
        for cpu_idx in range (multiprocessing.cpu_count()):
            p = multiprocessing.Process(target =generate_cpu_load,args=(cpu_idx,result,80))
            p.start()
            processes.append(p)
        for process in processes:
            process.join()
        last_job_time -= time.time() 
    else:
        if max_idle_time > -1 and time.time() > last_job_time+max_idle_time:
            exit
        sleep(1)



