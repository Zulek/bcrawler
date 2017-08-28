import java.util

import com.github.scribejava.apis.VkontakteApi
import com.github.scribejava.core.builder.ServiceBuilder
import com.github.scribejava.core.model.{OAuthRequest, Response, Verb}
import com.github.scribejava.core.oauth.OAuth20Service
import com.redis.RedisClient
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}


class KafkaSink(createProducer: () => KafkaProducer[String, String]) extends Serializable {

  lazy val producer: KafkaProducer[String, String] = createProducer()
  /** Produce message to Apache Kafka.
    *
    *  @param topic Kafka topic
    *  @param value message to send
    */
  def send(topic: String, value: String): Unit = producer.send(new ProducerRecord(topic, value))
}

object KafkaSink extends Serializable{
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

class RedisLazy(createProducer: () => RedisClient) extends Serializable {

  lazy val conn: RedisClient = createProducer()

  /** Adds message to Redis Set "test"
    *
    *  @param message Message
    *  @return Option[Long]. If Some(1L) value was added, Some(0L) already exists in Set.
    */
  def send(message: String): Option[Long] = {
    conn.sadd("test",message)
  }
}

object RedisLazy {
  def apply(): RedisLazy = {
    val f = () => {
      val redisConn = new RedisClient("localhost", 6379)

      sys.addShutdownHook {
        redisConn.quit
      }

      redisConn
    }
    new RedisLazy(f)
  }
}


class OAuthServiceLazy(createProducer: () => OAuth20Service) extends Serializable {

  lazy val service: OAuth20Service = createProducer()

  /** Fetches friends list of a user
    *
    *  @param id VK user id
    *  @return Option[String]. Contains friends list JSON response.
    */
  def send(id:String): Option[String] = {
    val request = new OAuthRequest(Verb.GET, s"https://api.vk.com/method/friends.get?user_id=$id&v=5.68")
    val response = service.execute(request)
    if (!response.isSuccessful) None
    else
      response.getBody match {
        case a: String if a.contains("error") => None
        case a: String => Some(a)
      }
  }
}

object OAuthServiceLazy {
  def apply(key: String, secret: String): OAuthServiceLazy = {
    val f = () => {
      val service = (new ServiceBuilder).apiKey(key).apiSecret(secret).build(VkontakteApi.instance)

      sys.addShutdownHook {
        service.close()
      }

      service
    }
    new OAuthServiceLazy(f)
  }
}
