name := "akka-workshop"

version := "0.1"

scalaVersion := "2.12.6"


lazy val akkaVersion = "2.6.18"
lazy val akkaHttpVersion = "10.2.9"
lazy val alpakkaVersion = "1.1.0"
lazy val parasolVersion = "0.2.0-SNAPSHOT"
lazy val akkaManagementVersion = "1.0.8"
lazy val elastic4sVersion = "6.3.7" //"7.0.1"
lazy val elasticVersion = "6.3.2"
lazy val redisScalaVersion = "1.8.4"
lazy val jodaTimeVersion = "2.9.9"
lazy val kamonVersion = "2.2.2"
lazy val jnaVersion = "5.8.0"
lazy val micrometerVersion = "1.7.2"
lazy val prometheusVersion = "0.11.0"
lazy val jmesPathVersion = "0.5.0"
lazy val jacksonModuleScala = "2.13.0"
lazy val mysqlConnectorVersion = "8.0.15" //"8.0.29" //"6.0.6" 8.0.29
lazy val odelayVersion = "0.3.2"
lazy val hikariCPVer = "4.0.3"
lazy val circeVersion = "0.14.1"

lazy val reflectASM = "com.esotericsoftware" % "reflectasm" % "1.11.3"

resolvers += Resolver.jcenterRepo

assemblyMergeStrategy in assembly := {
  case "plugin.properties" => MergeStrategy.last
  case "log4j.properties" => MergeStrategy.last
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case "META-INF/cxf/bus-extensions.txt" => MergeStrategy.first
  case "META-INF/blueprint.handlers" => MergeStrategy.first
  case "mozilla/public-suffix-list.txt" => MergeStrategy.first
  case PathList("about.html") => MergeStrategy.rename
  case "application.conf" => MergeStrategy.concat
  case "akka/discovery/kubernetes/KubernetesApiServiceDiscovery.class" => MergeStrategy.first
  case "akka/discovery/kubernetes/KubernetesApiServiceDiscovery$.class" => MergeStrategy.first
  case "module-info.class" => MergeStrategy.discard
  case "META-INF/versions/11/module-info.class" => MergeStrategy.discard
  case x =>
    val oldStrategy = (assemblyMergeStrategy in assembly).value
    oldStrategy(x)
}

/**
 * kryo-shaded-4.0.2.jar and reflectasm-1.11.3 conflict
 */

assemblyExcludedJars in assembly := {
  val cp = (fullClasspath in assembly).value
  cp.filter(x => (x.data.getName == "jcl-over-slf4j-1.7.24.jar")
    || (x.data.getName == "lz4-1.3.0.jar")
    || (x.data.getName == "lz4-java-1.4.jar")
    || (x.data.getName == "reflectasm-1.09.jar")
    || (x.data.getName == "reflectasm-1.11.3.jar")
    || (x.data.getName == "jna-4.5.1.jar")
    || (x.data.getName == "commons-logging-api-1.1.jar")
  )
}

//assemblyJarName in assembly := s"${name.value}_${scalaVersion.value.substring(0,4)}-${version.value}-assembly.jar"
assemblyJarName in assembly := s"orchestrator-assembly.jar"

mainClass in Compile := Some("tech.parasol.akka.workshop.cluster.ShardingApp")

mainClass in assembly := Some("tech.parasol.akka.workshop.cluster.ShardingApp")


javaAgents += "org.aspectj" % "aspectjweaver" % "1.9.7" % "runtime"

enablePlugins(JavaAgent)

val jmesPath = Seq(
  "io.burt" % "jmespath-core" % jmesPathVersion,
  "io.burt" % "jmespath-jackson" % jmesPathVersion
)

val mysqlConnector = Seq(
  "mysql"                                      % "mysql-connector-java"          % mysqlConnectorVersion
)

val circe = Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)

val jna = Seq(
  "net.java.dev.jna" % "jna" % jnaVersion
)

