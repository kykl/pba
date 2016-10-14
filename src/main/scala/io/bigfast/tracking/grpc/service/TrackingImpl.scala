package io.bigfast.tracking.grpc.service

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.scaladsl.Source
import akka.stream.{Materializer, OverflowStrategy}
import io.bigfast.tracking.{Empty, Event, TrackingGrpc}
import io.grpc.stub.StreamObserver
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}

class TrackingImpl()(implicit val system:ActorSystem, val materilizer:Materializer) extends TrackingGrpc.Tracking {
  override def track(responseObserver: StreamObserver[Empty]): StreamObserver[Event]  = {
    val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)

    val queue = Source.queue[Event](100000, OverflowStrategy.backpressure)
      .map { event =>
        new ProducerRecord[Array[Byte], String]("event", event.toString)
      }
      .to(Producer.plainSink(producerSettings))
      .run()

    new StreamObserver[Event] {
      override def onError(t: Throwable): Unit = {
        throw t
      }

      override def onCompleted(): Unit = {

      }

      override def onNext(event: Event): Unit = {
        queue.offer(event)
      }
    }
  }
}
