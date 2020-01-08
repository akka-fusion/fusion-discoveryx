package fusion.discoveryx.functest

import akka.remote.testconductor.RoleName
import akka.remote.testkit.{ MultiNodeConfig, STMultiNodeSpec, SchudulerXMultiNodeSpec }
import com.typesafe.config.ConfigFactory
import fusion.discoveryx.client.DefaultNamingClient
import fusion.discoveryx.common.Constants
import fusion.discoveryx.model.{ InstanceQuery, InstanceRegister, ServerStatusQuery }
import fusion.discoveryx.server.protocol.{ Logined, Namespace }
import fusion.discoveryx.server.{ DiscoveryX, DiscoveryXServer }
import helloscala.common.IntStatus

object DiscoveryXMultiTestConfig extends MultiNodeConfig {
  val servers: Vector[RoleName] = (0 until 1).map(i => role(s"server$i")).toVector

  val clients: Vector[RoleName] = (0 until 3).map(i => role(s"client$i")).toVector

  private def makeSeedNodes(): String = {
    servers.indices.map(i => "127.0.0.1:" + (49001 + i)).mkString("[\"", "\",\"", "\"]")
  }

  // this configuration will be used for all nodes
  // note that no fixed hostname names and ports are used
  commonConfig(ConfigFactory.parseString(s"""
    akka.loglevel = "DEBUG"
    akka.actor.provider = cluster
    discoveryx.name = discoveryx
    """).withFallback(ConfigFactory.load()))

  for ((node, idx) <- servers.zipWithIndex) {
    nodeConfig(node)(ConfigFactory.parseString(s"""fusion.http.default.server.port = ${48000 + idx}
      |discoveryx {
      |  akka.remote.artery.canonical.port = ${49001 + idx}
      |  akka.cluster.roles = [${Constants.MANAGEMENT}, ${Constants.CONFIG}, ${Constants.NAMING}]
      |  akka.cluster.seed-nodes = ${makeSeedNodes()}
      |}""".stripMargin))
  }

  for ((node, idx) <- clients.zipWithIndex) {
    nodeConfig(node)(ConfigFactory.parseString(s"""discoveryx {
      |  akka.cluster.roles = []
      |  akka.remote.artery.canonical.port = ${49101 + idx}
      |  akka.grpc.client {
      |    "*" {
      |      use-tls = false
      |      host = "127.0.0.1"
      |      port = ${48000 + idx}
      |    }
      |    "fusion.discoveryx.grpc.ConfigService" {
      |    }
      |    "fusion.discoveryx.grpc.NamingService" {
      |    }
      |  }
      |}""".stripMargin))
  }
}

abstract class DiscoveryXMultiTest
    extends SchudulerXMultiNodeSpec(DiscoveryXMultiTestConfig, config => DiscoveryX.fromOriginalConfig(config))
    with STMultiNodeSpec {
  import DiscoveryXMultiTestConfig._

  private val serviceName = "akka"
  private val groupName = "default"
  private var namespace: Namespace = _
  private var logined: Logined = _

  "FusionDiscovery" must {
    "wait for all nodes to enter a barrier" in {
      enterBarrier("startup")
    }

    "server startup" in {
      for ((role, idx) <- servers.zipWithIndex) {
        runOn(role) {
          val binding = DiscoveryXServer(discoveryX).start().futureValue
          binding.localAddress.getPort should be(48000 + idx)
          enterBarrier("server-startup")
        }
      }
      runOn(clients: _*) {
        enterBarrier("server-startup")
        val response = TestUtils(discoveryX.system).login().futureValue
        response.status should be(IntStatus.OK)
        logined = response.data.logined.value
      }
    }

    "create namespace" in {
      runOn(servers: _*) { enterBarrier("create-namespace") }
      runOn(clients: _*) {
        if (myself == clients.head) {
          val response = TestUtils(discoveryX.system).createNamespace(logined, "public").futureValue
          response.status should (be(IntStatus.OK) or be(IntStatus.CONFLICT))
        }
        enterBarrier("create-namespace")
      }
    }

    "get public namespace" in {
      runOn(servers: _*) { enterBarrier("get-public-namespace") }
      runOn(clients: _*) {
        val response = TestUtils(discoveryX.system).listNamespace(logined).futureValue
        response.status should be(IntStatus.OK)
        val listed = response.data.listed.value
        namespace = listed.namespaces.find(_.name == "public").value
        enterBarrier("get-public-namespace")
      }
    }

    "register server instance" in {
      runOn(servers: _*) {
        enterBarrier("register-instance")
      }
      runOn(clients: _*) {
        for ((_, idx) <- clients.zipWithIndex) {
          val namingClient = DefaultNamingClient(discoveryX.system)
          val in =
            InstanceRegister(
              namespace.namespace,
              serviceName,
              groupName,
              s"127.0.0.${200 + idx}",
              50000 + idx,
              health = true,
              enable = true)
          val replay = namingClient.registerInstance(in).futureValue
          IntStatus.isSuccess(replay.status) shouldBe true
        }
        enterBarrier("register-instance")
      }
    }

    "query instance" in {
      runOn(servers: _*) {
        enterBarrier("query-instance")
      }
      runOn(clients: _*) {
        val namingClient = DefaultNamingClient(discoveryX.system)
        namingClient.serverStatus(ServerStatusQuery()).futureValue.status should be(IntStatus.OK)

        val result =
          namingClient
            .queryInstance(InstanceQuery(namespace.namespace, serviceName, groupName, allHealthy = true))
            .futureValue
        println(s"Query Instance return is: $result")
        result.status should be(IntStatus.OK)
        result.data.serviceInfo should not be empty
        enterBarrier("query-instance")
      }
    }
  }
}

class DiscoveryXMultiTestMultiJvmNode1 extends DiscoveryXMultiTest
class DiscoveryXMultiTestMultiJvmNode2 extends DiscoveryXMultiTest
class DiscoveryXMultiTestMultiJvmNode3 extends DiscoveryXMultiTest
class DiscoveryXMultiTestMultiJvmNode4 extends DiscoveryXMultiTest
//class DiscoveryXMultiTestMultiJvmNode5 extends DiscoveryXMultiTest
//class DiscoveryXMultiTestMultiJvmNode6 extends DiscoveryXMultiTest
