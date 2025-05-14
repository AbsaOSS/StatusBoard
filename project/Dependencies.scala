import sbt.*

object Dependencies {
  object Versions {
    val scala213 = "2.13.12"

    val zio = "2.0.21"
    val zioLogging = "2.2.0"
    val zioConfig = "4.0.1"
    val zioMetricsConnectors = "2.3.1"
    val zioCache = "0.2.3"
    val zioAws = "7.21.15.11"

    val http4s = "0.23.16"
    val http4sPrometheus = "0.24.6"

    val tapir = "1.9.11"
  }

  def commonDependencies: Seq[ModuleID] = {
    Seq(
      "ch.qos.logback" % "logback-classic" % "1.5.6",
      // zio
      "dev.zio" %% "zio" % Versions.zio,
      "dev.zio" %% "zio-macros" % Versions.zio,
      "dev.zio" %% "zio-logging" % Versions.zioLogging,
      "dev.zio" %% "zio-logging-slf4j2" % Versions.zioLogging,
      "dev.zio" %% "zio-config" % Versions.zioConfig,
      "dev.zio" %% "zio-config-magnolia" % Versions.zioConfig,
      "dev.zio" %% "zio-config-typesafe" % Versions.zioConfig,
      "dev.zio" %% "zio-metrics-connectors-prometheus" % Versions.zioMetricsConnectors,
      "dev.zio" %% "zio-concurrent" % Versions.zio,
      "dev.zio" %% "zio-cache" % Versions.zioCache,
      // zio-aws
      "dev.zio" %% "zio-aws-netty" % Versions.zioAws,
      "dev.zio" %% "zio-aws-core" % Versions.zioAws,
	    "dev.zio" %% "zio-aws-sts" % Versions.zioAws,
      "dev.zio"%% "zio-aws-dynamodb" % Versions.zioAws,
      "dev.zio" %% "zio-aws-rds" % Versions.zioAws,
      "dev.zio" %% "zio-aws-emr" % Versions.zioAws,
      "dev.zio" %% "zio-aws-ec2" % Versions.zioAws,
      // http4s
      "org.http4s" %% "http4s-blaze-server" % Versions.http4s,
      "org.http4s" %% "http4s-blaze-client" % Versions.http4s,
      "org.http4s" %% "http4s-prometheus-metrics" % Versions.http4sPrometheus,
      // tapir
      "com.softwaremill.sttp.tapir" %% "tapir-http4s-server-zio" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-prometheus-metrics" % Versions.tapir,
      "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % Versions.tapir % Test,
      // circe
      "io.circe" %% "circe-generic-extras" % "0.14.1",
      "com.softwaremill.sttp.client3" %% "circe" % "3.9.5" % Test,
      // email
      "com.sun.mail" % "jakarta.mail" % "2.0.1",
      // NameOf macro
      "com.github.dwickern" %% "scala-nameof" % "4.0.0" % "provided",
      // testing
      "org.scalatest" %% "scalatest" % "3.2.18"  % Test,
      "dev.zio" %% "zio-test" % Versions.zio % Test,
      "dev.zio" %% "zio-test-sbt" % Versions.zio % Test,
      "dev.zio" %% "zio-test-junit" % Versions.zio % Test,
      "com.github.sbt" % "junit-interface" % "0.13.3" % Test,
      "org.mockito" % "mockito-core" % "5.11.0" % Test
    )
  }
}
