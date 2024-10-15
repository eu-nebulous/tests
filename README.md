# NebulOuS Test Automation

This project utilizes the **Citrus Framework** to automate **integration testing** of various components within the NebulOuS cloud environment. 
The tests focus on validating the interaction between components through **AMQP messaging queues** and **SAL REST APIs**, ensuring that the system behaves as expected.

The purpose of this project is to automate the following test cases related to application deployment:

## Test Cases

| Test Case ID | Description                                                                 | Status      |
|--------------|-----------------------------------------------------------------------------|-------------|
| [TC_17]      | NebulOuS agent recovers after failure with communication with NebulOuS core | Not Started |
| [TC_18]      | NebulOuS agent recovers after some of its component fails                   | Not Started |
| [TC_21]      | App deployment on manually-managed nodes                                    | In Testing  |
| [TC_22]      | App deployment on manually-managed node fails due to lack of resources      | In Testing  |
| [TC_23]      | App deployment using NebulOuS cloud providers                               | Implemented |
| [TC_24]      | App deployment on NebulOuS-managed node fails due to lack of resources      | Implemented |
| [TC_25]      | NebulOuS reacts to node failure                                             | Implemented |
| [TC_26]      | NebulOuS scales up and down applications to comply with SLO                 | In Testing  |

* _Implemented:
  Automation testing code has been implemented but is under maintenance due to component and intercommunication changes._

* _In Testing:
  Some parts of the test have been completed, and it is currently undergoing testing._

* _Not Started:
  No implementation of the test has been done yet._

