package com.stratio.tests.commons.spark

trait Headers {
  val contentTypeValue: String = "application/json; charset=utf-8"
  val contentType = "Content-Type"
  val cookie = "Cookie"
  val auth = "dcos-acs-auth-cookie="
  val json =
    """{
    "action": "CreateSubmissionRequest",
    "appArgs": ["", "10"],
    "appResource": "https://s3-eu-west-1.amazonaws.com/enablers/spark-examples-1.6.1-hadoop2.4.0.jar",
    "clientSparkVersion": "1.6.2",
    "environmentVariables": {
      "SPARK_SCALA_VERSION": "2.10",
      "SPARK_JAVA_OPTS": "-Dspark.mesos.executor.docker.image=qa.stratio.com/stratio/stratio-spark:1.0.1-1.6.2",
      "SPARK_HOME": "/root/.dcos/spark/dist/spark-1.6.2"
    },
    "mainClass": "org.apache.spark.examples.JavaSparkPi",
    "sparkProperties": {
      "spark.jars": "https://s3-eu-west-1.amazonaws.com/enablers/spark-examples-1.6.1-hadoop2.4.0.jar",
      "spark.ssl.noCertVerification": "true",
      "spark.driver.supervise": "false",
      "spark.app.name": "org.apache.spark.examples.JavaSparkPi",
      "spark.mesos.executor.docker.image": "qa.stratio.com/stratio/stratio-spark:1.0.1-1.6.2",
      "spark.submit.deployMode": "cluster"
    }
  }"""
}