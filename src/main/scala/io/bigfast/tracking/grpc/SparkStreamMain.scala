package io.bigfast.tracking.grpc

import io.bigfast.tracking.Event
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka010.ConsumerStrategies.Subscribe
import org.apache.spark.streaming.kafka010.LocationStrategies.PreferConsistent
import org.apache.spark.streaming.kafka010._
import org.apache.spark.streaming.{Seconds, StreamingContext}

/**
  * Created by kykl on 10/27/16.
  */
object SparkStreamMain {
  def main(args:Array[String]) = {
    val conf = new SparkConf().setMaster("spark://spark-master:7077").setAppName("SparkStreamMain")
    val ssc = new StreamingContext(conf, Seconds(1))

    val kafkaParams = Map[String, Object](
      "bootstrap.servers" -> "kafka:9092",
      "key.deserializer" -> classOf[ByteArrayDeserializer],
      "value.deserializer" -> classOf[ByteArrayDeserializer],
      "group.id" -> "my.spark.stream",
      "auto.offset.reset" -> "earliest",
      "enable.auto.commit" -> (false: java.lang.Boolean)
    )

    val topics = Array("event")
    val stream = KafkaUtils.createDirectStream[Array[Byte], Array[Byte]](
      ssc,
      PreferConsistent,
      Subscribe[Array[Byte], Array[Byte]](topics, kafkaParams)
    )

    stream.map(record => {
      val id = if (record != null && record.key != null) new String(record.key) else "empty"
      val event = Event.parseFrom(record.value)
      println(s"id: ${id} event: ${event.toString}")
      (id, event)
    }).print()

    ssc.start()
    ssc.awaitTermination()
  }
}
