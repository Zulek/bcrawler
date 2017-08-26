import java.util

import com.redis.RedisClient
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

class KafkaSink(createProducer: () => KafkaProducer[String, String]) extends Serializable {

  lazy val producer: KafkaProducer[String, String] = createProducer()

  def send(topic: String, value: String): Unit = producer.send(new ProducerRecord(topic, value))
}

object KafkaSink {
  def apply(config: util.HashMap[String, Object]): KafkaSink = {
    val f = () => {
      val producer = new KafkaProducer[String,String](config)

      sys.addShutdownHook {
        producer.close()
      }

      producer
    }
    new KafkaSink(f)
  }
}


object RedisConnection extends Serializable {
  lazy val conn: RedisClient = new RedisClient("localhost", 6379)
}