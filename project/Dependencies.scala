import sbt._

object Dependencies {
  val versionScala212 = "2.12.10"
  val versionScala213 = "2.13.1"
  val versionScalaCollectionCompat = "2.1.2"
  val versionJava8Compat = "0.9.0"
  val versionScalameta = "4.2.3"
  val versionScalatest = "3.0.8"
  val versionAkka = "2.6.1"
  val versionAkkaHttp = "10.1.11"
  val versionConfig = "1.3.4"
  val versionHikariCP = "3.4.1"
  val versionMySQL = "8.0.16"
  val versionPostgres = "42.2.9"
  val versionAlpnAgent = "2.0.9"

  val versionFusion = "2.0.1"

  val _fusionCommon = "com.akka-fusion" %% "fusion-common" % versionFusion
  val _fusionCore = "com.akka-fusion" %% "fusion-core" % versionFusion
  val _fusionProtobufV3 = "com.akka-fusion" %% "fusion-protobuf-v3" % versionFusion
  val _fusionTestkit = "com.akka-fusion" %% "fusion-testkit" % versionFusion

  val _akkaPersistenceTyped = "com.typesafe.akka" %% "akka-persistence-typed" % versionAkka
  val _akkaPersistenceQuery = "com.typesafe.akka" %% "akka-persistence-query" % versionAkka

  val _akkaMultiNodeTestkit = "com.typesafe.akka" %% "akka-multi-node-testkit" % versionAkka

  val _akkaClusters = Seq(
    "com.typesafe.akka" %% "akka-cluster-typed" % versionAkka,
    "com.typesafe.akka" %% "akka-cluster-sharding-typed" % versionAkka)

  val _akkaHttp = ("com.typesafe.akka" %% "akka-http" % versionAkkaHttp)
    .exclude("com.typesafe.akka", "akka-stream")
    .cross(CrossVersion.binary)

  val _akkaHttpTestkit = ("com.typesafe.akka" %% "akka-http-testkit" % versionAkkaHttp)
    .exclude("com.typesafe.akka", "akka-stream-testkit")
    .cross(CrossVersion.binary)
    .exclude("com.typesafe.akka", "akka-testkit")
    .cross(CrossVersion.binary)

  val _akkaHttp2 = ("com.typesafe.akka" %% "akka-http2-support" % versionAkkaHttp)
    .exclude("com.typesafe.akka", "akka-http-core")
    .cross(CrossVersion.binary)
    .exclude("com.typesafe.akka", "akka-stream")
    .cross(CrossVersion.binary)

  val _akkaHttps = Seq(_akkaHttp, _akkaHttp2, _akkaHttpTestkit % Test)

  val _akkaPersistenceCassandras = Seq("com.typesafe.akka" %% "akka-persistence-cassandra" % "0.101").map(
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
    ("com.github.dnvriend" %% "akka-persistence-jdbc" % "3.5.2")
      .excludeAll(ExclusionRule("com.typesafe.akka"))
      .cross(CrossVersion.binary)

  val _scalapbJson4s = "com.thesamet.scalapb" %% "scalapb-json4s" % "0.10.0"

  val _postgresql = "org.postgresql" % "postgresql" % versionPostgres
  val _mysql = "mysql" % "mysql-connector-java" % versionMySQL
  val _h2 = "com.h2database" % "h2" % "1.4.200"
  val _hikariCP = "com.zaxxer" % "HikariCP" % versionHikariCP

  val _alpnAgent = "org.mortbay.jetty.alpn" % "jetty-alpn-agent" % versionAlpnAgent
}
