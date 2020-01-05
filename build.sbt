import Commons._
import Dependencies._
import Environment._

ThisBuild / offline := true

ThisBuild / updateOptions := updateOptions.value.withCachedResolution(true).withLatestSnapshots(false)

ThisBuild / buildEnv := {
  sys.props
    .get("build.env")
    .orElse(sys.env.get("BUILD_ENV"))
    .flatMap {
      case "prod"  => Some(BuildEnv.Production)
      case "stage" => Some(BuildEnv.Stage)
      case "test"  => Some(BuildEnv.Test)
      case "dev"   => Some(BuildEnv.Development)
      case _       => None
    }
    .getOrElse(BuildEnv.Development)
}

ThisBuild / scalaVersion := versionScala213

ThisBuild / crossScalaVersions := Seq(versionScala212, versionScala213)

ThisBuild / scalafmtOnCompile := true

ThisBuild / sonarUseExternalConfig := true

ThisBuild / resolvers ++= Seq(
  "Bintray akka-fusion".at("https://akka-fusion.bintray.com/maven"),
  Resolver.sonatypeRepo("snapshots"))

lazy val root = Project(id = "fusion-discoveryx", base = file("."))
  .aggregate(discoveryxFunctest, discoveryxClientPlayWs, discoveryxServer, discoveryxClient, discoveryxCommon)
  .settings(Environment.settings: _*)
  .settings(skip in publish := true, aggregate in sonarScan := false)

lazy val discoveryxDocs = _project("discoveryx-docs")
  .enablePlugins(AkkaParadoxPlugin)
  .dependsOn(discoveryxFunctest, discoveryxClientPlayWs, discoveryxServer, discoveryxClient, discoveryxCommon)
  .settings(
    resolvers += Resolver.jcenterRepo,
    skip in publish := true,
    paradoxGroups := Map("Language" -> Seq("Scala")),
    sourceDirectory in Compile in paradoxTheme := sourceDirectory.value / "main" / "paradox" / "_template",
    paradoxProperties ++= Map(
        "project.name" -> "Fusion DiscoveryX",
        "canonical.base_url" -> "http://akka-fusion.github.io/akka-fusion/",
        "github.base_url" -> s"https://github.com/akka-fusion/fusion-discoveryx/tree/${version.value}",
        "scala.version" -> scalaVersion.value,
        "scala.binary_version" -> scalaBinaryVersion.value,
        "scaladoc.akka.base_url" -> s"http://doc.akka.io/api/$versionAkka",
        "play.ahc-ws-standalone.version" -> "2.1.2",
        "akka.version" -> versionAkka,
        "version" -> version.value))

lazy val discoveryxFunctest = _project("discoveryx-functest")
  .enablePlugins(MultiJvmPlugin)
  .dependsOn(discoveryxServer, discoveryxClient)
  .configs(MultiJvm)
  .settings(
    skip in publish := true,
    jvmOptions in MultiJvm := Seq("-Xmx512M"),
    libraryDependencies ++= Seq(_akkaMultiNodeTestkit % Test))

lazy val discoveryxClientPlayWs =
  _project("discoveryx-client-play-ws")
    .dependsOn(discoveryxClient)
    .settings(Publishing.publishing: _*)
    .settings(libraryDependencies ++= Seq(_playWS % Provided, _playWSStandalone))

lazy val discoveryxServer = _project("discoveryx-server")
  .enablePlugins(JavaAgent, AkkaGrpcPlugin, JavaAppPackaging)
  .dependsOn(discoveryxCommon)
  .settings(
    skip in publish := true,
    javaAgents += _alpnAgent % "runtime;test",
    akkaGrpcCodeGeneratorSettings += "server_power_apis",
    mainClass in Compile := Some("fusion.discoveryx.server.FusionDiscoveryXMain"),
    maintainer := "yang.xunjing@qq.com",
    bashScriptExtraDefines ++= Seq(
        """addJava "-Dconfig.file=${app_home}/../conf/application.conf"""",
        """addJava "-Dlogback.configurationFile=${app_home}/../conf/logback.xml"""",
        """addJava "-Dpidfile.path=${app_home}/../run/%s.pid"""".format(name.value)),
    batScriptExtraDefines ++= Seq(
        """call :add_java "-Dconfig.file=%APP_HOME%\conf\application.conf"""",
        """call :add_java "-Dlogback.configurationFile=${app_home}/../conf/logback.xml""""),
    scriptClasspath := Seq("*"),
    libraryDependencies ++= Seq(
        _scalapbJson4s,
        _postgresql % Runtime,
        _mysql % Provided,
        _h2,
        _hikariCP,
        _fusionProtobufV3,
        _fusionCore,
        _akkaPersistenceQuery,
        _akkaPersistenceJdbc,
        _akkaPersistenceTyped) ++ _akkaHttps ++ _akkaClusters ++ _akkaPersistenceCassandras)

lazy val discoveryxClient =
  _project("discoveryx-client")
    .dependsOn(discoveryxCommon)
    .settings(Publishing.publishing: _*)
    .settings(libraryDependencies ++= Seq())

lazy val discoveryxCommon = _project("discoveryx-common")
  .enablePlugins(AkkaGrpcPlugin)
  .settings(Publishing.publishing: _*)
  .settings(
    akkaGrpcCodeGeneratorSettings += "server_power_apis",
    libraryDependencies ++= Seq(
        "com.thesamet.scalapb" %% "scalapb-runtime" % scalapb.compiler.Version.scalapbVersion % "protobuf",
        _fusionCommon))

def _project(name: String, _base: String = null) =
  Project(id = name, base = file(if (_base eq null) name else _base))
    .enablePlugins(AutomateHeaderPlugin)
    .settings(basicSettings: _*)
    .settings(libraryDependencies ++= Seq(_fusionTestkit % Test))
