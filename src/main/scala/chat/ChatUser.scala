package chat

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSubMediator.{Subscribe, SubscribeAck, Unsubscribe}
import io.bigfast.messaging.Channel
import io.bigfast.messaging.Channel.Message
import io.grpc.stub.StreamObserver

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
  println(s"$name subscribed $topic")

  def receive = {
    case message: Message =>
      streamObserver.onNext(message)
    case subscriptionAdd: Channel.Subscription.Add =>
      mediator ! Subscribe(subscriptionAdd.channelId, self)
    case subscriptionRemove: Channel.Subscription.Remove =>
      mediator ! Unsubscribe(subscriptionRemove.channelId, self)
    case subscriptionAdded: SubscribeAck =>
    // TODO: create a stream for listening to subscribe/unsubscribe events?
  }
}
