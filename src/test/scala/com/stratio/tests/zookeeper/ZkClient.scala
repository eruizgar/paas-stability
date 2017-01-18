package com.stratio.tests.zookeeper

import org.apache.curator.framework.CuratorFramework

case class ZkClient(instance: CuratorFramework, principal: String)
