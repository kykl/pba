package io.bigfast.tracking.grpc.client

import java.util.concurrent.{CountDownLatch, TimeUnit}

import com.typesafe.config.{Config, ConfigFactory}
import io.bigfast.tracking.{Empty, Event, TrackingGrpc}
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver

/**
  * Created by kykl on 9/30/16.
  */
object TrackingClient {
  def main(args:Array[String]) = {
    val latch = new CountDownLatch(1)
    val config = ConfigFactory.load
    val host = config.getString("tracking.client.channel.host")
    val port = config.getInt("tracking.client.channel.port")
    val numberOfEvents = config.getInt("tracking.client.number-of-events")
    val timeout = config.getInt("tracking.client.termination-timeout")

    val builder = ManagedChannelBuilder.forAddress(host, port).usePlaintext(true)
    val channel = builder.build
    try {
      val client = TrackingGrpc.stub(channel)

      val empty = new StreamObserver[Empty] {
        override def onError(t: Throwable): Unit = {
          println("onError")
          t.printStackTrace()
          latch.countDown()
        }

        override def onCompleted(): Unit = {
          println("onCompleted")
          latch.countDown()
        }

        override def onNext(value: Empty): Unit = {
          println("onNext")
        }
      }

      val events = client.track(empty)

      (1 to numberOfEvents).foreach { i =>
        val now = System.nanoTime()
        events.onNext(Event(id = java.util.UUID.randomUUID.toString, uid = java.util.UUID.randomUUID.toString, createdAt = now, collectedAt = now))
      }

      events.onCompleted()
    }
    finally {
      println("await")
      latch.await(1, TimeUnit.MINUTES)

      //channel.shutdown.awaitTermination(timeout, TimeUnit.MINUTES)
    }
  }

}
