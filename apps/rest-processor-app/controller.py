import asyncio
from datetime import date
import datetime
import json
import os
import queue
import sys
import threading
from time import sleep
import time
import traceback
from uuid import uuid4
from fastapi import FastAPI
import stomp
from fastapi.responses import HTMLResponse
import logging

logger = logging.getLogger(__name__)
logger.setLevel(logging.DEBUG)
handler = logging.StreamHandler(sys.stdout)
handler.setLevel(logging.DEBUG)
formatter = logging.Formatter('%(asctime)s - %(name)s - %(levelname)s - %(message)s')
handler.setFormatter(formatter)
logger.addHandler(handler)


report_metrics_to_ems = os.getenv("report_metrics_to_ems","False")
stomp_broker_address = os.getenv("nebulous_ems_ip")
stomp_port = int(os.getenv("nebulous_ems_port",0))
stomp_user = os.getenv("nebulous_ems_user")
stomp_pass = os.getenv("nebulous_ems_password")
stomp_client = None

pending_requests =  queue.Queue() 
completed_requests = queue.Queue(100) 
app = FastAPI()

class CompletedRequest():
    def __init__(self, t, reception_time, completion_time, worker_id):
        self.t = t  
        self.reception_time = reception_time  
        self.completion_time = completion_time
        self.worker_id = worker_id 
        self.total_time =   round((completion_time - reception_time),4) 

    def __str__(self):
        task_dict = {
            't': self.t,
            'reception_time': self.reception_time,
            'completion_time': self.completion_time,
            'worker_id': self.worker_id,
            'total_time' : self.total_time
        }
        return json.dumps(task_dict)
    
class PendingRequest():
    def __init__(self, t, reception_time):
        self.t = t  
        self.reception_time = reception_time  
        self.age = 0

    def completedBy(self,worker_uid):
        return CompletedRequest(self.t,self.reception_time,time.time(),worker_uid)
    
    def __str__(self):
        task_dict = {
            't': self.t,
            'reception_time': self.reception_time,
            'age':self.age
        }
        return json.dumps(task_dict)
    
class WorkerCountTracker:
    def __init__(self):
        self.workers_map = {}
        self.lock = threading.Lock()
        self.expiry_time = 10  # IDs older than 10 seconds will be removed
        
    def cleanup_expired_ids(self):
        with self.lock:
            current_time = time.time()
            expired_workers = [id for id, last_time in self.workers_map.items() if current_time - last_time > self.expiry_time]
            for id in expired_workers:
                del self.workers_map[id]

    def update_worker(self, id,expiry_time):
        with self.lock:
            logger.debug("Set worker {} expiry to {}".format(id,expiry_time))
            self.workers_map[id] = expiry_time

    def get_workers(self):
        with self.lock:
            return list(self.workers_map.keys())
workerTracker = WorkerCountTracker()
#time.time()
def report_metrics():

    #
    # Report on RawMaxMessageAge
    #
    max_age = -1
    try:
        max_age = time.time() - list(pending_requests.queue)[0].reception_time
    except:
        None
    json_msg = {
        "metricValue": max_age,
        "level": 1,
        "timestamp": int(datetime.datetime.now().timestamp())
    }
    logger.info("RawMaxMessageAge: "+json.dumps(json_msg))
    stomp_client.send(body=json.dumps(json_msg), headers={'type':'textMessage', 'amq-msg-type':'text'}, destination="/topic/RawMaxMessageAge_SENSOR")
    
    #
    # Report on NumWorkers
    #
    workerTracker.cleanup_expired_ids()
    num_workers = len(workerTracker.get_workers())
    json_msg = {
        "metricValue": num_workers,
        "level": 1,
        "timestamp": int(datetime.datetime.now().timestamp())
    }
    logger.info("NumWorkers_SENSOR: "+json.dumps(json_msg))
    stomp_client.send(body=json.dumps(json_msg), headers={'type':'textMessage', 'amq-msg-type':'text'}, destination="/topic/NumWorkers_SENSOR")
    
    #
    # Report on NumPendingRequests
    #   
    num_requests = len(list(pending_requests.queue))
    json_msg = {
        "metricValue": num_requests,
        "level": 1,
        "timestamp": int(datetime.datetime.now().timestamp())
    }
    logger.info("NumPendingRequests_SENSOR: "+json.dumps(json_msg))
    stomp_client.send(body=json.dumps(json_msg), headers={'type':'textMessage', 'amq-msg-type':'text'}, destination="/topic/NumPendingRequests_SENSOR")    

    #
    # Report on AccumulatedSecondsPendingRequests
    #      
    total_queue_length = sum(list([req.t for req in list(pending_requests.queue)]))
    json_msg = {
        "metricValue": total_queue_length,
        "level": 1,
        "timestamp": int(datetime.datetime.now().timestamp())
    }
    logger.info("AccumulatedSecondsPendingRequests_SENSOR: "+json.dumps(json_msg))  
    stomp_client.send(body=json.dumps(json_msg), headers={'type':'textMessage', 'amq-msg-type':'text'}, destination="/topic/AccumulatedSecondsPendingRequests_SENSOR")    
        
    
def report_metrics_process():
    logger.info("Start report_metrics_process")
    while True:
        try:
            report_metrics()
            sleep(10)
        except Exception as e:
            traceback.print_exc()
            logger.error("report_metrics_process error",e)
            sleep(10)
            

