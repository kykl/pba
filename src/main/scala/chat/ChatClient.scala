package chat

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}
import io.bigfast.chat.Channel.Message
import io.grpc.stub.StreamObserver

object ChatClient {
  def props(name: String, mediator: ActorRef, streamObserver: StreamObserver[Message]): Props = Props(classOf[ChatClient], name, mediator, streamObserver)
}

class ChatClient(name: String, mediator: ActorRef, streamObserver: StreamObserver[Message]) extends Actor with ActorLogging {
  val topic = s"control-$name"
  mediator ! Subscribe(topic, self)
  println(s"$name joined chat room")

  val channels = Seq(1.toString, 2.toString)

  channels.foreach { channel =>
    mediator ! Subscribe(channel, self)
  }

  def receive = {
    case message: Message =>
      streamObserver.onNext(message)
  }
}

/*
ChatRegularActor

constructor(name/id: string, ResponseObserver[ChannelMessage]: responseObserver)
init - get mediator and subscribe to topic "control-{name/id}"

receive messages
- SubscribeChannel(topic) => (mediator ? Subscribe(topic, self)) pipeTo sender
- UnsubscribeChannel(topic) => (mediator ? Unsubscribe(topic, self)) pipeTo sender
- ChannelMessage(channelMessage) => responseObserver.onNext(channelMessage)
 */

/*
ChatPrivilegedActor

constructor(name/id: string, ResponseObserver[ChannelMessage]: responseObserver)
init - get mediator and subscribe to topic "control-{name/id}"

receive messages
- SubscribeUser(userId, channelId) => mediator ? Publish("control-userId", SubscribeChannel(channelId)
- UnsubscribeUser(userId, channelId) => mediator ? Publish("control-userId", UnsubscribeChannel(channelId)
- ChannelMessage => responseObserver.onNext(channelMessage)
 */

/*
ChatImpl - ChatGRPCServer

--> RUN THIS ON CONNECT
subscribeEvents(requestObserver, historyLength) {
  BLAH BLAH CREATE ACTORSystem ->
    val systemName = "ChatApp"
    val system1 = ActorSystem(systemName)
    val joinAddress = Cluster(system1).selfAddress
    Cluster(system1).join(joinAddress)
    val mediator = DistributedPubSub(system1).mediator

  # AFTER CHECK IF CHANNELID SUBSCRIBED
  val responseObserver = new StreamObserver[ChannelMessage] {
    onNext(channelMessage) = mediator ! Publish(channelMessage.channelId, channelMessage)
  }

  # Presend history first
  sql"Select * from messages where channelId == channelId order by desc limit historyLength".foreach { message =>
    responseObserver.onNext(channelMessage)
  }

  # Hook up actor and let the pipe run
  val actor = system1.actorOf(ChatClient.props("userId", responseObserver)

}

ChatterBox - ChatGRPCClient

subscribeEvents {
  new StreamObserver[ChannelMessage] {
    onNext(channelMessage) = UnityPlayer.UnitySendMessage("MessageSystem", "ReceiveMessage", channelMessage.content);
  }
}

sendMessage = streamObserver.onNext(channelMessage)
 */