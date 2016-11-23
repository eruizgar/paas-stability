package com.stratio.tests.spark

import com.stratio.tests.commons.spark._
import io.gatling.core.Predef._
import io.gatling.core.scenario.Simulation
import io.gatling.http.Predef._

import scala.concurrent.duration._
import scala.sys.process._

class SparkSimulation extends Simulation with Headers {

  val ramp_users = Integer.parseInt(System.getProperty("RAMP_USERS", "4"))
  val test_duration = Integer.parseInt(System.getProperty("TEST_DURATION", "120"))

  val dcos_user = System.getProperty("DCOS_USER", "admin@demo.stratio.com")
  val remote_user = System.getProperty("REMOTE_USER", "root")
  val remote_password = System.getProperty("REMOTE_PASSWORD", "stratio")
  val master_mesos = System.getProperty("MASTER_MESOS", "10.200.0.152")
  val serviceName = System.getProperty("SERVICE_NAME", "spark1")

  val dcos_secret = s"""sshpass -p $remote_password ssh -t -T -o StrictHostKeyChecking=no $remote_user@$master_mesos cat /var/lib/dcos/dcos-oauth/auth-token-secret""" !!
  val token_pre = s"""java  -jar src/test/resources/dcosTokenGenerator-0.1.0-SNAPSHOT-jar-with-dependencies.jar $dcos_secret $dcos_user""" !!
  val token = token_pre.replace("\n", "")

  val create =s"""http://$master_mesos/service/$serviceName/v1/submissions/create"""
  val status =s"""http://$master_mesos/service/$serviceName/v1/submissions/status/"""
  val delete =s"""http://$master_mesos/service/$serviceName/v1/submissions/kill/"""

  val headers = Map(contentType -> contentTypeValue, cookie -> (auth + token))


  val createJob = exec(http("Create job")
    .post(create)
    .headers(headers)
    .body(StringBody(json))
    .check(jsonPath("$.submissionId").saveAs("submissionId")))

  val getStatus = exec(http("Status job").get(status + "${submissionId}")
    .headers(headers).check(jsonPath("$.driverState").saveAs("driverState")))

  val checkStatus =
    exec(
      http("Check job").get(status + "${submissionId}")
    .headers(headers).check(checkIf(session =>
      session.attributes("driverState").asInstanceOf[String] == "FINISHED"){
      jsonPath("$.driverState").is("FINISHED")
    }))


  val scn = scenario("Spark stability")
        .exec(createJob).exec(getStatus).asLongAs(session =>
            session.attributes("driverState").asInstanceOf[String] != "FINISHED"){
        exec(getStatus).pause(5)
          .doIf(session =>
            session.attributes("driverState").asInstanceOf[String] == "FINISHED"){
            exec(checkStatus)
          }
  }


  setUp(scn.inject(rampUsers(ramp_users) over (100 seconds))).maxDuration(test_duration seconds)

}



