# Rest Processor App

This application consists of a controller that accepts "work" requests and a variable number of workers that process these requests. 

# Controller

The controller is an API implemented with FastAPI, featuring three endpoints:
* The "POST /" endpoint with a URL parameter "t" representing a positive integer value, which adds a new work request to the "pending requests" queue. The component periodically publishes to the EMS the age in seconds of the oldest request waiting in the queue (metric "max_age").
* The "POST /accept" endpoint removes the oldest element from the "pending requests" queue, which ideally will be consumed by the worker, and added to the controller's "completed requests" queue.
* The "GET /" endpoint displays the two queues and their requests in HTML format. The "pending requests" queue is sorted from oldest to youngest, while the "completed requests" queue is sorted from youngest to oldest, with a maximum of one hundred requests stored.


# Metrics
If env `report_metrics_to_ems` is set to True, the app controller periodically reports the following metrics to EMS:

- RawMaxMessageAge_SENSOR: The age of the oldest message in the queue.

- NumWorkers_SENSOR: The number of workers that are active. A worker is considered active if it has contacted the controller in the last 5 seconds or after the duration of the last work sent to the worker + 5 seconds 

- NumPendingRequests_SENSOR: The number of requests on the queue.

- AccumulatedSecondsPendingRequests_SENSOR: The accumulated duration of the requests in the pending queue.


# Worker

The worker consumes requests from the controller's "pending requests" queue via the "POST /accept" endpoint. Upon receiving a request, the worker sleeps for the duration received in the request (in seconds) until the queue is empty and receives a "null" response from the controller.

Once all pending requests have been processed, the worker begins a countdown based on the "max idle time" setting, which is specified by the user through the environment variable "MAX_IDLE_TIME". If no new requests are received within this idle time, the worker gracefully exits. If max_idle_time is seto to -1 worker will never terminate.

# Usage
In order to have tasks create POST request to your api address, with parameter t=\<task time in seconds>.

For example: 
```
curl -d post localhost:8000/?t=10
```

## Running locally

Start api:
```
$ fastapi run controller.py
```

Start worker, you can run as much as you want in different terminals:
```
$ python worker.py 
```

## Running in docker

Start all components with:

```
$ docker compose up -d
```

If you want more workers increase the deploy "replicas" value in the worker service

## Running in kubernetes

Start all components with:

```
$ kubectl apply -f ems-vela.yaml
```

If you want more workers increase the trait cpuscaler "min" value in the rest-processor-client service


## Pushing the images to docker registry

docker build . -f Dockerfile-worker -t dummy-rest-app-worker
docker image tag dummy-rest-app-worker rsprat/dummy-rest-app-worker:v1
docker push rsprat/dummy-rest-app-worker:v1

docker build . -f Dockerfile-controller -t dummy-rest-app-controller
docker image tag dummy-rest-app-controller rsprat/dummy-rest-app-controller:v1
docker push rsprat/dummy-rest-app-controller:v1