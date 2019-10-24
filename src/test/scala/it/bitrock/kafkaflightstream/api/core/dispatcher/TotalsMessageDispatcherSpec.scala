package it.bitrock.kafkaflightstream.api.core.dispatcher

import java.net.URI

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import it.bitrock.kafkaflightstream.api.config.{ConsumerConfig, KafkaConfig, WebsocketConfig}
import it.bitrock.kafkaflightstream.api.core.CoreResources.{ResourceMessageDispatcher, TestKafkaConsumerWrapperFactory}
import it.bitrock.kafkaflightstream.api.core.TotalsMessageDispatcherFactoryImpl
import it.bitrock.kafkaflightstream.api.core.poller.TotalsKafkaPollerCache
import it.bitrock.kafkaflightstream.api.definitions._
import it.bitrock.kafkaflightstream.api.kafka.KafkaConsumerWrapperFactory
import it.bitrock.kafkaflightstream.api.{BaseSpec, TestValues}
import it.bitrock.kafkageostream.testcommons.FixtureLoanerAnyResult
import org.scalatest.BeforeAndAfterAll
import spray.json._

import scala.concurrent.duration._
import scala.util.Random

class TotalsMessageDispatcherSpec
    extends TestKit(ActorSystem("TotalsMessageDispatcherSpec"))
    with BaseSpec
    with ImplicitSender
    with BeforeAndAfterAll
    with JsonSupport
    with TestValues {

  "Totals Message Dispatcher" should {

    "forward a JSON to source actor" when {
      "a CountFlight is received" in ResourceLoaner.withFixture {
        case ResourceMessageDispatcher(websocketConfig, kafkaConfig, consumerFactory, _, sourceProbe) =>
          val totalsKafkaPollerCache = TotalsKafkaPollerCache.build(kafkaConfig, consumerFactory)
          val messageDispatcher      = new TotalsMessageDispatcherFactoryImpl(websocketConfig, totalsKafkaPollerCache).build(sourceProbe.ref)
          val msg                    = CountFlight(DefaultStartTimeWindow, DefaultCountFlightAmount)
          messageDispatcher ! msg
          val expectedResult = ApiEvent(msg.getClass.getSimpleName, msg).toJson.toString
          sourceProbe.expectMsg(expectedResult)
      }
      "a CountAirline is received" in ResourceLoaner.withFixture {
        case ResourceMessageDispatcher(websocketConfig, kafkaConfig, consumerFactory, _, sourceProbe) =>
          val totalsKafkaPollerCache = TotalsKafkaPollerCache.build(kafkaConfig, consumerFactory)
          val messageDispatcher      = new TotalsMessageDispatcherFactoryImpl(websocketConfig, totalsKafkaPollerCache).build(sourceProbe.ref)
          val msg                    = CountAirline(DefaultStartTimeWindow, DefaultCountAirlineAmount)
          messageDispatcher ! msg
          val expectedResult = ApiEvent(msg.getClass.getSimpleName, msg).toJson.toString
          sourceProbe.expectMsg(expectedResult)
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