val kamon = Seq(
  "io.kamon" %% "kamon-bundle" % kamonVersion,
  "io.kamon" %% "kamon-prometheus" % kamonVersion
)

val odelay = "com.softwaremill.odelay"  %% "odelay-core" % odelayVersion

val akkaManagement = Seq(
  "com.lightbend.akka.management" %% "akka-management" % akkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-bootstrap" % akkaManagementVersion,
  "com.lightbend.akka.management" %% "akka-management-cluster-http" % akkaManagementVersion,
  //"com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % akkaManagementVersion
)

val kontainers = Seq(
  "io.kontainers" %% "micrometer-akka" % "0.12.2",
  "io.micrometer" % "micrometer-registry-prometheus" % micrometerVersion,
  "io.prometheus" % "simpleclient" % prometheusVersion,
  "io.prometheus" % "simpleclient_common" % prometheusVersion
)
val hikariCP = Seq(
  "com.zaxxer" % "HikariCP" % hikariCPVer
)

libraryDependencies ++= akkaManagement ++ circe ++ hikariCP ++ jmesPath ++ kontainers ++ mysqlConnector ++ kamon ++ jna ++ Seq(
  "com.typesafe.akka"                         %% "akka-actor"                    % akkaVersion,
  //"com.typesafe.akka"                         %% "akka-actor-typed"                    % akkaVersion,
  "com.typesafe.akka"                         %% "akka-stream"                   % akkaVersion,
  "com.typesafe.akka"                         %% "akka-slf4j"                    % akkaVersion,
  "com.typesafe.akka"                         %% "akka-remote"                   % akkaVersion,
  "com.typesafe.akka"                         %% "akka-cluster"                  % akkaVersion,
  "com.typesafe.akka"                         %% "akka-cluster-metrics"          % akkaVersion,
  "com.typesafe.akka"                         %% "akka-cluster-sharding"         % akkaVersion,
  "com.typesafe.akka"                         %% "akka-persistence"              % akkaVersion,
  "com.typesafe.akka"                         %% "akka-persistence-query"        % akkaVersion,
  "com.typesafe.akka"                         %% "akka-discovery"                % akkaVersion,
  "com.typesafe.akka"                         %% "akka-stream-testkit"           % akkaVersion,
  "ch.qos.logback"                             % "logback-classic"               % "1.2.3",
  "com.typesafe.play"                         %% "play-json"                     % "2.6.4",
  "ai.x"                                      %% "play-json-extensions"          % "0.10.0",
  "com.fasterxml.jackson.module"              %% "jackson-module-scala"          % jacksonModuleScala, //"2.11.1"
  "ch.megard"                                 %% "akka-http-cors"                % "0.4.2",
  "com.typesafe.akka"                         %% "akka-http-core"                % akkaHttpVersion,
  "com.typesafe.akka"                         %% "akka-http-jackson"             % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka"                         %% "akka-stream-kafka"             % "2.0.4",
  "de.heikoseeberger"                         %% "akka-http-jackson"             % "1.27.0",
  reflectASM,
  odelay,
  "net.debasishg" %% "redisclient" % "3.30",
  "com.twitter" %% "chill" % "0.9.4",
  "com.twitter" %% "chill-akka" % "0.9.4",
  "org.iq80.leveldb"            % "leveldb"          % "0.7",
  "org.fusesource.leveldbjni"   % "leveldbjni-all"   % "1.8",
  "com.github.blemale" %% "scaffeine" % "3.1.0" % "compile",
  "org.scalatest" %% "scalatest" % "3.2.9" % Test
)

unmanagedBase := baseDirectory.value / "libs"


scalacOptions := Seq(
  // Note: Add -deprecation when deprecated methods are removed
  "-target:jvm-1.8",
  "-unchecked",
  "-feature",
  "-language:_",
  "-encoding", "utf8",
  "-Xlint:-missing-interpolator",
  "-Ypatmat-exhaust-depth", "40",
  "-opt:l:inline",
  "-opt-inline-from:**"
)