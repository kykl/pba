package io.bigfast.messaging

import java.io.File
import java.util.{Base64, UUID}
import java.util.logging.Logger

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import chat.ChatUser
import io.bigfast.messaging.Channel.Subscription.{Add, Remove}
import io.bigfast.messaging.Channel.{Get, Message}
import io.grpc._
import io.grpc.stub.StreamObserver

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by kykl on 6/1/16.
  */

// https://github.com/xuwei-k/grpc-scala-sample/blob/master/grpc-scala/src/main/scala/io/grpc/examples/helloworld/HelloWorldServer.scala

object ChatServer {
  private val logger = Logger.getLogger(classOf[ChatServer].getName)
  private val port = 8443

  def main(args: Array[String]): Unit = {
    val server = new ChatServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }
}

class ChatServer(executionContext: ExecutionContext) {
  self =>
  implicit val ec = executionContext
  // Join akka pubsub cluster
  val systemName = "DistributedMessaging"
  val system = ActorSystem(systemName)
  val joinAddress = Cluster(system).selfAddress
  val mediator = DistributedPubSub(system).mediator
  Cluster(system).join(joinAddress)
  private[this] var server: Server = _

  private def start(): Unit = {
    //     server = ServerBuilder.forPort(HelloWorldServer.port).addService(GreeterGrpc.bindService(new GreeterImpl, executionContext)).build.start

    val certFile = new File("cert-chain.crt")
    val privateKey = new File("private-key.pem")
    server = ServerBuilder
      .forPort(ChatServer.port)
      .useTransportSecurity(certFile, privateKey)
      .addService(
        ServerInterceptors.intercept(
          ChatGrpc.bindService(new ChatImpl, executionContext),
          new HeaderServerInterceptor
        )
      )
      .build
      .start

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
      val userId: String = HeaderServerInterceptor.contextKey.get()
      println(s"Got userId: $userId")
      // TODO: get userId from auth somehow
      system.actorOf(ChatUser.props(userId, mediator, responseObserver))


      new StreamObserver[Channel.Message] {
        override def onError(t: Throwable): Unit = println(t)

        override def onCompleted(): Unit = responseObserver.onCompleted()

        override def onNext(message: Message): Unit = {
          val messageByteString = message.content.toByteArray
          val dec = Base64.getDecoder
          val b64String = new String(dec.decode(messageByteString))
          println(s"Server Got Message: $b64String")
          mediator ! Publish(message.channelId.toString, message)
        }
      }
    }

    override def createChannel(request: Empty): Future[Channel] = Future {
      val channelId = UUID.randomUUID().toString
      val channel = Channel(channelId)
      println(s"Creating channel ${channel.id}")
      channel
    }

    override def channelHistory(request: Get): Future[Channel] = Future {
      println(s"Returning channel history for channel ${request.channelId}")
      val msg = ChatterBox.encodeJsonAsByteString("{text: 'ping'}")
      val messages = Seq(
        Message(id = "1", channelId = request.channelId, userId = ChatterBox.userId, content = msg),
        Message(id = "2", channelId = request.channelId, userId = ChatterBox.userId, content = msg)
      )
      Channel(request.channelId, messages)
    }

    override def subscribeChannel(request: Add): Future[Empty] = Future {
      println(s"Subscribing to channel ${request.channelId} for user ${request.userId}")
      val adminTopic = ChatUser.adminTopic(request.userId.toString)
      mediator ! Publish(adminTopic, Add(request.channelId, request.userId))
      Empty.defaultInstance
    }

    override def unsubscribeChannel(request: Remove): Future[Empty] = Future {
      println(s"Unsubscribing to channel ${request.channelId} for user ${request.userId}")
      val adminTopic = ChatUser.adminTopic(request.userId.toString)
      mediator ! Publish(adminTopic, Remove(request.channelId, request.userId))
      Empty.defaultInstance
    }

  }

}
