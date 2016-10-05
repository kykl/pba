package io.bigfast.tracking

import java.util.Date
import java.util.concurrent.TimeUnit
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver

/**
  * Created by kykl on 9/30/16.
  */
object TrackerClient {
  def main(args:Array[String]) = {
    val builder = ManagedChannelBuilder.forAddress("127.0.0.1", 9443).usePlaintext(true)
    val channel = builder.build
    try {
      val client = TrackingGrpc.stub(channel)

      val empty = new StreamObserver[Empty] {
        override def onError(t: Throwable): Unit = {
          t.printStackTrace()
          //throw t
        }

        override def onCompleted(): Unit = {}

        override def onNext(value: Empty): Unit = {}
      }

      val events = client.track(empty)

      (1 to 100000).foreach { i =>
        val now = System.nanoTime()
        events.onNext(Event(id = i.toString + ": " + java.util.UUID.randomUUID.toString, uid = java.util.UUID.randomUUID.toString, createdAt = now, collectedAt = now))
      }

      //Thread.sleep(5000)
      events.onCompleted()
    }
    finally {
      channel.shutdown.awaitTermination(10, TimeUnit.SECONDS)
    }
  }

}
