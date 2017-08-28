import java.util
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