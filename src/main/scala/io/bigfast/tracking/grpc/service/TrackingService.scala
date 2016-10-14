package io.bigfast.tracking.grpc.service

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import io.bigfast.tracking.TrackingGrpc
import io.grpc.{Server, ServerBuilder}

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

/**
  * Created by kykl on 9/30/16.
  */

object TrackingService {
  implicit val system = ActorSystem("system")
  implicit val executionContext = ExecutionContext.Implicits.global
  implicit val materializer = ActorMaterializer()

  def main(args:Array[String]) = {
    val server = new TrackingService
    server.start()
    server.blockUntilShutdown()
  }
}

class TrackingService()(implicit val executionContext:ExecutionContext, val system:ActorSystem, val materilizer:Materializer) {
  self =>
  private[this] var server: Server = _
  private val config = system.settings.config

  private def start(): Unit = {
    server = ServerBuilder
      .forPort(config.getInt("tracking.service.port"))
      .addService(TrackingGrpc.bindService(new TrackingImpl, executionContext)).build.start

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
}
