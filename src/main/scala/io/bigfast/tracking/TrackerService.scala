package io.bigfast.tracking

import java.util

import akka.actor.ActorSystem
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream.{ActorMaterializer, OverflowStrategy}
import akka.stream.scaladsl.{Sink, Source}
import io.grpc.stub.StreamObserver
import io.grpc.{Server, ServerBuilder}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, Serializer, StringSerializer}

import scala.collection.mutable.Queue

/**
  * Created by kykl on 9/30/16.
  */

object TrackerService {

  //implicit val executionContext = ExecutionContext.global
  implicit val system = ActorSystem("system")
  implicit val executionContext = system.dispatchers.lookup("my-dispatcher")
  implicit val materializer = ActorMaterializer()

  def main(args:Array[String]) = {
    val server = new TrackerService
    server.start()
    server.blockUntilShutdown()
  }
}

class TrackerService {
  self =>

  import TrackerService._

  private[this] var server: Server = _

  private def start(): Unit = {
    server = ServerBuilder
      .forPort(9443)
      .addService(TrackingGrpc.bindService(new Tracker, executionContext)).build.start

    Runtime.getRuntime.addShutdownHook(new Thread() {
      override def run(): Unit = {
        System.err.println("*** shutting down gRPC server since JVM is shutting down")
        self.stop()
        System.err.println("*** server shut down")
      }
    })
  }

  private def stop(): Unit = {
    if (server != null) {
      server.shutdown()
    }
  }

  private def blockUntilShutdown(): Unit = {
    if (server != null) {
      server.awaitTermination()
    }
  }

  private class Tracker extends TrackingGrpc.Tracking {
    override def track(responseObserver: StreamObserver[Empty]): StreamObserver[Event]  = {
      val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer).withBootstrapServers("kafka:9092")

      val queue = Source.queue[Event](1000, OverflowStrategy.backpressure).map { event =>
        new ProducerRecord[Array[Byte], String]("ba-event", event.toString)
      }.to(Producer.plainSink(producerSettings)).run()

      new StreamObserver[Event] {
        override def onError(t: Throwable): Unit = {
          throw t
        }

        override def onCompleted(): Unit = {

        }

        override def onNext(event: Event): Unit = {
          println(s"onNext event.id: ${event.id}")
          queue.offer(event)
        }
      }
    }
  }
}
