import Commons._
import Dependencies._
import Environment._

ThisBuild / buildEnv := {
  sys.props
    .get("build.env")
    .orElse(sys.env.get("BUILD_ENV"))
    .flatMap {
      case "prod"  => Some(BuildEnv.Production)
      case "stage" => Some(BuildEnv.Stage)
      case "test"  => Some(BuildEnv.Test)
      case "dev"   => Some(BuildEnv.Developement)
      case _       => None
    }
    .getOrElse(BuildEnv.Developement)
}

ThisBuild / scalaVersion := versionScala213

ThisBuild / crossScalaVersions := Seq(versionScala212, versionScala213)

ThisBuild / scalafmtOnCompile := true

ThisBuild / sonarUseExternalConfig := true

lazy val root = Project(id = "fusion-discoveryx", base = file("."))
  .aggregate(discoveryxFunctest, discoveryxServer, discoveryxClient, discoveryxCommon)
  .settings(Environment.settings: _*)
  .settings(skip in publish := true, aggregate in sonarScan := false)

lazy val discoveryxDocs = _project("discoveryx-docs")
  .enablePlugins(/*ParadoxMaterialThemePlugin*/AkkaParadoxPlugin)
  .dependsOn(discoveryxFunctest, discoveryxServer, discoveryxClient, discoveryxCommon)
  .settings(
      resolvers += Resolver.jcenterRepo,
      skip in publish := true,
//    Compile / paradoxMaterialTheme ~= {
//      _.withLanguage(java.util.Locale.SIMPLIFIED_CHINESE)
//        .withColor("indigo", "red")
//        .withRepository(uri("https://github.com/akka-fusion/fusion-discoveryx"))
//        .withSocial(
//          uri("http://akka-fusion.github.io/akka-fusion/"),
//          uri("https://github.com/akka-fusion"),
//          uri("https://weibo.com/yangbajing"))
//    },
      paradoxGroups := Map("Language" -> Seq("Scala")),
      sourceDirectory in Compile in paradoxTheme := sourceDirectory.value / "main" / "paradox" / "_template",
      paradoxProperties ++= Map(
        "project.name" -> "Fusion DiscoveryX",
        "canonical.base_url" -> "http://akka-fusion.github.io/akka-fusion/",
        "github.base_url" -> s"https://github.com/akka-fusion/fusion-discoveryx/tree/${version.value}",
        "version" -> version.value,
        "scala.version" -> scalaVersion.value,
        "scala.binary_version" -> scalaBinaryVersion.value,
        "scaladoc.akka.base_url" -> s"http://doc.akka.io/api/$versionAkka",
        "akka.version" -> versionAkka))

lazy val discoveryxFunctest = _project("discoveryx-functest")
  .enablePlugins(MultiJvmPlugin)
  .dependsOn(discoveryxServer, discoveryxClient)
  .configs(MultiJvm)
  .settings(
    skip in publish := true,
    jvmOptions in MultiJvm := Seq("-Xmx512M"),
    libraryDependencies ++= Seq(_akkaMultiNodeTestkit % Test))

lazy val discoveryxServer = _project("discoveryx-server")
  .enablePlugins(JavaAgent, AkkaGrpcPlugin, JavaAppPackaging) //, LauncherJarPlugin)
  .dependsOn(discoveryxCommon)
  .settings(
    skip in publish := true,
    javaAgents += _alpnAgent % "runtime;test",
    akkaGrpcGeneratedSources := Seq(AkkaGrpc.Server),
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
