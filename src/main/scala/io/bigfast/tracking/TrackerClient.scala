package io.bigfast.tracking

import java.util.Date

import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver

/**
  * Created by kykl on 9/30/16.
  */
object TrackerClient {
  def main(args:Array[String]) = {
    val builder = ManagedChannelBuilder.forAddress("127.0.0.1", 9443).usePlaintext(true)
    val channel = builder.build
    val client = TrackingGrpc.stub(channel)

    val events = client.track(new StreamObserver[Empty] {
      override def onError(t: Throwable): Unit = {
        throw t
      }

      override def onCompleted(): Unit = {}

      override def onNext(value: Empty): Unit = {}
    })

    (1 to 100).foreach { _ =>
      val now = new Date
      events.onNext(Event(id = java.util.UUID.randomUUID.toString, uid = java.util.UUID.randomUUID.toString, createdAt = now.getTime, collectedAt = now.getTime))
    }
    Thread.sleep(8000)
    events.onCompleted()
  }

}
