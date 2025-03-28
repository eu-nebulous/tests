# NebulOuS Test Automation

This project utilizes the **Citrus Framework** to automate **integration testing** of various components within the NebulOuS cloud environment. 
The tests focus on validating the interaction between components through **AMQP messaging queues**, **SAL REST APIs**, **Resource Manager APIs** and utility APIs that support application deployment workflows and general platform operations—ensuring the system behaves as expected.
purpose of this project is to automate the following test cases related to application deployment:

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

