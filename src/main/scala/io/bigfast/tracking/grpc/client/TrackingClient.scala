package io.bigfast.tracking.grpc.client

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}
import io.bigfast.tracking.{Empty, Event, TrackingGrpc}
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver

/**
  * Created by kykl on 9/30/16.
  */
object TrackingClient {
  def main(args:Array[String]) = {
    val config = ConfigFactory.load
    val host = config.getString("tracking.client.channel.host")
    val port = config.getInt("tracking.client.channel.port")
    val builder = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true)
    val channel = builder.build
    try {
      val client = TrackingGrpc.stub(channel)

      val empty = new StreamObserver[Empty] {
        override def onError(t: Throwable): Unit = {
          throw t
        }

        override def onCompleted(): Unit = {}

        override def onNext(value: Empty): Unit = {}
      }

      val events = client.track(empty)

      (1 to 10000).foreach { i =>
        val now = System.nanoTime()
        events.onNext(Event(id = i.toString + ": " + java.util.UUID.randomUUID.toString, uid = java.util.UUID.randomUUID.toString, createdAt = now, collectedAt = now))
      }

      events.onCompleted()
    }
    finally {
      channel.shutdown.awaitTermination(10, TimeUnit.SECONDS)
    }
  }

}
