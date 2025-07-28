# NebulOuS Test Automation

This project utilizes the **Citrus Framework** to automate **integration testing** of various components within the NebulOuS cloud environment. 
The tests focus on validating the interaction between components through **AMQP messaging queues**, **SAL REST APIs**, **Resource Manager APIs** and utility APIs that support application deployment workflows and general platform operations—ensuring the system behaves as expected.
purpose of this project is to automate the following test cases related to application deployment:


### TC_23 App Deployment (NebulOuS Cloud Providers)
The **TC_23 App** deployment using NebulOuS cloud providers is fully dockerized.  
You can easily build and run the application using Docker.

#### Build the Docker Image

```bash
docker build -t tc23 .
```

#### Environment Variables
The following environment variables must be configured and provided at runtime in order for the test to execute successfully. These variables can be set directly or mounted via an environment file (e.g. using `--env-file` when running the container or pod).

| Variable | Description                                                                                                                                     |
| -------- |-------------------------------------------------------------------------------------------------------------------------------------------------|
| `SAL_API_URL` | URL of the SAL API service (local execution: `http://localhost:8088` cluster deployment: `http://nebulous-sal.nebulous-cd.svc.cluster.local:8080`). |
| `SAL_API_USER` | Username for authenticating with the SAL API.                                                                                                   |
| `SAL_API_PASSWORD` | Password for authenticating with the SAL API.                                                                                                   |
| `NEBULOUS_BROKER_ADDRESS` | Address of the message broker (default: `localhost`, cluster deployment: `nebulous-activemq.nebulous-cd.svc.cluster.local`).                    |
| `NEBULOUS_BROKER_URL` | Full URL of the message broker (default: `amqp://localhost:5672`, cluster deployment: `amqp://nebulous-activemq.nebulous-cd.svc.cluster.local:5672`). |
| `NEBULOUS_BROKER_PORT` | Port of the message broker (default: `5672`)                                                                                                    |
| `NEBULOUS_BROKER_USERNAME` | Username for the message broker authentication.                                                                                                 |
| `NEBULOUS_BROKER_PASSWORD` | Password for the message broker authentication.                                                                                                 |
| `CLOUD_RESOURCES_UUID` | Unique identifier for the cloud resources.                                                                                                      |
| `CLOUD_RESOURCES_TITLE` | Title/name for the cloud resources.                                                                                                             |
| `CLOUD_RESOURCES_PLATFORM` | Platform name or type for the cloud resources.                                                                                                  |
| `CLOUD_RESOURCES_ENABLED` | Flag to enable/disable the cloud resources.                                                                                                     |
| `CLOUD_RESOURCES_REGIONS` | Comma-separated list of regions for cloud resources.                                                                                            |
| `CLOUD_RESOURCES_USE_REGISTERED` | `boolean`, `true` if Cloud is already registered and requires validation for it, `false` to register new Cloud on SAL                           |
| `CLOUD_RESOURCES_SECRET` | Credentials Secret provided for Cloud Registration                                                                                              |

In the _.env_ file, the `CLOUD_RESOURCES_USE_REGISTERED` variable is set to `false` by default. This means a new OpenStack cloud will be registered to SAL, which requires the `CLOUD_RESOURCES_SECRET` to be provided.

If `CLOUD_RESOURCES_USE_REGISTERED` is set to `true`, the following environment variables must be provided for the registered cloud: `CLOUD_RESOURCES_UUID`,
`CLOUD_RESOURCES_TITLE`, `CLOUD_RESOURCES_PLATFORM` and `CLOUD_RESOURCES_REGIONS`.

[//]: # (| `RESOURCE_MANAGER_URL` | URL of the Resource Manager service.                                                                                                                |)

[//]: # (| `RESOURCE_MANAGER_USERNAME` | Username for authenticating with Resource Manager.                                                                                                  |)

[//]: # (| `RESOURCE_MANAGER_PASSWORD` | Password for authenticating with Resource Manager.                                                                                                  |)


## Test Cases

| Test Case ID | Description                                                 | Status      |
|--------------|-------------------------------------------------------------|-------------|
| [TC_21]      | App deployment on manually-managed nodes                    | Implemented  |
| [TC_23]      | App deployment using NebulOuS cloud providers               | Implemented |
| [TC_26]      | NebulOuS scales up and down applications to comply with SLO | In Testing  |
|      | KubeVela, Metric Model, AMPL Validation                     | In Testing |


* _Implemented:
  Automation testing code has been implemented but is under maintenance due to component and intercommunication changes._

* _In Testing:
  Some parts of the test have been completed, and it is currently undergoing testing._

* _Not Started:
  No implementation of the test has been done yet._

