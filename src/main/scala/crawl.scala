import java.util.HashMap

import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.OAuthRequest
import com.github.scribejava.core.model.Verb
import com.github.scribejava.apis.VkontakteApi
import com.github.scribejava.core.oauth.OAuth20Service
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerConfig, ProducerRecord}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.apache.spark.SparkConf
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

object crawl {
    def main(args: Array[String]): Unit = {

        val zkQuorum = "localhost:2181"
        val group = ""
        val topics = "test"
        val numThreads = "1"
        val sparkConf = new SparkConf().setMaster("spark://localhost:7077").setAppName("KafkaCrawler")
        val ssc = new StreamingContext(sparkConf, Seconds(2))
        ssc.checkpoint("checkpoint")

        val topicMap = topics.split(",").map((_, numThreads.toInt)).toMap
        val idNumber = KafkaUtils.createStream(ssc, zkQuorum, group, topicMap).map(_._2)
        idNumber.map(id => {
            val service: OAuth20Service = (new ServiceBuilder).build(VkontakteApi.instance)
            val request = new OAuthRequest(Verb.GET, s"https://api.vk.com/method/friends.get?user_id=$id&v=5.68")
            val response = service.execute(request)
            if (!response.isSuccessful) None
            else
                response.getBody match {
                    case a: String if a.contains("error") => None
                    case a: String => Some(a)
                }
        })
          .flatMap(a => a.map(aa => (parse(aa) \\ "items").children.map(js => js.values.toString)))
          .foreachRDD(a => a.foreach(_.foreach{id =>
            val brokers = "localhost:9092"
            val topic = "test"
              // Zookeeper connection properties
              val props = new HashMap[String, Object]()
              props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokers)
              props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                  "org.apache.kafka.common.serialization.StringSerializer")
              props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                  "org.apache.kafka.common.serialization.StringSerializer")

              val producer = new KafkaProducer[String, String](props)
              val message = new ProducerRecord[String, String](topic, null, id)
              producer.send(message)
          }))
        ssc.start()
        ssc.awaitTermination()
    }
}
