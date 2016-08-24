package io.bigfast.chat

import java.util.logging.Logger

import io.bigfast.chat.Channel.{Get, Message}
import io.grpc.stub.StreamObserver
import io.grpc.{Server, ServerBuilder}
import io.bigfast.chat.Channel.Subscription.{Add, Remove}

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
  implicit val ec = executionContext

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

    override def channelMessageStream(responseObserver: StreamObserver[Message]): StreamObserver[Message] = {
      new StreamObserver[Channel.Message] {
        override def onError(t: Throwable): Unit = println(t)

        override def onCompleted(): Unit = responseObserver.onCompleted()

        override def onNext(message: Message): Unit = {
          println(message)
          val responseMessage = message.copy(content = "pong")
          responseObserver.onNext(responseMessage)
        }
      }
    }

    override def createChannel(request: Empty): Future[Channel] = Future {
      val channel = Channel(1L)
      println(s"Creating channel ${channel.id}")
      channel
    }

    override def channelHistory(request: Get): Future[Channel] = Future {
      println(s"Returning channel history for channel ${request.channelId}")
      val messages = Seq(
        Message(id = 1L, channelId = request.channelId, userId = 2L, content = "ping"),
        Message(id = 2L, channelId = request.channelId, userId = 2L, content = "ping")
      )
      Channel(request.channelId, messages)
    }

    override def subscribeChannel(request: Add): Future[Empty] = Future {
      println(s"Subscribing to channel ${request.channelId} for user ${request.userId}")
      Empty.defaultInstance
    }

    override def unsubscribeChannel(request: Remove): Future[Empty] = Future {
      println(s"Unsubscribing to channel ${request.channelId} for user ${request.userId}")
      Empty.defaultInstance
    }

  }}
