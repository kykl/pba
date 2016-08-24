package chat

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern.{ask, pipe}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe}

object ChatClient {
  def props(name: String): Props = Props(classOf[ChatClient], name)

  case class Publish(msg: String)
  case class Message(from: String, text: String)
}

class ChatClient(name: String) extends Actor with ActorLogging {
  val mediator = DistributedPubSub(context.system).mediator
  val topic = "chatroom"
  mediator ! Subscribe(topic, self)
  println(s"$name joined chat room")

  val channels = Seq("apples", "oranges")

  channels.foreach { channel =>
    mediator ! Subscribe(channel, self)
  }

  def receive = {
    case ChatClient.Publish(msg) =>
      mediator ! Publish(topic, ChatClient.Message(name, msg))

    case ChatClient.Message(from, text) =>
      val direction = if (sender == self) ">>>>" else s"<< $from:"
      println(s"$name $direction $text")
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