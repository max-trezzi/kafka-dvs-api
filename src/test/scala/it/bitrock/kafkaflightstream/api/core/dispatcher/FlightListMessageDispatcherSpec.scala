package it.bitrock.kafkaflightstream.api.core.dispatcher

import java.net.URI

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import it.bitrock.kafkaflightstream.api.config.{ConsumerConfig, KafkaConfig, WebsocketConfig}
import it.bitrock.kafkaflightstream.api.core.CoreResources.{ResourceMessageDispatcher, TestKafkaConsumerWrapperFactory}
import it.bitrock.kafkaflightstream.api.core.FlightListMessageDispatcherFactoryImpl
import it.bitrock.kafkaflightstream.api.core.poller.FlightListKafkaPollerCache
import it.bitrock.kafkaflightstream.api.definitions._
import it.bitrock.kafkaflightstream.api.kafka.KafkaConsumerWrapperFactory
import it.bitrock.kafkaflightstream.api.{BaseSpec, TestValues}
import it.bitrock.kafkageostream.testcommons.FixtureLoanerAnyResult
import org.scalatest.BeforeAndAfterAll
import spray.json._

import scala.concurrent.duration._
import scala.util.Random

class FlightListMessageDispatcherSpec
    extends TestKit(ActorSystem("FlightMessageDispatcherSpec"))
    with BaseSpec
    with ImplicitSender
    with BeforeAndAfterAll
    with JsonSupport
    with TestValues {

  "Flight List Message Dispatcher" should {

    "forward a JSON to source actor" when {
      "a correct FlightReceivedList message is received" in ResourceLoaner.withFixture {
        case ResourceMessageDispatcher(websocketConfig, kafkaConfig, consumerFactory, _, sourceProbe) =>
          val flightListKafkaPollerCache = FlightListKafkaPollerCache.build(kafkaConfig, consumerFactory)
          val messageProcessor =
            new FlightListMessageDispatcherFactoryImpl(websocketConfig, flightListKafkaPollerCache).build(sourceProbe.ref)
          val msg = FlightReceivedList(
            Seq(
              FlightReceived(
                DefaultIataNumber,
                DefaultIcaoNumber,
                GeographyInfo(DefaultInBoxLatitude, DefaultInBoxLongitude, DefaultAltitude, DefaultDirection),
                DefaultSpeed,
                AirportInfo(
                  DefaultCodeAirport1,
                  DefaultNameAirport1,
                  DefaultNameCountry1,
                  DefaultCodeIso2Country1,
                  DefaultTimezone1,
                  DefaultGmt1
                ),
                AirportInfo(
                  DefaultCodeAirport2,
                  DefaultNameAirport2,
                  DefaultNameCountry2,
                  DefaultCodeIso2Country2,
                  DefaultTimezone2,
                  DefaultGmt2
                ),
                AirlineInfo(DefaultCodeAirline, DefaultNameAirline, DefaultSizeAirline),
                AirplaneInfo(DefaultNumberRegistration, DefaultProductionLine, DefaultModelCode),
                DefaultStatus,
                DefaultUpdated
              )
            )
          )
          messageProcessor ! msg
          sourceProbe expectMsg msg.toJson.toString
      }
      "the flights in the list are inside the box after its change" in ResourceLoaner.withFixture {
        case ResourceMessageDispatcher(websocketConfig, kafkaConfig, consumerFactory, _, sourceProbe) =>
          val flightListKafkaPollerCache = FlightListKafkaPollerCache.build(kafkaConfig, consumerFactory)
          val messageProcessor =
            new FlightListMessageDispatcherFactoryImpl(websocketConfig, flightListKafkaPollerCache).build(sourceProbe.ref)
          val msg = FlightReceivedList(
            Seq(
              FlightReceived(
                DefaultIataNumber,
                DefaultIcaoNumber,
                GeographyInfo(DefaultChangedInBoxLatitude, DefaultChangedInBoxLongitude, DefaultAltitude, DefaultDirection),
                DefaultSpeed,
                AirportInfo(
                  DefaultCodeAirport1,
                  DefaultNameAirport1,
                  DefaultNameCountry1,
                  DefaultCodeIso2Country1,
                  DefaultTimezone1,
                  DefaultGmt1
                ),
                AirportInfo(
                  DefaultCodeAirport2,
                  DefaultNameAirport2,
                  DefaultNameCountry2,
                  DefaultCodeIso2Country2,
                  DefaultTimezone2,
                  DefaultGmt2
                ),
                AirlineInfo(DefaultCodeAirline, DefaultNameAirline, DefaultSizeAirline),
                AirplaneInfo(DefaultNumberRegistration, DefaultProductionLine, DefaultModelCode),
                DefaultStatus,
                DefaultUpdated
              )
            )
          )
          messageProcessor ! changedBox
          messageProcessor ! msg
          sourceProbe expectMsg msg.toJson.toString
      }
    }

    "forward an empty message to source actor" when {
      "the flights in the list are out of the box" in ResourceLoaner.withFixture {
        case ResourceMessageDispatcher(websocketConfig, kafkaConfig, consumerFactory, _, sourceProbe) =>
          val flightListKafkaPollerCache = FlightListKafkaPollerCache.build(kafkaConfig, consumerFactory)
          val messageProcessor =
            new FlightListMessageDispatcherFactoryImpl(websocketConfig, flightListKafkaPollerCache).build(sourceProbe.ref)
          messageProcessor ! FlightReceivedList(
            Seq(
              FlightReceived(
                DefaultIataNumber,
                DefaultIcaoNumber,
                GeographyInfo(DefaultOutBoxLatitude, DefaultOutBoxLongitude, DefaultAltitude, DefaultDirection),
                DefaultSpeed,
                AirportInfo(
                  DefaultCodeAirport1,
                  DefaultNameAirport1,
                  DefaultNameCountry1,
                  DefaultCodeIso2Country1,
                  DefaultTimezone1,
                  DefaultGmt1
                ),
                AirportInfo(
                  DefaultCodeAirport2,
                  DefaultNameAirport2,
                  DefaultNameCountry2,
                  DefaultCodeIso2Country2,
                  DefaultTimezone2,
                  DefaultGmt2
                ),
                AirlineInfo(DefaultCodeAirline, DefaultNameAirline, DefaultSizeAirline),
                AirplaneInfo(DefaultNumberRegistration, DefaultProductionLine, DefaultModelCode),
                DefaultStatus,
                DefaultUpdated
              )
            )
          )
          sourceProbe expectMsg FlightReceivedList(Seq()).toJson.toString
      }
    }

  }

  object ResourceLoaner extends FixtureLoanerAnyResult[ResourceMessageDispatcher] {
    override def withFixture(body: ResourceMessageDispatcher => Any): Any = {
      val websocketConfig = WebsocketConfig(1.second, 0.second, "not-used", "not-used", "not-used", "not-used", "not-used")
      val kafkaConfig =
        KafkaConfig(
          "",
          URI.create("http://localhost:8080"),
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          "",
          ConsumerConfig(1.second, Duration.Zero)
        )
      val pollProbe                                    = TestProbe(s"poll-probe-${Random.nextInt()}")
      val sourceProbe                                  = TestProbe(s"source-probe-${Random.nextInt()}")
      val consumerFactory: KafkaConsumerWrapperFactory = new TestKafkaConsumerWrapperFactory(pollProbe.ref)

      body(
        ResourceMessageDispatcher(
          websocketConfig,
          kafkaConfig,
          consumerFactory,
          pollProbe,
          sourceProbe
        )
      )
    }
  }

  override def afterAll: Unit = {
    shutdown()
    super.afterAll()
  }
}
