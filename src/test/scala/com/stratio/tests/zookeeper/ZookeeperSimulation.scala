package com.stratio.tests.zookeeper

import com.stratio.tests.commons.TemporizedOperation
import com.stratio.tests.commons.zookeeper.ZnodeHandler
import io.gatling.core.Predef._
import io.gatling.core.action.Action
import io.gatling.core.action.builder.ActionBuilder
import io.gatling.core.feeder._
import io.gatling.core.scenario.Simulation
import io.gatling.core.structure.{ChainBuilder, ScenarioContext}
import org.apache.curator.framework.CuratorFrameworkFactory
import org.apache.curator.framework.imps.CuratorFrameworkState
import org.apache.curator.retry.ExponentialBackoffRetry
import org.apache.zookeeper.data.Stat

import scala.concurrent.duration._
import scala.concurrent.forkjoin.ThreadLocalRandom

class ZookeeperSimulation extends Simulation with ZnodeHandler {

  val runDuration = Integer.parseInt(System.getProperty("runD", "1"))
  val servers = System.getProperty("SERVERS", "zookeeper-sec.marathon.mesos:2181")
  val principal = System.getProperty("PRINCIPAL", "zookeeper/zookeeper-plugin-agent@DEMO.STRATIO.COM")
  val retryPolicy = new ExponentialBackoffRetry(1000, 3)
  val curatorZkClient = CuratorFrameworkFactory.builder()
    .connectString(servers)
    .retryPolicy(retryPolicy).build()
  System.setProperty("java.security.auth.login.config", System.getProperty("JAAS","/opt/zookeeper/conf/jaas.conf"))
  System.setProperty("java.security.krb5.conf", System.getProperty("KRB5","/etc/krb5.conf"))

  def getInstance(): ZkClient = {
    if (curatorZkClient.getState != CuratorFrameworkState.STARTED) {
      curatorZkClient.start
      curatorZkClient.blockUntilConnected()
    }
    ZkClient(curatorZkClient, principal)
  }

  val zkClient: ZkClient = getInstance()
  val curatorZookeeperClient = zkClient.instance
  curatorZookeeperClient.getZookeeperClient.blockUntilConnectedOrTimedOut
  val recordsByGroup: Map[String, IndexedSeq[Record[String]]] =
    csv("src/test/resources/feeders/znodes.csv").records.groupBy{ record => record("group") }
  val znodesByGroup: Map[String, IndexedSeq[String]] =
    recordsByGroup.mapValues{ records => records.map {record => record("znode")} }
  val genericErrorMessage = "Error performing the action: "

  def getErrorMessage(actionName: String) = s"""$genericErrorMessage $actionName"""

  val createZnodes = new ActionBuilder {
    override def build(ctx: ScenarioContext, next: Action): Action = {
      val statusCheck = { stat:Stat => stat == null }
      val createZnode = { znode:String => curatorZookeeperClient.create().forPath(znode) }
      val createZnodeOperation = { session: Session =>
        znodeOperationBuilder(session, statusCheck, curatorZookeeperClient, createZnode) 
      }
      val actionName = "Create znode"
      val errorMessage = getErrorMessage(actionName)
      new TemporizedOperation(next, ctx, actionName, createZnodeOperation, Some(errorMessage))
    }
  }

  val setData = new ActionBuilder {
    override def build(ctx: ScenarioContext, next: Action): Action = {
      val statusCheck = { stat:Stat => stat != null }
      val setZnode = { znode:String => curatorZookeeperClient.setData().forPath(znode, "test".getBytes()) }
      val setZnodeOperation = { session: Session =>
        znodeOperationBuilder(session, statusCheck, curatorZookeeperClient, setZnode)
      }

      val actionName = "Set data"
      val errorMessage = getErrorMessage(actionName)
      new TemporizedOperation(next, ctx, actionName, setZnodeOperation, Some(errorMessage))
    }
  }

  val getData = new ActionBuilder {
    override def build(ctx: ScenarioContext, next: Action): Action = {
      val getData = {znode:String => curatorZookeeperClient.getData().forPath(znode) }
      val statusCheck = {stat:Stat => stat != null }
      val getZnodeOperation = { session: Session =>
        znodeOperationBuilder(session, statusCheck, curatorZookeeperClient, getData)
      }
      val actionName = "Get data"
      val errorMessage = getErrorMessage(actionName)
      new TemporizedOperation(next, ctx, actionName, getZnodeOperation, Some(errorMessage))
    }
  }

  val removeZnodes = new ActionBuilder {
    override def build(ctx: ScenarioContext, next: Action): Action = {
      val removeZnode = { znode:String => curatorZookeeperClient.delete().forPath(znode) }
      val statusCheck = { stat:Stat => stat != null }
      val getZnodeOperation = { session: Session =>
        znodeOperationBuilder(session, statusCheck, curatorZookeeperClient, removeZnode)
      }
      val actionName = "Remove znode"
      val errorMessage = getErrorMessage(actionName)
      new TemporizedOperation(next, ctx, actionName, getZnodeOperation, Some(errorMessage))
    }
  }

  val createZnodeSetDataGetDataAndRemoveZnodes = ChainBuilder(List(createZnodes, setData, getData, removeZnodes))
  val znodesCreationAndRemovingScenario = scenario("Create, set data and get data and remove znode.")
    .feed(csv("src/test/resources/feeders/users.csv"))
    .exec { session =>
      session("group").validate[String].map { group =>
        val znodes = znodesByGroup(group)
        val selectedZnode = znodes(ThreadLocalRandom.current.nextInt(znodes.length))
        session.set("znode", selectedZnode)
      }
    }.forever(pace(1 seconds, 5 seconds).exec(createZnodeSetDataGetDataAndRemoveZnodes))

  setUp(znodesCreationAndRemovingScenario.inject(rampUsers(3) over(15 seconds))).maxDuration(runDuration minutes)
}