package io.bigfast.tracking.grpc.service

import akka.actor.ActorSystem
import akka.kafka.scaladsl.Producer
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.stream.scaladsl.{Sink, Source, SourceQueue}
import akka.stream.{Materializer, OverflowStrategy}
import io.bigfast.tracking.{Empty, Event, TrackingGrpc}
import io.grpc.stub.StreamObserver
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer

class TrackingImpl()(implicit val system:ActorSystem, val materilizer:Materializer) extends TrackingGrpc.Tracking {
  val conf =  system.settings.config
  val qBufsize = conf.getInt("tracking.service.queue.bufsize")

  override def track(responseObserver: StreamObserver[Empty]): StreamObserver[Event]  = {
    val producerSettings = ProducerSettings(system, new ByteArraySerializer, new ByteArraySerializer)
    var queue:SourceQueue[Event] = null
    Source.queue[Event](qBufsize, OverflowStrategy.backpressure).mapMaterializedValue { q => queue = q }
    .map { event =>
      ProducerMessage.Message(new ProducerRecord[Array[Byte], Array[Byte]]("event2", event.toByteArray), event.id)
    }
    .via(Producer.flow(producerSettings)).map { result =>
      val record = result.message.record
      val event = Event.parseFrom(record.value)
      println(s"${record.topic}/${record.partition} ${result.offset}: ${event}" +
        s"(${result.message.passThrough})")
      result
    }
    .runWith(Sink.ignore)

    new StreamObserver[Event] {
      override def onError(t: Throwable): Unit = {
        t.printStackTrace()
        throw t
      }

      override def onCompleted(): Unit = {
        println("completed")
        responseObserver.onCompleted()
      }

      override def onNext(event: Event): Unit = {
        try {
          queue.offer(event)
        }
        catch {
          case t:Throwable =>
            println(s"onNext ${event.id}")
            t.printStackTrace()
        }
      }
    }
  }
}
