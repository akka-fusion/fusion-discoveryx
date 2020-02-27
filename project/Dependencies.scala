import fusion.sbt.gen.BuildInfo
import sbt._

object Dependencies {
  val versionH2 = "1.4.200"
  val versionPlayAhcWsStandalone = "2.1.2"
  val versionPlayAhcWs = "2.8.0"
  val versionAkkaPersistenceCassandra = "0.101"
  val versionAkkaPersistenceJdbc = "3.5.2"

  val _akkaDiscovery = "com.typesafe.akka" %% "akka-discovery" % BuildInfo.versionAkka
  val _akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % BuildInfo.versionAkka
  val _akkaPersistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % BuildInfo.versionAkka
  val _akkaMultiNodeTestkit = "com.typesafe.akka" %% "akka-multi-node-testkit" % BuildInfo.versionAkka

  val _akkaClusters = Seq(
    "com.typesafe.akka" %% "akka-cluster-typed" % BuildInfo.versionAkka,
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % BuildInfo.versionAkka)

  val _akkaHttp = ("com.typesafe.akka" %% "akka-http" % BuildInfo.versionAkkaHttp)
    .exclude("com.typesafe.akka", "akka-stream")
    .cross(CrossVersion.binary)

  val _akkaHttpTestkit = ("com.typesafe.akka" %% "akka-http-testkit" % BuildInfo.versionAkkaHttp)
    .exclude("com.typesafe.akka", "akka-stream-testkit")
    .cross(CrossVersion.binary)
    .exclude("com.typesafe.akka", "akka-testkit")
    .cross(CrossVersion.binary)

  val _akkaHttp2 = ("com.typesafe.akka" %% "akka-http2-support" % BuildInfo.versionAkkaHttp)
    .exclude("com.typesafe.akka", "akka-http-core")
    .cross(CrossVersion.binary)
    .exclude("com.typesafe.akka", "akka-stream")
    .cross(CrossVersion.binary)

  val _akkaHttps = Seq(_akkaHttp, _akkaHttp2, _akkaHttpTestkit % Test)

  val _akkaPersistenceCassandras =
    Seq("com.typesafe.akka" %% "akka-persistence-cassandra" % versionAkkaPersistenceCassandra).map(
      _.exclude("org.scala-lang", "scala-library")
        .cross(CrossVersion.binary)
        .exclude("com.typesafe.akka", "akka-cluster-tools")
        .cross(CrossVersion.binary)
        .exclude("com.typesafe.akka", "akka-cluster-tools")
        .cross(CrossVersion.binary)
        .exclude("com.typesafe.akka", "akka-persistence")
        .cross(CrossVersion.binary)
        .exclude("com.typesafe.akka", "akka-persistence-query")
        .cross(CrossVersion.binary))

  val _akkaPersistenceJdbc =
    ("com.github.dnvriend" %% "akka-persistence-jdbc" % versionAkkaPersistenceJdbc)
      .excludeAll(ExclusionRule("com.typesafe.akka"))
      .cross(CrossVersion.binary)

  val _scalapbJson4s = "com.thesamet.scalapb" %% "scalapb-json4s" % BuildInfo.versionScalapbJson4s

  val _osLib = "com.lihaoyi" %% "os-lib" % BuildInfo.versionOsLib

  val _postgresql = "org.postgresql" % "postgresql" % BuildInfo.versionPostgres
  val _mysql = "mysql" % "mysql-connector-java" % BuildInfo.versionMySQL
  val _h2 = "com.h2database" % "h2" % versionH2
  val _hikariCP = "com.zaxxer" % "HikariCP" % BuildInfo.versionHikariCP

  val _alpnAgent = "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % BuildInfo.versionAlpnAgent

  val _playWSStandalone = "com.typesafe.play" %% "play-ahc-ws-standalone" % versionPlayAhcWsStandalone
  val _playWS = "com.typesafe.play" %% "play-ahc-ws" % versionPlayAhcWs
}
