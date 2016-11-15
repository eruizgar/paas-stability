package com.stratio.tests.spark

import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.sys.process._

class SparkSimulation extends Simulation {


  val scn = scenario("Spark stability")
    .exec(
      SparkSimulation.createJob).exitHereIfFailed.pause(50)
    .exec(
      SparkSimulation.getStatus
  ).exitHereIfFailed

  setUp(scn.inject(rampUsers(2) over (120 seconds))).maxDuration(120 seconds)
}

object SparkSimulation extends Headers {

  val dcos_user = System.getProperty("DCOS_USER", "admin@demo.stratio.com")
  val remote_user = System.getProperty("REMOTE_USER", "root")
  val remote_password = System.getProperty("REMOTE_PASSWORD", "stratio")
  val master_mesos = System.getProperty("MASTER_MESOS", "10.200.0.152")
  val serviceName = System.getProperty("SERVICE_NAME", "spark")

  val dcos_secret = s"""sshpass -p $remote_password ssh -t -T -o StrictHostKeyChecking=no $remote_user@$master_mesos cat /var/lib/dcos/dcos-oauth/auth-token-secret""" !!
  val token_pre = s"""java  -jar src/test/resources/dcosTokenGenerator-0.1.0-SNAPSHOT-jar-with-dependencies.jar $dcos_secret $dcos_user""" !!
  val token = token_pre.replace("\n", "")

  val create =s"""http://$master_mesos/service/$serviceName/v1/submissions/create"""
  val status =s"""http://$master_mesos/service/$serviceName/v1/submissions/status/"""
  val delete =s"""http://$master_mesos/service/$serviceName/v1/submissions/delete"""

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

  val headers = Map(contentType -> contentTypeValue, cookie -> (auth + token))


  def createJob = {
    http("Create job")
      .post(create)
      .headers(headers)
      .body(StringBody(json))
      .check(jsonPath("$.submissionId").saveAs("submissionId"))

  }
  def getStatus = {
    http("Status job").get(status + "${submissionId}")
      .headers(headers)
      .check(jsonPath("$.driverState").saveAs("driverState"))
      .check(jsonPath("$.driverState").is("FINISHED"))
  }
}

trait Headers {
  val contentTypeValue: String = "application/json; charset=utf-8"
  val contentType = "Content-Type"
  val cookie = "Cookie"
  val auth="dcos-acs-auth-cookie="
}


