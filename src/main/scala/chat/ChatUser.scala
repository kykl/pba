package chat

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern.{ask, pipe}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator.{Publish, Subscribe, SubscribeAck, Unsubscribe}
import io.bigfast.chat.Channel
import io.bigfast.chat.Channel.Message
import io.grpc.stub.StreamObserver

import scala.concurrent.Future

/*
ChatUser represents the Actor for a basic user in the messaging system
It's only responsibility is to relay messages from subscribed topics to the streamObserver
It has 2 auxiliary functions that allow it to subscribe and unsubscribe from a topic on command
 */
object ChatUser {
  def props(name: String, mediator: ActorRef, streamObserver: StreamObserver[Message]): Props = Props(classOf[ChatUser], name, mediator, streamObserver)

  def adminTopic(name: String) = s"admin-$name"
}

class ChatUser(name: String, mediator: ActorRef, streamObserver: StreamObserver[Message]) extends Actor with ActorLogging {
  val topic = ChatUser.adminTopic(name)
  mediator ! Subscribe(topic, self)
  println(s"$name joined chat room")

  def receive = {
    case message: Message =>
      streamObserver.onNext(message)
    case subscriptionAdd: Channel.Subscription.Add =>
      mediator ! Subscribe(subscriptionAdd.channelId.toString, self)
    case subscriptionRemove: Channel.Subscription.Remove =>
      mediator ! Unsubscribe(subscriptionRemove.channelId.toString, self)
    case subscriptionAdded: SubscribeAck =>
      // TODO: create a stream for listening to subscribe/unsubscribe events?
  }
}
