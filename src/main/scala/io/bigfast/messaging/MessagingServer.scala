package io.bigfast.messaging

import java.io.File
import java.util.{Base64, UUID}
import java.util.logging.Logger

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.Publish
import chat.ChatUser
import com.rndmi.messaging.MessagingClient
import com.rndmi.messaging.auth.RandomAuthService
import io.bigfast.messaging.Channel.Subscription.{Add, Remove}
import io.bigfast.messaging.Channel.{Get, Message}
import io.bigfast.messaging.auth.AuthService
import io.grpc._
import io.grpc.stub.StreamObserver

import scala.concurrent.{ExecutionContext, Future}

/**
  * MessagingServer
  * Allowed injection of authentication mechanism
  * 2 types of endpoints - user and privileged
  */

object MessagingServer {
  private val logger = Logger.getLogger(classOf[MessagingServer].getName)
  private val port = 8443

  def main(args: Array[String]): Unit = {
    implicit val authService = new RandomAuthService
    val server = new MessagingServer(ExecutionContext.global)
    server.start()
    server.blockUntilShutdown()
  }
}

class MessagingServer(executionContext: ExecutionContext)(implicit val authService: AuthService) {
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
    val certFile = new File("cert-chain.crt")
    val privateKey = new File("private-key.pem")
    server = ServerBuilder
      .forPort(MessagingServer.port)
      .useTransportSecurity(certFile, privateKey)
      .addService(
        ServerInterceptors.intercept(
          MessagingGrpc.bindService(new ChatImpl, executionContext),
          new HeaderServerInterceptor
        )
      )
      .build
      .start

    MessagingServer.logger.info("Server started, listening on " + MessagingServer.port)
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

  private class ChatImpl extends MessagingGrpc.Messaging {

    override def channelMessageStream(responseObserver: StreamObserver[Message]): StreamObserver[Message] = {
      val userId: String = HeaderServerInterceptor.userIdKey.get()
      println(s"Got userId: $userId")
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
      val msg = MessagingClient.encodeJsonAsByteString("{text: 'ping'}")
      val messages = Seq(
        Message(id = "1", channelId = request.channelId, userId = MessagingClient.userId, content = msg),
        Message(id = "2", channelId = request.channelId, userId = MessagingClient.userId, content = msg)
      )
      Channel(request.channelId, messages)
    }

    override def subscribeChannel(request: Add): Future[Empty] = Future {
      println(s"Subscribe to channel ${request.channelId} for user ${request.userId}")
      val adminTopic = ChatUser.adminTopic(request.userId.toString)
      mediator ! Publish(adminTopic, Add(request.channelId, request.userId))
      Empty.defaultInstance
    }

    override def unsubscribeChannel(request: Remove): Future[Empty] = Future {
      println(s"Unsubscribe from channel ${request.channelId} for user ${request.userId}")
      val adminTopic = ChatUser.adminTopic(request.userId.toString)
      mediator ! Publish(adminTopic, Remove(request.channelId, request.userId))
      Empty.defaultInstance
    }
  }
}