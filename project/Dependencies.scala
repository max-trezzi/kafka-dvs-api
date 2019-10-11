import sbt._

object Dependencies {

  object CustomResolvers {

    lazy val BitrockNexus = "Bitrock Nexus" at "https://nexus.reactive-labs.io/repository/maven-bitrock/"
    lazy val Confluent    = "confluent" at "https://packages.confluent.io/maven/"

    lazy val resolvers: Seq[Resolver] = Seq(BitrockNexus, Confluent)

  }

  object Versions {

    lazy val Scala             = "2.12.8"
    lazy val Akka              = "2.5.22"
    lazy val AkkaHttp          = "10.1.8"
    lazy val AkkaHttpCors      = "0.4.0"
    lazy val ConfluentPlatform = "5.1.0"
    lazy val JakartaWsRs       = "2.1.4"
    lazy val Kafka             = "2.1.0"
    lazy val KafkaFlightStream = "0.1.11"
    lazy val TestCommons       = "0.0.3"
    lazy val KafkaCommons      = "0.0.3"
    lazy val LogbackClassic    = "1.2.3"
    lazy val PureConfig        = "0.10.2"
    lazy val ScalaLogging      = "3.9.2"
    lazy val Slf4j             = "1.7.26"

  }

  object Logging {

    lazy val prodDeps: Seq[ModuleID] = Seq(
      "ch.qos.logback"             % "logback-classic"  % Versions.LogbackClassic, // required by scala-logging
      "com.typesafe.scala-logging" %% "scala-logging"   % Versions.ScalaLogging,
      "org.slf4j"                  % "log4j-over-slf4j" % Versions.Slf4j // mandatory when log4j gets excluded
    )

    lazy val excludeDeps: Seq[ExclusionRule] = Seq(
      ExclusionRule("org.slf4j", "slf4j-log4j12"),
      ExclusionRule("log4j", "log4j")
    )

  }

  lazy val prodDeps: Seq[ModuleID] = Seq(
    "ch.megard"                    %% "akka-http-cors"                  % Versions.AkkaHttpCors,
    "com.github.pureconfig"        %% "pureconfig"                      % Versions.PureConfig,
    "com.typesafe.akka"            %% "akka-http-spray-json"            % Versions.AkkaHttp,
    "io.confluent"                 % "kafka-avro-serializer"            % Versions.ConfluentPlatform,
    "it.bitrock.kafkaflightstream" %% "kafka-flightstream-avro-schemas" % Versions.KafkaFlightStream,
    "it.bitrock.kafkageostream"    %% "kafka-commons"                   % Versions.KafkaCommons,
    "org.apache.kafka"             % "kafka-clients"                    % Versions.Kafka
  ) ++ Logging.prodDeps

  lazy val testDeps: Seq[ModuleID] = Seq(
    "com.typesafe.akka"         %% "akka-http-testkit"              % Versions.AkkaHttp,
    "com.typesafe.akka"         %% "akka-stream-testkit"            % Versions.Akka,
    "io.github.embeddedkafka"   %% "embedded-kafka-schema-registry" % Versions.ConfluentPlatform,
    "it.bitrock.kafkageostream" %% "test-commons"                   % Versions.TestCommons,
    "jakarta.ws.rs"             % "jakarta.ws.rs-api"               % Versions.JakartaWsRs, // mandatory when javax.ws.rs-api gets excluded
    "org.mockito"               % "mockito-core"                    % "2.7.22"
  ).map(_ % s"$Test,$IntegrationTest")

  lazy val excludeDeps: Seq[ExclusionRule] = Seq(
    ExclusionRule("javax.ws.rs", "javax.ws.rs-api")
  ) ++ Logging.excludeDeps

}
