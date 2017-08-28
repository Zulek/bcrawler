import java.util

import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Verb
import com.github.scribejava.apis.VkontakteApi
import com.github.scribejava.core.oauth.OAuth20Service
import org.apache.kafka.clients.producer.ProducerConfig
import org.json4s._
import org.json4s.native.JsonMethods._
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

/** VK Crawler.
  *
  *  Using API key and secret from VK application Crawler starts
  *  consuming ids from Apache Kafka, fetches friends of current id and pushes into redis and kafka if its not in redis.
  *  To initialize crawler provide key and secret as arguments
  */
object Crawl {
  def main(args: Array[String]): Unit = {
    if (args.length < 2) {
      System.err.println("Usage: Crawl <apiKey / ID ВК приложения> <apiSecret / Защищённый ключ>")
      System.exit(1)
    }
    val (key, secret) = (args(0),args(1))
    val sparkConf: SparkConf = new SparkConf().setAppName("KafkaCrawler").setMaster("local[*]")
    val ssc = new StreamingContext(sparkConf, Seconds(2))

    val kafkaStream: DStream[String] = {
      val sparkStreamingConsumerGroup = "test"
      val kafkaParams = Map(
        "zookeeper.connect" -> "localhost:2181",
        "group.id" -> "test",
        "zookeeper.connection.timeout.ms" -> "1000")
      val inputTopic = "test"
      val numPartitionsOfInputTopic = 5
      val streams = (1 to numPartitionsOfInputTopic) map { _ =>
        KafkaUtils.createStream(ssc, kafkaParams("zookeeper.connect"), kafkaParams("group.id"), Map(inputTopic -> 1), StorageLevel.MEMORY_ONLY_SER).map(_._2)
      }
      val unifiedStream = ssc.union(streams)
      val sparkProcessingParallelism = 1
      unifiedStream.repartition(sparkProcessingParallelism)
    }

    val brokerList = "localhost:9092"
    val outputTopic = "test"
    val props = new util.HashMap[String, Object]()
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList)
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringSerializer")
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
      "org.apache.kafka.common.serialization.StringSerializer")

    val kafkaSink: Broadcast[KafkaSink] = ssc.sparkContext.broadcast(KafkaSink(props))
    val service: Broadcast[OAuthServiceLazy] = ssc.sparkContext.broadcast(OAuthServiceLazy(key,secret))
    val redis: Broadcast[RedisLazy] = ssc.sparkContext.broadcast(RedisLazy())

    kafkaStream
      .flatMap(id => service.value.send(id))
      .map(friendsList => (parse(friendsList) \\ "items").children.map(js => js.values.toString))
      .foreachRDD ( rdd =>
        rdd.foreach { message =>
          message.foreach{m =>
            if (redis.value.send(m).contains(1L))
              kafkaSink.value.send(outputTopic, m)
          }
        }
      )

    // Run the streaming job
    ssc.start()
    ssc.awaitTermination()
  }
}