def update_age():
    logger.info("Pending requests age clock started")
    while True:
        for i in range(pending_requests.qsize()):
            pending_requests.queue[i].age = round((time.time() - pending_requests.queue[i].reception_time),4)
        sleep(3)
        


@app.on_event("startup")
async def startup_event():
    global stomp_client
    logger.info("Starting controller's API")
    thread1 = threading.Thread(target=update_age)
    thread1.daemon = True  
    thread1.start()
    if "True" == report_metrics_to_ems:
        logger.info("Connecting to STOMP")
        try:
            stomp_client = stomp.Connection12(host_and_ports=[(stomp_broker_address, stomp_port)])
            stomp_client.set_listener('', stomp.PrintingListener())
            stomp_client.connect(stomp_user, stomp_pass, wait=True)
            thread = threading.Thread(target=report_metrics_process)
            thread.daemon = True  
            thread.start()
            logger.info("Connection sucessfully")
        except Exception as e:
            traceback.print_exc() 
            sys.exit(1)
    else:
        logger.info("EMS reporting is disabled")
    logger.info("Done")


@app.post("/")
async def root(t : int):
    
    pending_request = PendingRequest(t,time.time())
    pending_requests.put(pending_request)
    logger.debug("New request: "+str(pending_request))
    return {"message": "Request recieved, "+str(pending_request)}


@app.get("/", response_class=HTMLResponse)
async def root():
    pending_requests_str = list(pending_requests.queue)
    completed_requests_str = list(completed_requests.queue)
    pending_requests_html = "<tr>"
    
    for req in pending_requests_str:
        for field in str(req).split(","):
            field_stripped = field.split(":")[1].strip("} ")   
            if(field.split(":")[0].find("reception_time")!= -1):           
                field_stripped=datetime.datetime.fromtimestamp(float(field_stripped)).isoformat()
            pending_requests_html += f"<td>{field_stripped}</td>\n" 
        pending_requests_html += "</tr>\n"
    

    completed_requests_html = ""
    for req in completed_requests_str:
        completed_request_html = "<tr>"
        for field in str(req).split(","):
            field_stripped = field.split(":")[1].strip("} ")
            if(field.split(":")[0].find("reception_time")!= -1 or field.split(":")[0].find("completion_time")!= -1):
                field_stripped=datetime.datetime.fromtimestamp(float(field_stripped)).isoformat()
            completed_request_html += f"<td>{field_stripped}</td>\n"
        completed_request_html += "</tr>\n"
        completed_requests_html = completed_request_html+completed_requests_html

    workers_table_html = ""
    workers_count = 0
    for id, last_time in workerTracker.workers_map.items():
        workers_count+=1
        worker_table_row ="<tr>"
        worker_table_row += "<td>"+id+"</td>"
        worker_table_row += "<td>"+datetime.datetime.fromtimestamp(float(last_time)).isoformat()+"</td>"
        worker_table_row +="</tr>"
        workers_table_html += worker_table_row

    html_response = f"""
    <html>
        <head>
        <meta http-equiv="refresh" content="2">
        <title>Requests</title>
            <style>
    

                td{{
                border: 1px solid #ddd;
                padding: 8px;
                }}

                tr:nth-child(even){{
                    background-color: #f2f2f2;}}

                tr:hover {{
                    background-color: #ddd;}}

                th {{
                    padding-top: 12px;
                    padding-bottom: 12px;
                    text-align: left;
                    background-color: #04AA6D;
                    color: white;
                }}
           
                #pending {{
                    float: left;
                    width: 30%;
                    font-family: Arial, Helvetica, sans-serif;

                    
                }}
                #completed {{
                    float: left;
                    width: 70%;
                    font-family: Arial, Helvetica, sans-serif;
                }}
            </style>
        </head>
        <body>
            <div id="pending">
            <table>
                <h1>Pending Requests:</h1>
                <tr>
                    <th>Workload duration</th>
                    <th>Reception Time</th>
                    <th>Age</th>
                </tr>
                {pending_requests_html}
            </table>
            </div>
            <div id="completed">
            <table >
                <h1>Completed Requests:</h1>
                <tr>
                    <th>Workload duration</th>
                    <th>Reception Time</th>
                    <th>Processing start time</th>
                    <th>Worker id</th>
                    <th>Total Time</th>
                </tr>
                {completed_requests_html}
            </table>
            </div>
            
            <div >
            <table >
                <h1>Workers: {workers_count}</h1>
                <tr>
                    <th>Worker id</th>
                    <th>Timeout</th>
                </tr>
                {workers_table_html}
            </table>
            </div>
            
        </body>
    </html>
    """
    return html_response

@app.post("/accept")
async def accept(worker_id):
    lock = threading.Lock()
    lock.acquire()
    try:
        if(pending_requests.qsize()==0):
            workerTracker.update_worker(worker_id,time.time()+workerTracker.expiry_time)
            return {"message": None}
        pending_request = pending_requests.get(0)        
        workerTracker.update_worker(worker_id,pending_request.t+time.time()+workerTracker.expiry_time)        
        logger.debug("Selected request: "+ str(pending_request))
        completed_request = pending_request.completedBy(worker_id)
        try:
            completed_requests.put_nowait(completed_request)
        except:
            completed_requests.get()
            completed_requests.put_nowait(completed_request)
        logger.debug("Completing request: "+ str(completed_request))
        return {"message":  pending_request.t}
    finally:
        # Release the lock after accessing the queue
        
        lock.release()





