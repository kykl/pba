package io.bigfast.tracking

import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.stream.ActorMaterializer
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import org.apache.kafka.common.serialization.ByteArraySerializer

/**
  * Created by kykl on 10/4/16.
  */
object Main {
  def main(args:Array[String]): Unit = {
    implicit val system = ActorSystem("system")
    //implicit val executionContext = system.dispatchers.lookup("my-dispatcher")
    implicit val materializer = ActorMaterializer()

    val producerSettings = ProducerSettings(system, new ByteArraySerializer, new ByteArraySerializer).withBootstrapServers("kafka:9092")

    val props = new java.util.Properties()
    //val input = new FileInputStream("/tmp/producer.properties");

    // load a properties file
    //props.load(input);
    producerSettings.properties.foreach {
      i => props.put(i._1, i._2)
    }



    val kp = new KafkaProducer[Array[Byte],Array[Byte]](props, new ByteArraySerializer, new ByteArraySerializer)
    //val kp = producerSettings.createKafkaProducer()

    (1 to 100).foreach { i =>
      val retval = kp.send(new ProducerRecord[Array[Byte], Array[Byte]]("x-event", i.toString.getBytes)).get
      println(s"ts: ${retval.timestamp} topic: ${retval.topic}")
    }

   /* val done = Source(1 to 100)
      .map { n =>
        // val partition = math.abs(n) % 2
        val partition = 0
        ProducerMessage.Message(new ProducerRecord[Array[Byte], String](
          "topic3", partition, null, n.toString
        ), n)
      }
      .via(Producer.flow(producerSettings))
      .map { result =>
        val record = result.message.record
        println(s"${record.topic}/${record.partition} ${result.offset}: ${record.value}" +
          s"(${result.message.passThrough})")
        result
      }
      .runWith(Sink.ignore)*/
  }
}
