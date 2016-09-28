package io.bigfast.chat

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.TimeUnit

import com.google.protobuf.ByteString
import io.bigfast.chat.Channel.{Message, Subscription}
import io.grpc._
import io.grpc.stub.{MetadataUtils, StreamObserver}

import scala.util.Random

/**
  * Created by kykl on 8/16/16.
  */

object ChatterBox {
  val userId = "user123"

  def apply(host:String = "localhost", port:Int = 8443):ChatterBox = {
    val builder = ManagedChannelBuilder.forAddress(host, port)
    val channel = builder.build()
    val metadata = new Metadata()
    metadata.put(
      Metadata.Key.of("AUTHORIZATION", Metadata.ASCII_STRING_MARSHALLER),
      userId
    )
    val blockingStub = MetadataUtils.attachHeaders(
      ChatGrpc.blockingStub(channel),
      metadata
    )
    val asyncStub = MetadataUtils.attachHeaders(
      ChatGrpc.stub(channel),
      metadata
    )
    new ChatterBox(channel, blockingStub, asyncStub)
  }

  def main(args:Array[String]): Unit = {
    val chatterBox = ChatterBox(host = "messaging.rndmi.com")

    try {
      chatterBox.connectStream("my channel")
    }
    catch {
      case t:Throwable =>
        println(t)
        chatterBox.shutdown()
    }
  }

  def encodeJsonAsByteString(jsonString: String): ByteString = {
    val b64String = Base64.getEncoder.encodeToString(jsonString.getBytes(StandardCharsets.UTF_8))
    ByteString.copyFrom(b64String, StandardCharsets.UTF_8)
  }

  def decodeByteStringAsJson(byteString: ByteString): String = {
    val messageByteString = byteString.toByteArray
    val dec = Base64.getDecoder
    new String(dec.decode(messageByteString))
  }

}

class ChatterBox private (channel: ManagedChannel, blockingStub: ChatGrpc.ChatBlockingStub, asyncStub: ChatGrpc.ChatStub) {
  def connectStream(userId: String): StreamObserver[Message] = {
    val r = new StreamObserver[Message] {
      override def onError(t: Throwable): Unit = {
        println(t)
      }

      override def onCompleted(): Unit = {
        println("Completed")
      }

      override def onNext(message: Message): Unit = {
        val b64String = ChatterBox.decodeByteStringAsJson(message.content)
        println(s"Client Receive Message: $b64String")
      }
    }

    val requestObserver = asyncStub.channelMessageStream(r)

    println(s"Testing channel Create")
    val chatChannel = blockingStub.createChannel(Empty())
    println(s"Created channel with id ${chatChannel.id}")

    println(s"Subscribing to channel ${chatChannel.id}")
    blockingStub.subscribeChannel(Subscription.Add(
      chatChannel.id,
      ChatterBox.userId
    ))

    println(s"Testing messaging")
    val msg = "{'text':'hello there!'}"
    val byteString = ChatterBox.encodeJsonAsByteString(msg)
    requestObserver.onNext(Message(
      channelId = chatChannel.id,
      userId = ChatterBox.userId,
      content = byteString
    ))
    Thread.sleep(Random.nextInt(1000) + 500)
    requestObserver.onNext(Message(
      channelId = chatChannel.id,
      userId = ChatterBox.userId,
      content = byteString
    ))
    Thread.sleep(Random.nextInt(1000) + 500)

    requestObserver.onCompleted()

    println(s"Testing blocking calls")
    val channel = blockingStub.channelHistory(Channel.Get(chatChannel.id))
    println(s"Got channel $channel")


    r
  }



  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }
}