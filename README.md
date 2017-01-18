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

If you want to launch Zookeeper tests against a secured zookeeper you have to point JAAS and KRB5 properties to a file correctly configured.
In jaas.conf you have to configure keytab property with a valid keytab for your zookeeper secured.

Jaas.conf example:
```sh
Client {
  com.sun.security.auth.module.Krb5LoginModule required
  useKeyTab=true
  storeKey=true
  useTicketCache=false
  keyTab="/path/to/zkClient.keytab"
  principal="your-principal";
};
```
Krb5.conf example
```sh
[libdefaults]
default_realm = DEMO.STRATIO.COM
dns_lookup_realm = false
[realms]
DEMO.STRATIO.COM = {
  kdc = idp.integration.labs.stratio.com
  admin_server = idp.integration.labs.stratio.com
  default_domain = demo.stratio.com
}
[domain_realm]
.demo.stratio.com = DEMO.STRATIO.COM
demo.stratio.com = DEMO.STRATIO.COM
```