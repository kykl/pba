package io.bigfast.chat

import java.nio.charset.StandardCharsets
import java.util.Base64
import java.util.concurrent.TimeUnit

import com.google.protobuf.ByteString
import io.bigfast.chat.Channel.Message
import io.grpc.stub.StreamObserver
import io.grpc.{ManagedChannel, ManagedChannelBuilder}

import scala.util.Random

/**
  * Created by kykl on 8/16/16.
  */

object ChatterBox {
  def apply(host:String = "localhost", port:Int = 8443):ChatterBox = {
    val builder = ManagedChannelBuilder.forAddress(host, port)
    val channel = builder.build()
    new ChatterBox(channel, ChatGrpc.blockingStub(channel), ChatGrpc.stub(channel))
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


    println(s"Testing messaging")
    val msg = "{'text':'hello there!'}"
    val byteString = ChatterBox.encodeJsonAsByteString(msg)
    requestObserver.onNext(Message(channelId = "1", userId = "2", content = byteString))
    Thread.sleep(Random.nextInt(1000) + 500)
    requestObserver.onNext(Message(channelId = "1", userId = "2", content = byteString))
    Thread.sleep(Random.nextInt(1000) + 500)

    requestObserver.onCompleted()

    println(s"Testing blocking calls")
    val channel = blockingStub.channelHistory(Channel.Get("1"))
    println(s"Got channel $channel")
    r
  }



  def shutdown(): Unit = {
    channel.shutdown.awaitTermination(5, TimeUnit.SECONDS)
  }
}