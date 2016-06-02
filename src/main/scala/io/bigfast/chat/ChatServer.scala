package io.bigfast.chat

import java.util.logging.Logger

import io.grpc.stub.StreamObserver
import io.grpc.{Server, ServerBuilder}

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by kykl on 6/1/16.
  */

// https://github.com/xuwei-k/grpc-scala-sample/blob/master/grpc-scala/src/main/scala/io/grpc/examples/helloworld/HelloWorldServer.scala

object ChatServer {
  private val logger = Logger.getLogger(classOf[ChatServer].getName)

  def main(args: Array[String]): Unit = {
    val server = new ChatServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }

  private val port = 50051
}

class ChatServer(executionContext: ExecutionContext) { self =>
  private[this] var server: Server = null

  private def start(): Unit = {
    //     server = ServerBuilder.forPort(HelloWorldServer.port).addService(GreeterGrpc.bindService(new GreeterImpl, executionContext)).build.start

    server = ServerBuilder.forPort(ChatServer.port).addService(ChatGrpc.bindService(new ChatImpl, executionContext)).build.start
      //.addService(ChatGrpc.bindService(new ChatImpl(), executionContext)

    ChatServer.logger.info("Server started, listening on " + ChatServer.port)
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

  private class ChatImpl extends ChatGrpc.Chat {

    override def subscribeEvents(responseObserver: StreamObserver[Event]): StreamObserver[EventSubscription] = ???

    override def createChannel(request: CreateChannelRequest): Future[CreateChannelResponse] = ???

  }}
