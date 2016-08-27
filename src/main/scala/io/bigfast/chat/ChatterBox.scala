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
  def apply(host:String = "localhost", port:Int = 50051):ChatterBox = {
    val builder = ManagedChannelBuilder.forAddress(host, port)
    builder.usePlaintext(true)
    val channel = builder.build()
    new ChatterBox(channel, ChatGrpc.blockingStub(channel), ChatGrpc.stub(channel))
  }

  def main(args:Array[String]) = {
    val chatterBox = ChatterBox()

    try {
      val response = chatterBox.subscribe("my channel")
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

}

class ChatterBox private (channel: ManagedChannel, blockingStub: ChatGrpc.ChatBlockingStub, asyncStub: ChatGrpc.ChatStub) {
  def subscribe(ch:String):StreamObserver[Message] = {
    val r = new StreamObserver[Message] {
      override def onError(t: Throwable): Unit = {
        println(t)
      }

      override def onCompleted(): Unit = {
        println("Completed")
      }

      override def onNext(value: Message): Unit = {
        println(s"Message: $value")
        val shiz = ByteString.copyFrom("{hello: 'HI'}", "UTF-8")
        val enc = Base64.getEncoder
        enc.encodeToString(shiz.toByteArray)
      }
    }

    val requestObserver = asyncStub.channelMessageStream(r)


    println(s"Testing messaging")
    val msg = "{text: 'ping'}"
    val msgByteSTring = ChatterBox.encodeJsonAsByteString(msg)
    requestObserver.onNext(Message(channelId = "1", userId = "2", content = msgByteSTring))
    Thread.sleep(Random.nextInt(1000) + 500)
    requestObserver.onNext(Message(channelId = "1", userId = "2", content = msgByteSTring))
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