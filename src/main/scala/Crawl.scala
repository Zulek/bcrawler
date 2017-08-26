import java.util

import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Verb
import com.github.scribejava.apis.VkontakteApi
import com.github.scribejava.core.oauth.OAuth20Service
import com.redis.RedisClient
import org.apache.kafka.clients.producer.ProducerConfig
import org.json4s._
import org.json4s.native.JsonMethods._
import org.apache.spark.SparkConf
import org.apache.spark.broadcast.Broadcast
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.dstream.DStream
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

object Crawl {

  val sparkConf: SparkConf = new SparkConf().setMaster("local[*]").setAppName("KafkaCrawler")
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
    val sparkProcessingParallelism = 1 // You'd probably pick a higher value than 1 in production.
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

  def start(key:String, secret: String): Unit = {

    // Define the actual data flow of the streaming job
    kafkaStream
      .flatMap(id => {
        val service: OAuth20Service = (new ServiceBuilder).apiKey(key).apiSecret(secret).build(VkontakteApi.instance)
        val friendsLists = {
          val request = new OAuthRequest(Verb.GET, s"https://api.vk.com/method/friends.get?user_id=$id&v=5.68")
          val response = service.execute(request)
          if (!response.isSuccessful) None
          else
            response.getBody match {
              case a: String if a.contains("error") => None
              case a: String => Some(a)
            }
        }
        service.close()
        friendsLists
      })
      .map(friendsList => (parse(friendsList) \\ "items").children.map(js => js.values.toString))
      .foreachRDD ( rdd => {
        rdd.foreach { message =>
          message.foreach(m => if (RedisConnection.conn.sadd("test", m).contains(1L))
            kafkaSink.value.send(outputTopic, m))
        }
      })

    // Run the streaming job
    ssc.start()
    ssc.awaitTermination()
  }
}

//object Crawl {
//  def apply(apiKey: String, apiSecret: String): Crawl = new Crawl(apiKey, apiSecret)
//}