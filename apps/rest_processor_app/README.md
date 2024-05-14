# Rest Processor App

This application consists of a controller that accepts "work" requests and a variable number of workers that process these requests. 

# Controller

The controller is an API implemented with FastAPI, featuring three endpoints:
* The "POST /" endpoint with a URL parameter "t" representing a positive integer value, which adds a new work request to the "pending requests" queue. The component periodically publishes to the EMS the age in seconds of the oldest request waiting in the queue (metric "max_age").
* The "POST /accept" endpoint removes the oldest element from the "pending requests" queue, which ideally will be consumed by the worker, and added to the controller's "completed requests" queue.
* The "GET /" endpoint displays the two queues and their requests in HTML format. The "pending requests" queue is sorted from oldest to youngest, while the "completed requests" queue is sorted from youngest to oldest, with a maximum of one hundred requests stored.

# Worker

The worker consumes requests from the controller's "pending requests" queue via the "POST /accept" endpoint. Upon receiving a request, the worker sleeps for the duration received in the request (in seconds) until the queue is empty and receives a "null" response from the controller.

Once all pending requests have been processed, the worker begins a countdown based on the "max idle time" setting, which is specified by the user through the environment variable "MAX_IDLE_TIME". If no new requests are received within this idle time, the worker gracefully exits.
