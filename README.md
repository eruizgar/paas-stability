#  Paas-Stability
# Stability tests for PaaS services

## Description
Mavenized scala project to stress Paas services using Gatling (http://gatling.io/).

In order to test the reliability of the Stratio PaaS, we have created a simple mechanism to execute 
kind of stability tests that mimic the behaviour of a number of users interacting with the clusters.

These users should interact with the frameworks installed within the Stratio PaaS and should perform 
common operations, for instance: create a kafka topic and produce some messages, create and remove 
zookeeper znods, run spark jobs...

In order to do so we have created this project which using [Gatling](http://gatling.io/#/) will execute 
a stability scenario against a running Stratio PaaS instance. 

## How it works

Different maven profiles have been created to run the perf tests.

## Running the scenario

To run the scenario you just need to clone this repo and run the following command within the recently
created repo:

```
$ mvn gatling:execute 
```

### Run-just-one-service profile

You might want to run the performance tests just for one of the Services defined. To do so run the following commands:

```sh
$ mvn test -P<SERVICE>
```
Where SERVICE is the Service to run the tests against (see the pom.xml file to find out the different profiles)

### Requirements

If you want to launch Spark tests against a Stratio PaaS instance you need to install sshpass to obtain the connection token.
